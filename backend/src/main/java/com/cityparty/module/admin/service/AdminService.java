package com.cityparty.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.result.PageResult;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.activity.vo.ActivityVO;
import com.cityparty.module.admin.vo.AdminCreditRecordVO;
import com.cityparty.module.admin.vo.AdminNoticeVO;
import com.cityparty.module.admin.vo.AdminUserVO;
import com.cityparty.module.admin.vo.DashboardVO;
import com.cityparty.module.credit.entity.CreditRecord;
import com.cityparty.module.credit.mapper.CreditRecordMapper;
import com.cityparty.module.notice.entity.SystemNotice;
import com.cityparty.module.notice.mapper.SystemNoticeMapper;
import com.cityparty.module.report.entity.Report;
import com.cityparty.module.report.mapper.ReportMapper;
import com.cityparty.module.review.entity.ActivityReview;
import com.cityparty.module.review.mapper.ActivityReviewMapper;
import com.cityparty.module.review.service.ActivityReviewService;
import com.cityparty.module.review.vo.ActivityReviewVO;
import com.cityparty.module.signup.entity.ActivitySignup;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.signup.service.SignupService;
import com.cityparty.module.signup.vo.SignupVO;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.entity.UserProfile;
import com.cityparty.module.user.mapper.UserMapper;
import com.cityparty.module.user.mapper.UserProfileMapper;
import com.cityparty.module.user.service.UserService;
import com.cityparty.module.user.vo.UserMeVO;
import com.cityparty.module.waitlist.service.ActivityWaitlistService;
import com.cityparty.module.waitlist.vo.ActivityWaitlistVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final ActivityMapper activityMapper;
    private final ActivitySignupMapper signupMapper;
    private final ActivityReviewMapper reviewMapper;
    private final CreditRecordMapper creditRecordMapper;
    private final SystemNoticeMapper noticeMapper;
    private final ReportMapper reportMapper;
    private final UserService userService;
    private final ActivityService activityService;
    private final SignupService signupService;
    private final ActivityWaitlistService waitlistService;
    private final ActivityReviewService reviewService;

    public DashboardVO dashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setUserCount(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)));
        vo.setActivityCount(activityMapper.selectCount(new LambdaQueryWrapper<Activity>().eq(Activity::getDeleted, 0)));
        vo.setSignupCount(signupMapper.selectCount(new LambdaQueryWrapper<ActivitySignup>().eq(ActivitySignup::getDeleted, 0)));
        vo.setReviewCount(reviewMapper.selectCount(new LambdaQueryWrapper<ActivityReview>().eq(ActivityReview::getDeleted, 0)));
        vo.setNoticeCount(noticeMapper.selectCount(new LambdaQueryWrapper<SystemNotice>().eq(SystemNotice::getDeleted, 0)));
        return vo;
    }

    public PageResult<AdminUserVO> users(String keyword, long current, long size) {
        List<Long> nicknameUserIds = findUserIdsByNickname(keyword);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)
                .orderByDesc(User::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> {
                w.like(User::getUsername, keyword).or().like(User::getPhone, keyword);
                if (!nicknameUserIds.isEmpty()) {
                    w.or().in(User::getId, nicknameUserIds);
                }
            });
        }
        Page<User> page = userMapper.selectPage(page(current, size), wrapper);
        return new PageResult<>(
                page.getRecords().stream().map(this::toAdminUserVO).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    public AdminUserVO userDetail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(404, "用户不存在");
        }
        return toAdminUserVO(user);
    }

    public PageResult<ActivityVO> activities(String keyword,
                                             String category,
                                             String status,
                                             long current,
                                             long size) {
        return activityService.page(keyword, category, null, null, status, safeCurrent(current), safeSize(size));
    }

    public ActivityVO activityDetail(Long id) {
        return activityService.detail(id);
    }

    public PageResult<SignupVO> activitySignups(Long activityId, long current, long size) {
        return signupService.activitySignups(activityId, safeCurrent(current), safeSize(size));
    }

    public PageResult<ActivityWaitlistVO> activityWaitlist(Long activityId, long current, long size) {
        return waitlistService.listWaitlist(activityId, safeCurrent(current), safeSize(size));
    }

    public PageResult<SignupVO> signups(Long activityId, String status, long current, long size) {
        LambdaQueryWrapper<ActivitySignup> wrapper = new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getDeleted, 0)
                .orderByDesc(ActivitySignup::getCreatedAt);
        if (activityId != null) {
            wrapper.eq(ActivitySignup::getActivityId, activityId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ActivitySignup::getStatus, status);
        }
        Page<ActivitySignup> page = signupMapper.selectPage(page(current, size), wrapper);
        return new PageResult<>(
                page.getRecords().stream().map(signupService::toVO).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    public PageResult<ActivityReviewVO> reviews(Long activityId, Long userId, long current, long size) {
        return reviewService.adminPage(activityId, userId, safeCurrent(current), safeSize(size));
    }

    public PageResult<AdminCreditRecordVO> credits(Long userId, long current, long size) {
        LambdaQueryWrapper<CreditRecord> wrapper = new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getDeleted, 0)
                .orderByDesc(CreditRecord::getCreatedAt);
        if (userId != null) {
            wrapper.eq(CreditRecord::getUserId, userId);
        }
        Page<CreditRecord> page = creditRecordMapper.selectPage(page(current, size), wrapper);
        return new PageResult<>(
                page.getRecords().stream().map(this::toAdminCreditRecordVO).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    public PageResult<AdminNoticeVO> notices(Long userId, long current, long size) {
        LambdaQueryWrapper<SystemNotice> wrapper = new LambdaQueryWrapper<SystemNotice>()
                .eq(SystemNotice::getDeleted, 0)
                .orderByDesc(SystemNotice::getCreatedAt);
        if (userId != null) {
            wrapper.eq(SystemNotice::getUserId, userId);
        }
        Page<SystemNotice> page = noticeMapper.selectPage(page(current, size), wrapper);
        return new PageResult<>(
                page.getRecords().stream().map(this::toAdminNoticeVO).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    public PageResult<Report> reports(long current, long size) {
        Page<Report> page = reportMapper.selectPage(page(current, size), new LambdaQueryWrapper<Report>()
                .eq(Report::getDeleted, 0)
                .orderByDesc(Report::getCreatedAt));
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    private AdminUserVO toAdminUserVO(User user) {
        UserMeVO detail = userService.getMe(user.getId());
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(detail.getUsername());
        vo.setPhone(detail.getPhone());
        vo.setRole(detail.getRole());
        vo.setStatus(detail.getStatus());
        vo.setCreditScore(detail.getCreditScore());
        vo.setNickname(detail.getNickname());
        vo.setAvatarUrl(detail.getAvatarUrl());
        vo.setCity(detail.getCity());
        vo.setBio(detail.getBio());
        vo.setInterestTags(detail.getInterestTags());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    private AdminCreditRecordVO toAdminCreditRecordVO(CreditRecord record) {
        AdminCreditRecordVO vo = new AdminCreditRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        fillUserDisplay(vo, record.getUserId());
        vo.setChangeScore(record.getChangeScore());
        vo.setBeforeScore(record.getBeforeScore());
        vo.setAfterScore(record.getAfterScore());
        vo.setReason(record.getReason());
        vo.setSourceType(record.getSourceType());
        vo.setSourceId(record.getSourceId());
        if (isReviewSource(record) && record.getSourceId() != null) {
            ActivityReview review = reviewMapper.selectById(record.getSourceId());
            if (review != null) {
                Activity activity = activityMapper.selectById(review.getActivityId());
                if (activity != null) {
                    vo.setActivityId(activity.getId());
                    vo.setActivityTitle(activity.getTitle());
                }
            }
        }
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }

    private AdminNoticeVO toAdminNoticeVO(SystemNotice notice) {
        AdminNoticeVO vo = new AdminNoticeVO();
        vo.setId(notice.getId());
        vo.setUserId(notice.getUserId());
        User user = userMapper.selectById(notice.getUserId());
        UserProfile profile = findProfile(notice.getUserId());
        vo.setUsername(user == null ? "未知用户" : user.getUsername());
        vo.setNickname(displayName(user, profile));
        vo.setType(notice.getType());
        vo.setTitle(notice.getTitle());
        vo.setContent(notice.getContent());
        vo.setRelatedId(notice.getRelatedId());
        vo.setRead(Integer.valueOf(1).equals(notice.getReadFlag()));
        vo.setCreatedAt(notice.getCreatedAt());
        return vo;
    }

    private void fillUserDisplay(AdminCreditRecordVO vo, Long userId) {
        User user = userMapper.selectById(userId);
        UserProfile profile = findProfile(userId);
        vo.setUsername(user == null ? "未知用户" : user.getUsername());
        vo.setNickname(displayName(user, profile));
    }

    private List<Long> findUserIdsByNickname(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        return userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                        .like(UserProfile::getNickname, keyword)
                        .eq(UserProfile::getDeleted, 0))
                .stream()
                .map(UserProfile::getUserId)
                .distinct()
                .toList();
    }

    private UserProfile findProfile(Long userId) {
        if (userId == null) {
            return null;
        }
        return userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .eq(UserProfile::getDeleted, 0)
                .last("limit 1"));
    }

    private String displayName(User user, UserProfile profile) {
        if (profile != null && StringUtils.hasText(profile.getNickname())) {
            return profile.getNickname();
        }
        return user == null ? "未知用户" : user.getUsername();
    }

    private boolean isReviewSource(CreditRecord record) {
        if (!StringUtils.hasText(record.getSourceType())) {
            return false;
        }
        String sourceType = record.getSourceType().toUpperCase(Locale.ROOT);
        return "ACTIVITY_REVIEW".equals(sourceType) || "REVIEW".equals(sourceType);
    }

    private <T> Page<T> page(long current, long size) {
        return new Page<>(safeCurrent(current), safeSize(size));
    }

    private long safeCurrent(long current) {
        return Math.max(current, 1);
    }

    private long safeSize(long size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
