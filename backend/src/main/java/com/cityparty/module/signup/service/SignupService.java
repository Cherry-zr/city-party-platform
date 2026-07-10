package com.cityparty.module.signup.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.result.PageResult;
import com.cityparty.common.security.UserContext;
import com.cityparty.common.utils.PageUtils;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.signup.dto.SignupCreateDTO;
import com.cityparty.module.signup.entity.ActivitySignup;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.signup.vo.SignupVO;
import com.cityparty.module.user.service.UserService;
import com.cityparty.module.waitlist.service.ActivityWaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final ActivitySignupMapper signupMapper;
    private final ActivityMapper activityMapper;
    private final ActivityService activityService;
    private final UserService userService;
    @Lazy
    private final ActivityWaitlistService waitlistService;

    @Transactional(rollbackFor = Exception.class)
    public SignupVO signup(Long activityId, SignupCreateDTO dto) {
        Long userId = UserContext.getUserId();
        Activity activity = activityService.requireActivity(activityId);
        if (activity.getCreatorId().equals(userId)) {
            throw new BusinessException("不能报名自己发起的活动");
        }
        if (!"SIGNING".equals(activity.getStatus()) && !"FULL".equals(activity.getStatus())) {
            throw new BusinessException("当前活动状态不可报名");
        }
        ActivitySignup existed = signupMapper.selectOne(new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getActivityId, activityId)
                .eq(ActivitySignup::getUserId, userId)
                .eq(ActivitySignup::getDeleted, 0)
                .last("limit 1"));
        LocalDateTime now = LocalDateTime.now();
        String nextStatus = Integer.valueOf(1).equals(activity.getNeedApproval()) ? "PENDING" : "APPROVED";
        ActivitySignup signup = existed == null ? new ActivitySignup() : existed;
        if (existed != null && ("PENDING".equals(existed.getStatus()) || "APPROVED".equals(existed.getStatus()) || "WAITING".equals(existed.getStatus()))) {
            throw new BusinessException("已报名该活动");
        }
        if (activity.getApprovedCount() >= activity.getMaxParticipants()) {
            throw new BusinessException(409, "活动已满员，可以加入候补队列");
        }
        signup.setActivityId(activityId);
        signup.setUserId(userId);
        signup.setStatus(nextStatus);
        signup.setApplyMessage(dto == null ? null : dto.getApplyMessage());
        signup.setUpdatedAt(now);
        signup.setDeleted(0);
        if (signup.getId() == null) {
            signup.setCreatedAt(now);
            signupMapper.insert(signup);
        } else {
            signupMapper.updateById(signup);
        }
        if ("APPROVED".equals(nextStatus)) {
            if (!activityService.increaseApprovedCountIfAvailable(activityId)) {
                throw new BusinessException(409, "Activity is full. Please join the waitlist.");
            }
        }
        return toVO(signup);
    }

    @Transactional(rollbackFor = Exception.class)
    public SignupVO cancel(Long activityId) {
        Long userId = UserContext.getUserId();
        ActivitySignup signup = signupMapper.selectOne(new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getActivityId, activityId)
                .eq(ActivitySignup::getUserId, userId)
                .eq(ActivitySignup::getDeleted, 0)
                .last("limit 1"));
        if (signup == null || "CANCELLED".equals(signup.getStatus())) {
            throw new BusinessException("未报名该活动");
        }
        Activity activity = activityService.requireActivity(activityId);
        String oldStatus = signup.getStatus();
        if ("WAITING".equals(oldStatus)) {
            waitlistService.cancelWaitlistForUser(activityId, userId);
            ActivitySignup updated = signupMapper.selectById(signup.getId());
            return toVO(updated);
        }
        signup.setStatus("CANCELLED");
        signup.setUpdatedAt(LocalDateTime.now());
        signupMapper.updateById(signup);
        if ("APPROVED".equals(oldStatus) && activity.getApprovedCount() > 0) {
            activityService.decreaseApprovedCount(activityId);
            waitlistService.promoteNextIfAvailable(activityId);
        }
        return toVO(signup);
    }

    @Transactional(rollbackFor = Exception.class)
    public SignupVO review(Long signupId, String status) {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new BusinessException("审核状态只能是 APPROVED 或 REJECTED");
        }
        ActivitySignup signup = signupMapper.selectById(signupId);
        if (signup == null || Integer.valueOf(1).equals(signup.getDeleted())) {
            throw new BusinessException("报名记录不存在");
        }
        Activity activity = activityService.requireActivity(signup.getActivityId());
        if (!activity.getCreatorId().equals(UserContext.getUserId())) {
            throw new BusinessException(403, "只有活动发起人可以审核");
        }
        if (!"PENDING".equals(signup.getStatus())) {
            throw new BusinessException("该报名记录无需审核");
        }
        if ("APPROVED".equals(status) && activity.getApprovedCount() >= activity.getMaxParticipants()) {
            throw new BusinessException("活动已满员");
        }
        signup.setStatus(status);
        signup.setReviewedAt(LocalDateTime.now());
        signup.setUpdatedAt(LocalDateTime.now());
        signupMapper.updateById(signup);
        if ("APPROVED".equals(status)) {
            if (!activityService.increaseApprovedCountIfAvailable(activity.getId())) {
                throw new BusinessException(409, "Activity is full.");
            }
        }
        return toVO(signup);
    }

    public PageResult<SignupVO> mySignups(long current, long size) {
        Page<ActivitySignup> page = signupMapper.selectPage(PageUtils.page(current, size), new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getUserId, UserContext.getUserId())
                .eq(ActivitySignup::getDeleted, 0)
                .orderByDesc(ActivitySignup::getCreatedAt));
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<SignupVO> activitySignups(Long activityId, long current, long size) {
        Activity activity = activityService.requireActivity(activityId);
        if (!activity.getCreatorId().equals(UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new BusinessException(403, "无权查看该活动报名");
        }
        Page<ActivitySignup> page = signupMapper.selectPage(PageUtils.page(current, size), new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getActivityId, activityId)
                .eq(ActivitySignup::getDeleted, 0)
                .orderByDesc(ActivitySignup::getCreatedAt));
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public SignupVO toVO(ActivitySignup signup) {
        SignupVO vo = new SignupVO();
        vo.setId(signup.getId());
        vo.setActivityId(signup.getActivityId());
        vo.setUserId(signup.getUserId());
        vo.setStatus(signup.getStatus());
        vo.setApplyMessage(signup.getApplyMessage());
        vo.setReviewedAt(signup.getReviewedAt());
        vo.setCreatedAt(signup.getCreatedAt());
        vo.setActivity(activityService.toVO(activityMapper.selectById(signup.getActivityId())));
        vo.setUser(userService.getMe(signup.getUserId()));
        return vo;
    }
}
