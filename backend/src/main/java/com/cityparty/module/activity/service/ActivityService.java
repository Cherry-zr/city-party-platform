package com.cityparty.module.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.result.PageResult;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.activity.dto.ActivityCreateDTO;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.entity.ActivityTag;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.mapper.ActivityTagMapper;
import com.cityparty.module.activity.vo.ActivityVO;
import com.cityparty.module.activity.vo.CreatorVO;
import com.cityparty.module.favorite.entity.ActivityFavorite;
import com.cityparty.module.favorite.mapper.ActivityFavoriteMapper;
import com.cityparty.module.signup.entity.ActivitySignup;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.entity.UserProfile;
import com.cityparty.module.user.mapper.UserMapper;
import com.cityparty.module.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityMapper activityMapper;
    private final ActivityTagMapper activityTagMapper;
    private final ActivitySignupMapper signupMapper;
    private final ActivityFavoriteMapper favoriteMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

    @Transactional(rollbackFor = Exception.class)
    public ActivityVO create(ActivityCreateDTO dto) {
        if (dto.getMaxParticipants() < dto.getMinParticipants()) {
            throw new BusinessException("最大人数不能小于最小人数");
        }
        LocalDateTime now = LocalDateTime.now();
        Activity activity = new Activity();
        activity.setCreatorId(UserContext.getUserId());
        activity.setTitle(dto.getTitle());
        activity.setCategory(dto.getCategory());
        activity.setTags(joinTags(dto.getTags()));
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setSignupDeadline(dto.getSignupDeadline());
        activity.setCity(dto.getCity());
        activity.setAddress(dto.getAddress());
        activity.setLongitude(dto.getLongitude());
        activity.setLatitude(dto.getLatitude());
        activity.setMinParticipants(dto.getMinParticipants());
        activity.setMaxParticipants(dto.getMaxParticipants());
        activity.setCostType(dto.getCostType());
        activity.setCostAmount(dto.getCostAmount());
        activity.setAaRule(dto.getAaRule());
        activity.setCoverUrl(dto.getCoverUrl());
        activity.setDescription(dto.getDescription());
        activity.setNotes(dto.getNotes());
        activity.setNeedApproval(Boolean.TRUE.equals(dto.getNeedApproval()) ? 1 : 0);
        activity.setStatus("SIGNING");
        activity.setApprovedCount(0);
        activity.setFavoriteCount(0);
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);
        activity.setDeleted(0);
        activityMapper.insert(activity);
        saveTags(activity.getId(), dto.getTags());
        return toVO(activity);
    }

    public PageResult<ActivityVO> page(String keyword, String category, String tag, String city, String status, long current, long size) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .eq(Activity::getDeleted, 0)
                .orderByDesc(Activity::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Activity::getTitle, keyword).or().like(Activity::getDescription, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Activity::getCategory, category);
        }
        if (StringUtils.hasText(tag)) {
            wrapper.like(Activity::getTags, tag);
        }
        if (StringUtils.hasText(city)) {
            wrapper.eq(Activity::getCity, city);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Activity::getStatus, status);
        }
        Page<Activity> page = activityMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public ActivityVO detail(Long id) {
        return toVO(requireActivity(id));
    }

    public PageResult<ActivityVO> myActivities(long current, long size) {
        Page<Activity> page = activityMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCreatorId, UserContext.getUserId())
                .eq(Activity::getDeleted, 0)
                .orderByDesc(Activity::getCreatedAt));
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public Activity requireActivity(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null || Integer.valueOf(1).equals(activity.getDeleted())) {
            throw new BusinessException("活动不存在");
        }
        return activity;
    }

    public ActivityVO toVO(Activity activity) {
        ActivityVO vo = new ActivityVO();
        vo.setId(activity.getId());
        vo.setCreatorId(activity.getCreatorId());
        vo.setTitle(activity.getTitle());
        vo.setCategory(activity.getCategory());
        vo.setTags(splitTags(activity.getTags()));
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setSignupDeadline(activity.getSignupDeadline());
        vo.setCity(activity.getCity());
        vo.setAddress(activity.getAddress());
        vo.setLongitude(activity.getLongitude());
        vo.setLatitude(activity.getLatitude());
        vo.setMinParticipants(activity.getMinParticipants());
        vo.setMaxParticipants(activity.getMaxParticipants());
        vo.setCostType(activity.getCostType());
        vo.setCostAmount(activity.getCostAmount());
        vo.setAaRule(activity.getAaRule());
        vo.setCoverUrl(activity.getCoverUrl());
        vo.setDescription(activity.getDescription());
        vo.setNotes(activity.getNotes());
        vo.setNeedApproval(Integer.valueOf(1).equals(activity.getNeedApproval()));
        vo.setStatus(activity.getStatus());
        vo.setApprovedCount(activity.getApprovedCount());
        vo.setFavoriteCount(activity.getFavoriteCount());
        vo.setCreatedAt(activity.getCreatedAt());
        vo.setCreator(buildCreator(activity.getCreatorId()));
        Long userId = UserContext.getUserIdOrNull();
        if (userId == null) {
            vo.setFavorited(false);
            vo.setSignupStatus(null);
            return vo;
        }
        vo.setFavorited(favoriteMapper.selectCount(new LambdaQueryWrapper<ActivityFavorite>()
                .eq(ActivityFavorite::getActivityId, activity.getId())
                .eq(ActivityFavorite::getUserId, userId)
                .eq(ActivityFavorite::getDeleted, 0)) > 0);
        ActivitySignup signup = signupMapper.selectOne(new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getActivityId, activity.getId())
                .eq(ActivitySignup::getUserId, userId)
                .eq(ActivitySignup::getDeleted, 0)
                .orderByDesc(ActivitySignup::getCreatedAt)
                .last("limit 1"));
        vo.setSignupStatus(signup == null ? null : signup.getStatus());
        return vo;
    }

    public void refreshStatusAfterCountChange(Activity activity) {
        if (activity.getApprovedCount() >= activity.getMaxParticipants()) {
            activity.setStatus("FULL");
        } else if ("FULL".equals(activity.getStatus())) {
            activity.setStatus("SIGNING");
        }
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
    }

    private CreatorVO buildCreator(Long creatorId) {
        User user = userMapper.selectById(creatorId);
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, creatorId)
                .eq(UserProfile::getDeleted, 0)
                .last("limit 1"));
        CreatorVO vo = new CreatorVO();
        vo.setId(creatorId);
        vo.setNickname(profile == null ? (user == null ? "未知用户" : user.getUsername()) : profile.getNickname());
        vo.setAvatarUrl(profile == null ? null : profile.getAvatarUrl());
        vo.setCity(profile == null ? null : profile.getCity());
        vo.setCreditScore(user == null ? null : user.getCreditScore());
        return vo;
    }

    private void saveTags(Long activityId, List<String> tags) {
        if (tags == null) {
            return;
        }
        for (String tag : tags) {
            if (!StringUtils.hasText(tag)) {
                continue;
            }
            ActivityTag activityTag = new ActivityTag();
            activityTag.setActivityId(activityId);
            activityTag.setTagName(tag);
            activityTag.setCreatedAt(LocalDateTime.now());
            activityTagMapper.insert(activityTag);
        }
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(",", tags.stream().filter(StringUtils::hasText).toList());
    }

    private List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(",")).filter(StringUtils::hasText).toList();
    }
}
