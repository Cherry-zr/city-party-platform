package com.cityparty.module.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.activity.vo.ActivityVO;
import com.cityparty.module.recommendation.algorithm.RecommendationScorer;
import com.cityparty.module.recommendation.vo.RecommendationScoreDetailVO;
import com.cityparty.module.recommendation.vo.RecommendedActivityVO;
import com.cityparty.module.signup.entity.ActivitySignup;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.mapper.UserMapper;
import com.cityparty.module.user.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private static final int MAX_LIMIT = 10;
    private static final int MAX_CANDIDATES = 100;
    private static final long CACHE_TTL_MINUTES = 5L;
    private static final String CACHE_PREFIX = "city-party:recommendation:activities";
    private static final Set<String> RECOMMENDABLE_STATUSES = Set.of("SIGNING", "FULL");
    private static final Set<String> EXCLUDED_SIGNUP_STATUSES = Set.of(
            "PENDING", "APPROVED", "WAITING", "PROMOTED", "COMPLETED"
    );

    private final ActivityMapper activityMapper;
    private final ActivitySignupMapper signupMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final ActivityService activityService;
    private final RecommendationScorer scorer;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public List<RecommendedActivityVO> recommendActivities(BigDecimal longitude,
                                                           BigDecimal latitude,
                                                           int limit) {
        validateLimit(limit);
        Long userId = UserContext.getUserId();
        userService.requireUser(userId);
        List<String> userInterests = userService.listInterestNames(userId);
        String cacheKey = buildCacheKey(userId, longitude, latitude, limit, userInterests);
        List<RecommendedActivityVO> cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Set<Long> excludedActivityIds = findExcludedActivityIds(userId);
        List<Activity> candidates = recallCandidates(userId, excludedActivityIds, now);
        List<Activity> eligibleCandidates = candidates.stream()
                .filter(activity -> isEligible(activity, userId, excludedActivityIds, now))
                .toList();
        Map<Long, User> creators = loadCreators(eligibleCandidates);

        List<ScoredCandidate> ranked = eligibleCandidates.stream()
                .map(activity -> score(activity, userInterests, longitude, latitude, creators, now))
                .sorted(candidateComparator())
                .limit(limit)
                .toList();

        List<RecommendedActivityVO> result = ranked.stream()
                .map(this::toRecommendedActivity)
                .toList();
        writeCache(cacheKey, result);
        return result;
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BusinessException(400, "推荐数量必须在 1 到 10 之间");
        }
    }

    private Set<Long> findExcludedActivityIds(Long userId) {
        List<ActivitySignup> signups = signupMapper.selectList(new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getUserId, userId)
                .eq(ActivitySignup::getDeleted, 0)
                .in(ActivitySignup::getStatus, EXCLUDED_SIGNUP_STATUSES));
        Set<Long> activityIds = new HashSet<>();
        if (signups != null) {
            signups.stream()
                    .map(ActivitySignup::getActivityId)
                    .filter(Objects::nonNull)
                    .forEach(activityIds::add);
        }
        return activityIds;
    }

    private List<Activity> recallCandidates(Long userId,
                                            Set<Long> excludedActivityIds,
                                            LocalDateTime now) {
        LambdaQueryWrapper<Activity> query = new LambdaQueryWrapper<Activity>()
                .eq(Activity::getDeleted, 0)
                .in(Activity::getStatus, RECOMMENDABLE_STATUSES)
                .gt(Activity::getStartTime, now)
                .ge(Activity::getSignupDeadline, now)
                .ne(Activity::getCreatorId, userId)
                .orderByAsc(Activity::getStartTime)
                .orderByAsc(Activity::getId)
                .last("LIMIT " + MAX_CANDIDATES);
        if (!excludedActivityIds.isEmpty()) {
            query.notIn(Activity::getId, excludedActivityIds);
        }
        List<Activity> activities = activityMapper.selectList(query);
        return activities == null ? List.of() : activities;
    }

    private boolean isEligible(Activity activity,
                               Long userId,
                               Set<Long> excludedActivityIds,
                               LocalDateTime now) {
        return activity != null
                && activity.getId() != null
                && Integer.valueOf(0).equals(activity.getDeleted())
                && RECOMMENDABLE_STATUSES.contains(activity.getStatus())
                && !Objects.equals(activity.getCreatorId(), userId)
                && activity.getStartTime() != null
                && activity.getStartTime().isAfter(now)
                && activity.getSignupDeadline() != null
                && !activity.getSignupDeadline().isBefore(now)
                && !excludedActivityIds.contains(activity.getId());
    }

    private Map<Long, User> loadCreators(List<Activity> candidates) {
        Set<Long> creatorIds = new HashSet<>();
        candidates.stream()
                .map(Activity::getCreatorId)
                .filter(Objects::nonNull)
                .forEach(creatorIds::add);
        if (creatorIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(creatorIds);
        Map<Long, User> creators = new HashMap<>();
        if (users != null) {
            users.stream()
                    .filter(Objects::nonNull)
                    .filter(user -> user.getId() != null)
                    .filter(user -> !Integer.valueOf(1).equals(user.getDeleted()))
                    .forEach(user -> creators.put(user.getId(), user));
        }
        return creators;
    }

    private ScoredCandidate score(Activity activity,
                                  Collection<String> userInterests,
                                  BigDecimal longitude,
                                  BigDecimal latitude,
                                  Map<Long, User> creators,
                                  LocalDateTime now) {
        User creator = creators.get(activity.getCreatorId());
        Integer creatorCreditScore = creator == null ? null : creator.getCreditScore();
        RecommendationScorer.ScoreResult score = scorer.score(new RecommendationScorer.ScoreInput(
                userInterests,
                activityFeatures(activity),
                longitude,
                latitude,
                activity.getLongitude(),
                activity.getLatitude(),
                activity.getApprovedCount(),
                activity.getMaxParticipants(),
                activity.getFavoriteCount(),
                activity.getStartTime(),
                creatorCreditScore,
                now
        ));
        return new ScoredCandidate(activity, score);
    }

    private List<String> activityFeatures(Activity activity) {
        List<String> features = new ArrayList<>();
        if (activity.getTags() != null) {
            for (String tag : activity.getTags().split(",")) {
                features.add(tag);
            }
        }
        features.add(activity.getCategory());
        return List.copyOf(scorer.normalize(features));
    }

    private Comparator<ScoredCandidate> candidateComparator() {
        return Comparator
                .comparing((ScoredCandidate candidate) -> candidate.score().recommendationScore(), Comparator.reverseOrder())
                .thenComparing(candidate -> candidate.activity().getStartTime())
                .thenComparing(candidate -> candidate.activity().getId());
    }

    private RecommendedActivityVO toRecommendedActivity(ScoredCandidate candidate) {
        RecommendationScorer.ScoreResult score = candidate.score();
        ActivityVO activity = activityService.toVO(candidate.activity());
        activity.setDistanceKm(score.distanceKm());

        RecommendationScoreDetailVO detail = new RecommendationScoreDetailVO();
        detail.setInterest(score.interestScore());
        detail.setDistance(score.distanceScore());
        detail.setHotness(score.hotnessScore());
        detail.setTime(score.timeScore());
        detail.setCredit(score.creditScore());

        RecommendedActivityVO vo = new RecommendedActivityVO();
        vo.setActivity(activity);
        vo.setRecommendationScore(score.recommendationScore());
        vo.setDistanceKm(score.distanceKm());
        vo.setReasons(buildReasons(score));
        vo.setScoreDetail(detail);
        return vo;
    }

    private List<String> buildReasons(RecommendationScorer.ScoreResult score) {
        List<String> reasons = new ArrayList<>();
        if (!score.matchedInterests().isEmpty()) {
            reasons.add("匹配你的兴趣：" + String.join("、", score.matchedInterests().stream().limit(3).toList()));
        }
        if (score.distanceKm() != null) {
            reasons.add("距你约 " + score.distanceKm().stripTrailingZeros().toPlainString() + " km");
        }
        if (score.hotnessScore().compareTo(BigDecimal.valueOf(60)) >= 0) {
            reasons.add("近期报名热度较高");
        }
        if (score.timeScore().compareTo(BigDecimal.valueOf(70)) >= 0) {
            reasons.add("近期可参加");
        }
        if (score.creditScore() != null && score.creditScore().compareTo(BigDecimal.valueOf(80)) >= 0) {
            reasons.add("发起人信用良好");
        }
        if (reasons.isEmpty()) {
            reasons.add("活动仍在可报名时间内");
        }
        return reasons.stream().limit(3).toList();
    }

    private String buildCacheKey(Long userId,
                                 BigDecimal longitude,
                                 BigDecimal latitude,
                                 int limit,
                                 Collection<String> userInterests) {
        String location = "none";
        if (longitude != null && latitude != null) {
            location = roundedCoordinate(longitude) + "," + roundedCoordinate(latitude);
        }
        String interestFingerprint = Integer.toUnsignedString(
                String.join("|", scorer.normalize(userInterests)).hashCode(),
                36
        );
        return CACHE_PREFIX + ":" + userId + ":" + location + ":" + limit + ":" + interestFingerprint;
    }

    private String roundedCoordinate(BigDecimal coordinate) {
        return coordinate.setScale(3, RoundingMode.HALF_UP).toPlainString();
    }

    private List<RecommendedActivityVO> readCache(String key) {
        try {
            String cached = redis.opsForValue().get(key);
            if (cached == null) {
                return null;
            }
            return objectMapper.readValue(cached, new TypeReference<List<RecommendedActivityVO>>() {
            });
        } catch (Exception e) {
            log.warn("Recommendation cache read failed; falling back to real-time calculation ({})",
                    e.getClass().getSimpleName());
            return null;
        }
    }

    private void writeCache(String key, List<RecommendedActivityVO> recommendations) {
        try {
            redis.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(recommendations),
                    CACHE_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("Recommendation cache write failed ({})", e.getClass().getSimpleName());
        }
    }

    private record ScoredCandidate(Activity activity, RecommendationScorer.ScoreResult score) {
    }
}
