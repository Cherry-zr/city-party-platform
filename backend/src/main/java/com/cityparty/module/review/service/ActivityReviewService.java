package com.cityparty.module.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.result.PageResult;
import com.cityparty.common.security.UserContext;
import com.cityparty.common.utils.PageUtils;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.credit.entity.CreditRecord;
import com.cityparty.module.credit.mapper.CreditRecordMapper;
import com.cityparty.module.notice.service.SystemNoticeService;
import com.cityparty.module.review.dto.ReviewCreateDTO;
import com.cityparty.module.review.entity.ActivityReview;
import com.cityparty.module.review.mapper.ActivityReviewMapper;
import com.cityparty.module.review.vo.ActivityReviewVO;
import com.cityparty.module.review.vo.ReviewTargetVO;
import com.cityparty.module.signup.entity.ActivitySignup;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.entity.UserProfile;
import com.cityparty.module.user.mapper.UserMapper;
import com.cityparty.module.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ActivityReviewService {

    private static final int MIN_CREDIT_SCORE = 60;
    private static final int MAX_CREDIT_SCORE = 120;

    private final ActivityMapper activityMapper;
    private final ActivitySignupMapper signupMapper;
    private final ActivityReviewMapper reviewMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final CreditRecordMapper creditRecordMapper;
    private final SystemNoticeService noticeService;

    public List<ReviewTargetVO> targets(Long activityId) {
        Long reviewerId = UserContext.getUserId();
        Activity activity = requireReviewableActivity(activityId, reviewerId);
        Set<Long> memberIds = memberIds(activity);
        memberIds.remove(reviewerId);

        Set<Long> reviewedIds = new LinkedHashSet<>(reviewMapper.selectList(
                new LambdaQueryWrapper<ActivityReview>()
                        .select(ActivityReview::getTargetUserId)
                        .eq(ActivityReview::getActivityId, activityId)
                        .eq(ActivityReview::getReviewerId, reviewerId)
                        .eq(ActivityReview::getDeleted, 0)
        ).stream().map(ActivityReview::getTargetUserId).filter(Objects::nonNull).toList());

        return memberIds.stream()
                .map(userId -> toTargetVO(userId, reviewedIds.contains(userId)))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ActivityReviewVO create(Long activityId, ReviewCreateDTO dto) {
        validateRequest(dto);
        Long reviewerId = UserContext.getUserId();
        Activity activity = requireReviewableActivity(activityId, reviewerId);
        Long targetUserId = dto.getTargetUserId();
        if (reviewerId.equals(targetUserId)) {
            throw new BusinessException("不能评价自己");
        }
        if (!isMember(activity, targetUserId)) {
            throw new BusinessException(403, "被评价用户不是该活动成员");
        }
        if (reviewExists(activityId, reviewerId, targetUserId)) {
            throw new BusinessException(409, "已评价该成员，不能重复评价");
        }

        User targetUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, targetUserId)
                .eq(User::getDeleted, 0)
                .last("FOR UPDATE"));
        if (targetUser == null) {
            throw new BusinessException("被评价用户不存在");
        }

        int beforeScore = targetUser.getCreditScore() == null ? 100 : targetUser.getCreditScore();
        int afterScore = Math.max(MIN_CREDIT_SCORE,
                Math.min(MAX_CREDIT_SCORE, beforeScore + creditDeltaFor(dto.getRating())));
        int actualDelta = afterScore - beforeScore;

        ActivityReview review = new ActivityReview();
        review.setActivityId(activityId);
        review.setReviewerId(reviewerId);
        review.setTargetUserId(targetUserId);
        review.setRating(dto.getRating());
        review.setContent(normalizeContent(dto.getContent()));
        review.setTags(joinTags(dto.getTags()));
        review.setCreditDelta(actualDelta);
        review.setCreatedAt(LocalDateTime.now());
        review.setDeleted(0);
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "已评价该成员，不能重复评价");
        }

        targetUser.setCreditScore(afterScore);
        if (userMapper.updateById(targetUser) != 1) {
            throw new BusinessException("信用分更新失败");
        }

        CreditRecord creditRecord = new CreditRecord();
        creditRecord.setUserId(targetUserId);
        creditRecord.setChangeScore(actualDelta);
        creditRecord.setBeforeScore(beforeScore);
        creditRecord.setAfterScore(afterScore);
        creditRecord.setReason("活动《" + activity.getTitle() + "》收到 " + dto.getRating() + " 分评价");
        creditRecord.setSourceType("ACTIVITY_REVIEW");
        creditRecord.setSourceId(review.getId());
        creditRecord.setCreatedAt(LocalDateTime.now());
        creditRecord.setDeleted(0);
        creditRecordMapper.insert(creditRecord);

        noticeService.createActivityReviewNotice(
                targetUserId,
                activityId,
                activity.getTitle(),
                dto.getRating(),
                actualDelta
        );
        return toVO(review);
    }

    public PageResult<ActivityReviewVO> activityReviews(Long activityId, long current, long size) {
        Long userId = UserContext.getUserId();
        requireReviewableActivity(activityId, userId);
        Page<ActivityReview> page = reviewMapper.selectPage(
                page(current, size),
                new LambdaQueryWrapper<ActivityReview>()
                        .eq(ActivityReview::getActivityId, activityId)
                        .eq(ActivityReview::getDeleted, 0)
                        .orderByDesc(ActivityReview::getCreatedAt)
        );
        return toPageResult(page);
    }

    public PageResult<ActivityReviewVO> myReviews(String type, long current, long size) {
        if (!"sent".equals(type) && !"received".equals(type)) {
            throw new BusinessException("type 仅支持 sent 或 received");
        }
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<ActivityReview> wrapper = new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getDeleted, 0)
                .orderByDesc(ActivityReview::getCreatedAt);
        if ("sent".equals(type)) {
            wrapper.eq(ActivityReview::getReviewerId, userId);
        } else {
            wrapper.eq(ActivityReview::getTargetUserId, userId);
        }
        return toPageResult(reviewMapper.selectPage(page(current, size), wrapper));
    }

    public PageResult<ActivityReviewVO> adminPage(Long activityId, Long userId, long current, long size) {
        LambdaQueryWrapper<ActivityReview> wrapper = new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getDeleted, 0)
                .orderByDesc(ActivityReview::getCreatedAt);
        if (activityId != null) {
            wrapper.eq(ActivityReview::getActivityId, activityId);
        }
        if (userId != null) {
            wrapper.and(w -> w.eq(ActivityReview::getReviewerId, userId)
                    .or()
                    .eq(ActivityReview::getTargetUserId, userId));
        }
        return toPageResult(reviewMapper.selectPage(page(current, size), wrapper));
    }

    private Page<ActivityReview> page(long current, long size) {
        return PageUtils.page(current, size);
    }

    private PageResult<ActivityReviewVO> toPageResult(Page<ActivityReview> page) {
        return new PageResult<>(
                page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    private Activity requireReviewableActivity(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || Integer.valueOf(1).equals(activity.getDeleted())) {
            throw new BusinessException("活动不存在");
        }
        if (activity.getEndTime() == null || activity.getEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("活动尚未结束，暂时不能评价");
        }
        if (!isMember(activity, userId)) {
            throw new BusinessException(403, "仅活动成员可以查看或提交评价");
        }
        return activity;
    }

    private boolean isMember(Activity activity, Long userId) {
        if (userId == null) {
            return false;
        }
        if (userId.equals(activity.getCreatorId())) {
            return true;
        }
        return signupMapper.selectCount(new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getActivityId, activity.getId())
                .eq(ActivitySignup::getUserId, userId)
                .eq(ActivitySignup::getStatus, "APPROVED")
                .eq(ActivitySignup::getDeleted, 0)) > 0;
    }

    private Set<Long> memberIds(Activity activity) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(activity.getCreatorId());
        signupMapper.selectList(new LambdaQueryWrapper<ActivitySignup>()
                        .select(ActivitySignup::getUserId)
                        .eq(ActivitySignup::getActivityId, activity.getId())
                        .eq(ActivitySignup::getStatus, "APPROVED")
                        .eq(ActivitySignup::getDeleted, 0)
                        .orderByAsc(ActivitySignup::getCreatedAt))
                .stream()
                .map(ActivitySignup::getUserId)
                .filter(Objects::nonNull)
                .forEach(ids::add);
        return ids;
    }

    private boolean reviewExists(Long activityId, Long reviewerId, Long targetUserId) {
        return reviewMapper.selectCount(new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getActivityId, activityId)
                .eq(ActivityReview::getReviewerId, reviewerId)
                .eq(ActivityReview::getTargetUserId, targetUserId)
                .eq(ActivityReview::getDeleted, 0)) > 0;
    }

    private void validateRequest(ReviewCreateDTO dto) {
        if (dto == null || dto.getTargetUserId() == null) {
            throw new BusinessException("请选择评价对象");
        }
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new BusinessException("评分必须在 1 到 5 之间");
        }
        if (dto.getContent() != null && dto.getContent().trim().length() > 500) {
            throw new BusinessException("评价内容不能超过 500 字");
        }
        List<String> tags = dto.getTags();
        if (tags == null) {
            return;
        }
        List<String> normalized = tags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (normalized.size() > 5) {
            throw new BusinessException("评价标签不能超过 5 个");
        }
        if (normalized.stream().anyMatch(tag -> tag.length() > 20 || tag.contains(","))) {
            throw new BusinessException("单个标签不能超过 20 字且不能包含逗号");
        }
    }

    private int creditDeltaFor(int rating) {
        return switch (rating) {
            case 5 -> 2;
            case 4 -> 1;
            case 3 -> 0;
            case 2 -> -2;
            case 1 -> -4;
            default -> throw new BusinessException("评分必须在 1 到 5 之间");
        };
    }

    private String normalizeContent(String content) {
        return StringUtils.hasText(content) ? content.trim() : null;
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        List<String> normalized = tags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    private List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private ReviewTargetVO toTargetVO(Long userId, boolean reviewed) {
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            return null;
        }
        UserProfile profile = profile(userId);
        ReviewTargetVO vo = new ReviewTargetVO();
        vo.setUserId(userId);
        vo.setNickname(profile == null ? user.getUsername() : profile.getNickname());
        vo.setAvatarUrl(profile == null ? null : profile.getAvatarUrl());
        vo.setCreditScore(user.getCreditScore());
        vo.setReviewed(reviewed);
        return vo;
    }

    private ActivityReviewVO toVO(ActivityReview review) {
        Activity activity = activityMapper.selectById(review.getActivityId());
        User reviewer = userMapper.selectById(review.getReviewerId());
        User target = userMapper.selectById(review.getTargetUserId());
        UserProfile reviewerProfile = profile(review.getReviewerId());
        UserProfile targetProfile = profile(review.getTargetUserId());

        ActivityReviewVO vo = new ActivityReviewVO();
        vo.setId(review.getId());
        vo.setActivityId(review.getActivityId());
        vo.setActivityTitle(activity == null ? "未知活动" : activity.getTitle());
        vo.setReviewerId(review.getReviewerId());
        vo.setReviewerNickname(displayName(reviewer, reviewerProfile));
        vo.setReviewerAvatarUrl(reviewerProfile == null ? null : reviewerProfile.getAvatarUrl());
        vo.setTargetUserId(review.getTargetUserId());
        vo.setTargetNickname(displayName(target, targetProfile));
        vo.setTargetAvatarUrl(targetProfile == null ? null : targetProfile.getAvatarUrl());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setTags(splitTags(review.getTags()));
        vo.setCreditDelta(review.getCreditDelta());
        vo.setCreatedAt(review.getCreatedAt());
        return vo;
    }

    private UserProfile profile(Long userId) {
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
}
