package com.cityparty.module.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.result.PageResult;
import com.cityparty.common.security.UserContext;
import com.cityparty.common.utils.PageUtils;
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
import com.cityparty.module.waitlist.entity.ActivityWaitlist;
import com.cityparty.module.waitlist.mapper.ActivityWaitlistMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private static final Set<String> MY_ACTIVITY_TYPES = Set.of("published", "joined", "waiting", "finished");
    private static final BigDecimal DEFAULT_NEARBY_DISTANCE_KM = BigDecimal.valueOf(5);
    private static final BigDecimal MAX_NEARBY_DISTANCE_KM = BigDecimal.valueOf(100);

    private final ActivityMapper activityMapper;
    private final ActivityTagMapper activityTagMapper;
    private final ActivitySignupMapper signupMapper;
    private final ActivityFavoriteMapper favoriteMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final ActivityWaitlistMapper waitlistMapper;

    @Transactional(rollbackFor = Exception.class)
    public ActivityVO create(ActivityCreateDTO dto) {
        validateCreateRequest(dto);
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
        Page<Activity> page = activityMapper.selectPage(PageUtils.page(current, size), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<ActivityVO> nearby(BigDecimal longitude,
                                         BigDecimal latitude,
                                         BigDecimal distanceKm,
                                         String category,
                                         String tag,
                                         String city,
                                         long current,
                                         long size) {
        long safeCurrent = PageUtils.safeCurrent(current);
        long safeSize = PageUtils.safeSize(size);
        BigDecimal maxDistance = normalizeDistance(distanceKm);
        if (longitude == null || latitude == null) {
            return page(null, category, tag, city, null, safeCurrent, safeSize);
        }
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .eq(Activity::getDeleted, 0)
                .isNotNull(Activity::getLongitude)
                .isNotNull(Activity::getLatitude);
        applyBoundingBox(wrapper, longitude, latitude, maxDistance);
        if (StringUtils.hasText(category)) {
            wrapper.eq(Activity::getCategory, category);
        }
        if (StringUtils.hasText(tag)) {
            wrapper.like(Activity::getTags, tag);
        }
        if (StringUtils.hasText(city)) {
            wrapper.eq(Activity::getCity, city);
        }
        List<ActivityVO> sorted = activityMapper.selectList(wrapper).stream()
                .map(activity -> {
                    ActivityVO vo = toVO(activity);
                    vo.setDistanceKm(calculateDistanceKm(latitude, longitude, activity.getLatitude(), activity.getLongitude()));
                    return vo;
                })
                .filter(vo -> vo.getDistanceKm() != null && vo.getDistanceKm().compareTo(maxDistance) <= 0)
                .sorted(Comparator.comparing(ActivityVO::getDistanceKm))
                .toList();
        long from = Math.max((safeCurrent - 1) * safeSize, 0);
        long to = Math.min(from + safeSize, sorted.size());
        List<ActivityVO> records = from >= sorted.size() ? Collections.emptyList() : sorted.subList((int) from, (int) to);
        return new PageResult<>(records, (long) sorted.size(), safeCurrent, safeSize);
    }

    public ActivityVO detail(Long id) {
        return toVO(requireActivity(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public ActivityVO update(Long id, ActivityCreateDTO dto) {
        Activity activity = validateUpdateRequest(id, dto);
        LocalDateTime now = LocalDateTime.now();
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
        activity.setUpdatedAt(now);
        activityMapper.updateById(activity);
        activityTagMapper.delete(new LambdaQueryWrapper<ActivityTag>().eq(ActivityTag::getActivityId, id));
        saveTags(id, dto.getTags());
        activityMapper.refreshJoinableStatus(id, now);
        return detail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public ActivityVO cancel(Long id) {
        Activity activity = requireActivity(id);
        ensureCanManage(activity);
        if ("FINISHED".equals(activity.getStatus())) {
            throw new BusinessException("Finished activity cannot be cancelled.");
        }
        if (!"CANCELLED".equals(activity.getStatus())) {
            activity.setStatus("CANCELLED");
            activity.setUpdatedAt(LocalDateTime.now());
            activityMapper.updateById(activity);
        }
        return detail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public ActivityVO finish(Long id) {
        Activity activity = requireActivity(id);
        ensureCanManage(activity);
        if ("CANCELLED".equals(activity.getStatus())) {
            throw new BusinessException("Cancelled activity cannot be finished.");
        }
        LocalDateTime now = LocalDateTime.now().withNano(0);
        if (activity.getStartTime() == null || !now.isAfter(activity.getStartTime().withNano(0))) {
            throw new BusinessException("Activity cannot be finished before it starts.");
        }
        activity.setStatus("FINISHED");
        if (activity.getEndTime() == null
                || !activity.getEndTime().isAfter(activity.getStartTime())
                || activity.getEndTime().isAfter(now)) {
            activity.setEndTime(now);
        }
        activity.setUpdatedAt(now);
        activityMapper.updateById(activity);
        return detail(id);
    }

    public PageResult<ActivityVO> myActivities(String type, long current, long size) {
        String normalizedType = StringUtils.hasText(type)
                ? type.trim().toLowerCase(Locale.ROOT)
                : "published";
        if (!MY_ACTIVITY_TYPES.contains(normalizedType)) {
            throw new BusinessException("type 仅支持 published、joined、waiting 或 finished");
        }
        Page<Activity> page = activityMapper.selectMyActivities(
                PageUtils.page(current, size),
                UserContext.getUserId(),
                normalizedType,
                LocalDateTime.now()
        );
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public Activity requireActivity(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null || Integer.valueOf(1).equals(activity.getDeleted())) {
            throw new BusinessException("活动不存在");
        }
        return activity;
    }

    public void validateCreateRequest(ActivityCreateDTO dto) {
        validateSchedule(dto);
        validateParticipantLimits(dto, null);
    }

    public Activity validateUpdateRequest(Long id, ActivityCreateDTO dto) {
        Activity activity = requireActivity(id);
        ensureCanManage(activity);
        if ("CANCELLED".equals(activity.getStatus()) || "FINISHED".equals(activity.getStatus())) {
            throw new BusinessException("Activity cannot be edited after cancel or finish.");
        }
        validateSchedule(dto);
        validateParticipantLimits(dto, activity);
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
        vo.setFeeType(activity.getCostType());
        vo.setFeeAmount(activity.getCostAmount());
        vo.setAaRule(activity.getAaRule());
        vo.setCoverUrl(activity.getCoverUrl());
        vo.setDescription(activity.getDescription());
        vo.setNotes(activity.getNotes());
        vo.setNeedApproval(Integer.valueOf(1).equals(activity.getNeedApproval()));
        vo.setStatus(activity.getStatus());
        vo.setApprovedCount(activity.getApprovedCount());
        vo.setFavoriteCount(activity.getFavoriteCount());
        vo.setWaitlistCount(waitlistMapper.selectCount(new LambdaQueryWrapper<ActivityWaitlist>()
                .eq(ActivityWaitlist::getActivityId, activity.getId())
                .eq(ActivityWaitlist::getStatus, "WAITING")
                .eq(ActivityWaitlist::getDeleted, 0)));
        vo.setCreatedAt(activity.getCreatedAt());
        CreatorVO creator = buildCreator(activity.getCreatorId());
        vo.setCreator(creator);
        vo.setCreatorNickname(creator.getNickname());
        vo.setCreatorAvatar(creator.getAvatarUrl());
        Long userId = UserContext.getUserIdOrNull();
        if (userId == null) {
            vo.setFavorited(false);
            vo.setSignupStatus(null);
            vo.setCanJoinWaitlist(false);
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
        vo.setCanJoinWaitlist(activity.getApprovedCount() >= activity.getMaxParticipants()
                && !activity.getCreatorId().equals(userId)
                && (signup == null || (!"PENDING".equals(signup.getStatus())
                && !"APPROVED".equals(signup.getStatus())
                && !"WAITING".equals(signup.getStatus()))));
        return vo;
    }

    public void refreshStatusAfterCountChange(Activity activity) {
        if (activity == null || activity.getId() == null) {
            return;
        }
        activityMapper.refreshJoinableStatus(activity.getId(), LocalDateTime.now());
    }

    public boolean increaseApprovedCountIfAvailable(Long activityId) {
        return activityMapper.increaseApprovedCountIfAvailable(activityId, LocalDateTime.now()) > 0;
    }

    public void decreaseApprovedCount(Long activityId) {
        activityMapper.decreaseApprovedCount(activityId, LocalDateTime.now());
    }

    private void ensureCanManage(Activity activity) {
        Long userId = UserContext.getUserId();
        if (!activity.getCreatorId().equals(userId) && !UserContext.isAdmin()) {
            throw new BusinessException(403, "No permission to manage this activity.");
        }
    }

    private void validateSchedule(ActivityCreateDTO dto) {
        if (dto.getStartTime() == null || dto.getEndTime() == null || dto.getSignupDeadline() == null) {
            throw new BusinessException("Activity start time, end time and signup deadline are required.");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException("Activity end time must be after start time.");
        }
        if (dto.getSignupDeadline().isAfter(dto.getStartTime())) {
            throw new BusinessException("Signup deadline cannot be after activity start time.");
        }
    }

    private void validateParticipantLimits(ActivityCreateDTO dto, Activity current) {
        if (dto.getMinParticipants() == null || dto.getMaxParticipants() == null) {
            throw new BusinessException("Min participants and max participants are required.");
        }
        if (dto.getMaxParticipants() < dto.getMinParticipants()) {
            throw new BusinessException("Max participants cannot be less than min participants.");
        }
        if (current != null && dto.getMaxParticipants() < current.getApprovedCount()) {
            throw new BusinessException("Max participants cannot be less than approved count.");
        }
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

    private BigDecimal normalizeDistance(BigDecimal distanceKm) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return DEFAULT_NEARBY_DISTANCE_KM;
        }
        return distanceKm.min(MAX_NEARBY_DISTANCE_KM);
    }

    private void applyBoundingBox(LambdaQueryWrapper<Activity> wrapper,
                                  BigDecimal longitude,
                                  BigDecimal latitude,
                                  BigDecimal distanceKm) {
        double radiusKm = distanceKm.doubleValue();
        double latDelta = radiusKm / 111.32D;
        double lngBase = Math.cos(Math.toRadians(latitude.doubleValue())) * 111.32D;
        double lngDelta = radiusKm / Math.max(Math.abs(lngBase), 0.01D);
        wrapper.between(Activity::getLatitude,
                        latitude.subtract(BigDecimal.valueOf(latDelta)),
                        latitude.add(BigDecimal.valueOf(latDelta)))
                .between(Activity::getLongitude,
                        longitude.subtract(BigDecimal.valueOf(lngDelta)),
                        longitude.add(BigDecimal.valueOf(lngDelta)));
    }

    private BigDecimal calculateDistanceKm(BigDecimal latitude,
                                           BigDecimal longitude,
                                           BigDecimal targetLatitude,
                                           BigDecimal targetLongitude) {
        if (latitude == null || longitude == null || targetLatitude == null || targetLongitude == null) {
            return null;
        }
        double earthRadiusKm = 6371.0088;
        double lat1 = Math.toRadians(latitude.doubleValue());
        double lat2 = Math.toRadians(targetLatitude.doubleValue());
        double deltaLat = Math.toRadians(targetLatitude.subtract(latitude).doubleValue());
        double deltaLng = Math.toRadians(targetLongitude.subtract(longitude).doubleValue());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(earthRadiusKm * c).setScale(2, RoundingMode.HALF_UP);
    }
}
