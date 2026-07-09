package com.cityparty.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.credit.CreditLevelResolver;
import com.cityparty.module.notice.entity.SystemNotice;
import com.cityparty.module.notice.mapper.SystemNoticeMapper;
import com.cityparty.module.review.entity.ActivityReview;
import com.cityparty.module.review.mapper.ActivityReviewMapper;
import com.cityparty.module.signup.entity.ActivitySignup;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.user.dto.UpdateProfileDTO;
import com.cityparty.module.user.entity.InterestTag;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.entity.UserInterest;
import com.cityparty.module.user.entity.UserProfile;
import com.cityparty.module.user.mapper.InterestTagMapper;
import com.cityparty.module.user.mapper.UserInterestMapper;
import com.cityparty.module.user.mapper.UserMapper;
import com.cityparty.module.user.mapper.UserProfileMapper;
import com.cityparty.module.user.vo.ProfileOverviewVO;
import com.cityparty.module.user.vo.PublicUserProfileVO;
import com.cityparty.module.user.vo.UserMeVO;
import com.cityparty.module.waitlist.mapper.ActivityWaitlistMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final InterestTagMapper interestTagMapper;
    private final UserInterestMapper userInterestMapper;
    private final ActivityMapper activityMapper;
    private final ActivitySignupMapper signupMapper;
    private final ActivityWaitlistMapper waitlistMapper;
    private final ActivityReviewMapper reviewMapper;
    private final SystemNoticeMapper noticeMapper;
    @Lazy
    private final ActivityService activityService;

    public UserMeVO getMe(Long userId) {
        User user = requireUser(userId);
        UserProfile profile = getProfile(userId);
        UserMeVO vo = new UserMeVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreditScore(user.getCreditScore());
        vo.setNickname(profile == null ? user.getUsername() : profile.getNickname());
        vo.setAvatarUrl(profile == null ? null : profile.getAvatarUrl());
        vo.setCity(profile == null ? null : profile.getCity());
        vo.setBio(profile == null ? null : profile.getBio());
        vo.setInterestTags(listInterestNames(userId));
        return vo;
    }

    public ProfileOverviewVO profileOverview(Long userId) {
        UserMeVO me = getMe(userId);
        ProfileOverviewVO vo = new ProfileOverviewVO();
        vo.setId(me.getId());
        vo.setUsername(me.getUsername());
        vo.setNickname(me.getNickname());
        vo.setAvatarUrl(me.getAvatarUrl());
        vo.setCity(me.getCity());
        vo.setBio(me.getBio());
        vo.setInterestTags(me.getInterestTags());
        vo.setCreditScore(me.getCreditScore());
        vo.setCreditLevel(CreditLevelResolver.resolve(me.getCreditScore()));
        vo.setPublishedActivityCount(activityMapper.selectCount(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCreatorId, userId)
                .eq(Activity::getDeleted, 0)));
        vo.setJoinedActivityCount(defaultCount(signupMapper.countJoinedActivities(userId)));
        vo.setWaitingActivityCount(defaultCount(waitlistMapper.countWaitingActivities(userId)));
        vo.setReceivedReviewCount(reviewMapper.selectCount(new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getTargetUserId, userId)
                .eq(ActivityReview::getDeleted, 0)));
        var averageRating = reviewMapper.selectAverageRatingByTargetUserId(userId);
        vo.setAverageRating(averageRating == null ? null : averageRating.setScale(1, RoundingMode.HALF_UP));
        vo.setUnreadNoticeCount(noticeMapper.selectCount(new LambdaQueryWrapper<SystemNotice>()
                .eq(SystemNotice::getUserId, userId)
                .eq(SystemNotice::getReadFlag, 0)
                .eq(SystemNotice::getDeleted, 0)));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserMeVO updateProfile(Long userId, UpdateProfileDTO dto) {
        requireUser(userId);
        LocalDateTime now = LocalDateTime.now();
        UserProfile profile = getProfile(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setCreatedAt(now);
            profile.setDeleted(0);
        }
        if (StringUtils.hasText(dto.getNickname())) {
            profile.setNickname(dto.getNickname());
        }
        if (StringUtils.hasText(dto.getAvatarUrl())) {
            profile.setAvatarUrl(dto.getAvatarUrl());
        }
        if (StringUtils.hasText(dto.getCity())) {
            profile.setCity(dto.getCity());
        }
        if (dto.getBio() != null) {
            profile.setBio(dto.getBio());
        }
        profile.setUpdatedAt(now);
        if (profile.getId() == null) {
            userProfileMapper.insert(profile);
        } else {
            userProfileMapper.updateById(profile);
        }
        if (dto.getInterestTags() != null) {
            saveInterests(userId, dto.getInterestTags());
        }
        return getMe(userId);
    }

    public PublicUserProfileVO publicProfile(Long userId) {
        User user = requireUser(userId);
        UserProfile profile = getProfile(userId);
        PublicUserProfileVO vo = new PublicUserProfileVO();
        vo.setId(user.getId());
        vo.setNickname(profile == null ? user.getUsername() : profile.getNickname());
        vo.setAvatarUrl(profile == null ? null : profile.getAvatarUrl());
        vo.setCity(profile == null ? null : profile.getCity());
        vo.setBio(profile == null ? null : profile.getBio());
        vo.setCreditScore(user.getCreditScore());
        vo.setInterestTags(listInterestNames(userId));
        vo.setCreatedActivityCount(activityMapper.selectCount(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCreatorId, userId)
                .eq(Activity::getDeleted, 0)));
        vo.setJoinedActivityCount(signupMapper.selectCount(new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getUserId, userId)
                .eq(ActivitySignup::getStatus, "APPROVED")
                .eq(ActivitySignup::getDeleted, 0)));
        Page<Activity> page = activityMapper.selectPage(new Page<>(1, 10), new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCreatorId, userId)
                .eq(Activity::getDeleted, 0)
                .orderByDesc(Activity::getCreatedAt));
        vo.setPublicActivities(page.getRecords().stream().map(activityService::toVO).toList());
        return vo;
    }

    public User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    public UserProfile getProfile(Long userId) {
        return userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .eq(UserProfile::getDeleted, 0)
                .last("limit 1"));
    }

    public List<String> listInterestNames(Long userId) {
        List<UserInterest> interests = userInterestMapper.selectList(new LambdaQueryWrapper<UserInterest>()
                .eq(UserInterest::getUserId, userId));
        if (interests.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (UserInterest interest : interests) {
            InterestTag tag = interestTagMapper.selectById(interest.getTagId());
            if (tag != null) {
                names.add(tag.getName());
            }
        }
        return names;
    }

    private void saveInterests(Long userId, List<String> names) {
        userInterestMapper.delete(new LambdaQueryWrapper<UserInterest>().eq(UserInterest::getUserId, userId));
        for (String name : names) {
            if (!StringUtils.hasText(name)) {
                continue;
            }
            InterestTag tag = interestTagMapper.selectOne(new LambdaQueryWrapper<InterestTag>()
                    .eq(InterestTag::getName, name)
                    .last("limit 1"));
            if (tag == null) {
                tag = new InterestTag();
                tag.setName(name);
                tag.setSortOrder(100);
                tag.setCreatedAt(LocalDateTime.now());
                interestTagMapper.insert(tag);
            }
            UserInterest interest = new UserInterest();
            interest.setUserId(userId);
            interest.setTagId(tag.getId());
            interest.setCreatedAt(LocalDateTime.now());
            userInterestMapper.insert(interest);
        }
    }

    private long defaultCount(Long count) {
        return count == null ? 0L : count;
    }
}
