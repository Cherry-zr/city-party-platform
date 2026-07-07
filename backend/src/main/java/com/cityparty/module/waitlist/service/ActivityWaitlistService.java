package com.cityparty.module.waitlist.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.result.PageResult;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.notice.service.SystemNoticeService;
import com.cityparty.module.signup.entity.ActivitySignup;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.user.service.UserService;
import com.cityparty.module.waitlist.entity.ActivityWaitlist;
import com.cityparty.module.waitlist.mapper.ActivityWaitlistMapper;
import com.cityparty.module.waitlist.vo.ActivityWaitlistVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ActivityWaitlistService {

    private static final String WAITLIST_KEY_PREFIX = "activity:waitlist:";

    private final ActivityWaitlistMapper waitlistMapper;
    private final ActivitySignupMapper signupMapper;
    private final ActivityMapper activityMapper;
    private final ActivityService activityService;
    private final UserService userService;
    private final SystemNoticeService noticeService;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional(rollbackFor = Exception.class)
    public ActivityWaitlistVO joinWaitlist(Long activityId) {
        Long userId = UserContext.getUserId();
        Activity activity = activityService.requireActivity(activityId);
        if (activity.getCreatorId().equals(userId)) {
            throw new BusinessException("不能候补自己发起的活动");
        }
        if (!"SIGNING".equals(activity.getStatus()) && !"FULL".equals(activity.getStatus())) {
            throw new BusinessException("当前活动状态不可候补");
        }
        if (activity.getApprovedCount() < activity.getMaxParticipants()) {
            throw new BusinessException("活动尚未满员，请直接报名");
        }

        ActivitySignup signup = latestSignup(activityId, userId);
        if (signup != null && ("PENDING".equals(signup.getStatus()) || "APPROVED".equals(signup.getStatus()))) {
            throw new BusinessException("已报名该活动，不能加入候补");
        }
        if (signup != null && "WAITING".equals(signup.getStatus())) {
            throw new BusinessException("已在候补队列中");
        }

        ActivityWaitlist waitlist = waitlistMapper.selectOne(new LambdaQueryWrapper<ActivityWaitlist>()
                .eq(ActivityWaitlist::getActivityId, activityId)
                .eq(ActivityWaitlist::getUserId, userId)
                .eq(ActivityWaitlist::getDeleted, 0)
                .last("limit 1"));
        if (waitlist != null && "WAITING".equals(waitlist.getStatus())) {
            throw new BusinessException("已在候补队列中");
        }

        LocalDateTime now = LocalDateTime.now();
        if (signup == null) {
            signup = new ActivitySignup();
            signup.setActivityId(activityId);
            signup.setUserId(userId);
            signup.setApplyMessage("加入候补队列");
            signup.setCreatedAt(now);
            signup.setDeleted(0);
        }
        signup.setStatus("WAITING");
        signup.setReviewedAt(null);
        signup.setUpdatedAt(now);
        if (signup.getId() == null) {
            signupMapper.insert(signup);
        } else {
            signupMapper.updateById(signup);
        }

        if (waitlist == null) {
            waitlist = new ActivityWaitlist();
            waitlist.setActivityId(activityId);
            waitlist.setUserId(userId);
            waitlist.setCreatedAt(now);
            waitlist.setDeleted(0);
        }
        waitlist.setStatus("WAITING");
        waitlist.setQueueNo(nextQueueNo(activityId));
        waitlist.setUpdatedAt(now);
        if (waitlist.getId() == null) {
            waitlistMapper.insert(waitlist);
        } else {
            waitlistMapper.updateById(waitlist);
        }
        pushRedis(activityId, waitlist.getId());
        return toVO(waitlist);
    }

    @Transactional(rollbackFor = Exception.class)
    public ActivityWaitlistVO cancelWaitlist(Long activityId) {
        return cancelWaitlistForUser(activityId, UserContext.getUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ActivityWaitlistVO cancelWaitlistForUser(Long activityId, Long userId) {
        ActivityWaitlist waitlist = waitlistMapper.selectOne(new LambdaQueryWrapper<ActivityWaitlist>()
                .eq(ActivityWaitlist::getActivityId, activityId)
                .eq(ActivityWaitlist::getUserId, userId)
                .eq(ActivityWaitlist::getStatus, "WAITING")
                .eq(ActivityWaitlist::getDeleted, 0)
                .last("limit 1"));
        if (waitlist == null) {
            throw new BusinessException("未加入该活动候补");
        }
        waitlist.setStatus("CANCELLED");
        waitlist.setUpdatedAt(LocalDateTime.now());
        waitlistMapper.updateById(waitlist);

        ActivitySignup signup = latestSignup(activityId, userId);
        if (signup != null && "WAITING".equals(signup.getStatus())) {
            signup.setStatus("CANCELLED");
            signup.setUpdatedAt(LocalDateTime.now());
            signupMapper.updateById(signup);
        }
        removeRedis(activityId, waitlist.getId());
        return toVO(waitlist);
    }

    public PageResult<ActivityWaitlistVO> listWaitlist(Long activityId, long current, long size) {
        Activity activity = activityService.requireActivity(activityId);
        if (!activity.getCreatorId().equals(UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new BusinessException(403, "无权查看该活动候补列表");
        }
        Page<ActivityWaitlist> page = waitlistMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<ActivityWaitlist>()
                .eq(ActivityWaitlist::getActivityId, activityId)
                .eq(ActivityWaitlist::getDeleted, 0)
                .orderByAsc(ActivityWaitlist::getQueueNo)
                .orderByAsc(ActivityWaitlist::getCreatedAt));
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public void promoteNextIfAvailable(Long activityId) {
        Activity activity = activityService.requireActivity(activityId);
        if (activity.getApprovedCount() >= activity.getMaxParticipants()) {
            return;
        }
        ActivityWaitlist waitlist = nextWaitingFromRedis(activityId);
        if (waitlist == null) {
            waitlist = firstWaitingFromMysql(activityId);
        }
        if (waitlist == null) {
            activityService.refreshStatusAfterCountChange(activity);
            return;
        }
        ActivitySignup signup = latestSignup(activityId, waitlist.getUserId());
        LocalDateTime now = LocalDateTime.now();
        if (signup == null) {
            signup = new ActivitySignup();
            signup.setActivityId(activityId);
            signup.setUserId(waitlist.getUserId());
            signup.setApplyMessage("候补自动转正");
            signup.setCreatedAt(waitlist.getCreatedAt() == null ? now : waitlist.getCreatedAt());
            signup.setDeleted(0);
        }
        signup.setStatus("APPROVED");
        signup.setReviewedAt(now);
        signup.setUpdatedAt(now);
        if (signup.getId() == null) {
            signupMapper.insert(signup);
        } else {
            signupMapper.updateById(signup);
        }

        waitlist.setStatus("PROMOTED");
        waitlist.setUpdatedAt(now);
        waitlistMapper.updateById(waitlist);

        activity.setApprovedCount(activity.getApprovedCount() + 1);
        activityService.refreshStatusAfterCountChange(activity);
        noticeService.createWaitlistPromotedNotice(waitlist.getUserId(), activityId, activity.getTitle());
    }

    private ActivityWaitlist nextWaitingFromRedis(Long activityId) {
        while (true) {
            String value;
            try {
                value = stringRedisTemplate.opsForList().leftPop(waitlistKey(activityId));
            } catch (RuntimeException e) {
                return null;
            }
            if (value == null) {
                return null;
            }
            try {
                ActivityWaitlist waitlist = waitlistMapper.selectById(Long.valueOf(value));
                if (waitlist != null && "WAITING".equals(waitlist.getStatus()) && Integer.valueOf(0).equals(waitlist.getDeleted())) {
                    return waitlist;
                }
            } catch (NumberFormatException ignored) {
                // Skip invalid Redis values and continue with the next queue item.
            }
        }
    }

    private ActivityWaitlist firstWaitingFromMysql(Long activityId) {
        return waitlistMapper.selectOne(new LambdaQueryWrapper<ActivityWaitlist>()
                .eq(ActivityWaitlist::getActivityId, activityId)
                .eq(ActivityWaitlist::getStatus, "WAITING")
                .eq(ActivityWaitlist::getDeleted, 0)
                .orderByAsc(ActivityWaitlist::getQueueNo)
                .orderByAsc(ActivityWaitlist::getCreatedAt)
                .last("limit 1"));
    }

    private ActivitySignup latestSignup(Long activityId, Long userId) {
        return signupMapper.selectOne(new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getActivityId, activityId)
                .eq(ActivitySignup::getUserId, userId)
                .eq(ActivitySignup::getDeleted, 0)
                .orderByDesc(ActivitySignup::getCreatedAt)
                .last("limit 1"));
    }

    private Long nextQueueNo(Long activityId) {
        ActivityWaitlist latest = waitlistMapper.selectOne(new LambdaQueryWrapper<ActivityWaitlist>()
                .eq(ActivityWaitlist::getActivityId, activityId)
                .eq(ActivityWaitlist::getDeleted, 0)
                .orderByDesc(ActivityWaitlist::getQueueNo)
                .last("limit 1"));
        return latest == null || latest.getQueueNo() == null ? 1L : latest.getQueueNo() + 1;
    }

    private void pushRedis(Long activityId, Long waitlistId) {
        try {
            stringRedisTemplate.opsForList().rightPush(waitlistKey(activityId), String.valueOf(waitlistId));
        } catch (RuntimeException e) {
            throw new BusinessException("Redis 候补队列暂不可用，请稍后重试");
        }
    }

    private void removeRedis(Long activityId, Long waitlistId) {
        try {
            stringRedisTemplate.opsForList().remove(waitlistKey(activityId), 0, String.valueOf(waitlistId));
        } catch (RuntimeException ignored) {
            // MySQL keeps the durable waitlist state; Redis can be rebuilt from WAITING rows.
        }
    }

    private String waitlistKey(Long activityId) {
        return WAITLIST_KEY_PREFIX + activityId;
    }

    private ActivityWaitlistVO toVO(ActivityWaitlist waitlist) {
        Activity activity = activityMapper.selectById(waitlist.getActivityId());
        ActivityWaitlistVO vo = new ActivityWaitlistVO();
        vo.setId(waitlist.getId());
        vo.setActivityId(waitlist.getActivityId());
        vo.setActivityTitle(activity == null ? null : activity.getTitle());
        vo.setUserId(waitlist.getUserId());
        vo.setStatus(waitlist.getStatus());
        vo.setQueueNo(waitlist.getQueueNo());
        vo.setCreatedAt(waitlist.getCreatedAt());
        vo.setUpdatedAt(waitlist.getUpdatedAt());
        vo.setUser(userService.getMe(waitlist.getUserId()));
        return vo;
    }
}
