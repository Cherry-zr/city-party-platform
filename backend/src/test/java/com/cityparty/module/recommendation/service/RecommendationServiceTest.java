package com.cityparty.module.recommendation.service;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.security.LoginUser;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {

    private static final Long CURRENT_USER_ID = 7L;
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-31T04:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final ActivitySignupMapper signupMapper = mock(ActivitySignupMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserService userService = mock(UserService.class);
    private final ActivityService activityService = mock(ActivityService.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        UserContext.set(new LoginUser(CURRENT_USER_ID, "recommendation_user", "USER"));
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn(null);
        when(userService.requireUser(CURRENT_USER_ID)).thenReturn(user(CURRENT_USER_ID, 100));
        when(userService.listInterestNames(CURRENT_USER_ID)).thenReturn(List.of("周末"));
        when(signupMapper.selectList(any())).thenReturn(List.of());
        when(activityService.toVO(any(Activity.class))).thenAnswer(invocation -> toVO(invocation.getArgument(0)));
        service = new RecommendationService(
                activityMapper,
                signupMapper,
                userMapper,
                userService,
                activityService,
                new RecommendationScorer(),
                redis,
                objectMapper,
                CLOCK
        );
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void excludesIneligibleActivitiesAndKeepsFullActivity() {
        Activity own = activity(1L, "自己的活动", "SIGNING", "周末");
        own.setCreatorId(CURRENT_USER_ID);
        Activity cancelled = activity(2L, "已取消", "CANCELLED", "周末");
        Activity finished = activity(3L, "已结束", "FINISHED", "周末");
        Activity started = activity(4L, "已开始", "SIGNING", "周末");
        started.setStartTime(NOW.minusHours(1));
        Activity expired = activity(5L, "报名截止", "SIGNING", "周末");
        expired.setSignupDeadline(NOW.minusSeconds(1));
        Activity deleted = activity(6L, "已删除", "SIGNING", "周末");
        deleted.setDeleted(1);
        Activity full = activity(7L, "满员可候补", "FULL", "周末");
        Activity signed = activity(8L, "已经报名", "SIGNING", "周末");
        when(signupMapper.selectList(any())).thenReturn(List.of(signup(signed.getId(), "APPROVED")));
        stubCandidates(List.of(own, cancelled, finished, started, expired, deleted, full, signed));

        List<RecommendedActivityVO> result = service.recommendActivities(null, null, 10);

        assertThat(result).extracting(item -> item.getActivity().getId()).containsExactly(full.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "APPROVED", "WAITING", "PROMOTED", "COMPLETED"})
    void excludesEveryActiveSignupAndWaitlistStatus(String status) {
        Activity candidate = activity(20L, "已参与活动", "SIGNING", "周末");
        when(signupMapper.selectList(any())).thenReturn(List.of(signup(candidate.getId(), status)));
        stubCandidates(List.of(candidate));

        assertThat(service.recommendActivities(null, null, 6)).isEmpty();
    }

    @Test
    void changedInterestsChangeRanking() {
        Activity boardGame = activity(30L, "桌游", "SIGNING", "桌游");
        Activity sports = activity(31L, "运动", "SIGNING", "运动");
        when(userService.listInterestNames(CURRENT_USER_ID))
                .thenReturn(List.of("桌游"), List.of("运动"));
        stubCandidates(List.of(boardGame, sports));

        List<RecommendedActivityVO> boardGameRanking = service.recommendActivities(null, null, 2);
        List<RecommendedActivityVO> sportsRanking = service.recommendActivities(null, null, 2);

        assertThat(boardGameRanking.get(0).getActivity().getId()).isEqualTo(boardGame.getId());
        assertThat(sportsRanking.get(0).getActivity().getId()).isEqualTo(sports.getId());
    }

    @Test
    void locationRanksNearerActivityFirst() {
        when(userService.listInterestNames(CURRENT_USER_ID)).thenReturn(List.of());
        Activity near = activity(40L, "近处活动", "SIGNING", "其他");
        near.setLongitude(decimal(116.400));
        near.setLatitude(decimal(39.900));
        Activity far = activity(41L, "远处活动", "SIGNING", "其他");
        far.setLongitude(decimal(116.500));
        far.setLatitude(decimal(40.000));
        stubCandidates(List.of(far, near));

        List<RecommendedActivityVO> result = service.recommendActivities(decimal(116.400), decimal(39.900), 2);

        assertThat(result.get(0).getActivity().getId()).isEqualTo(near.getId());
        assertThat(result.get(0).getDistanceKm()).isEqualByComparingTo("0.00");
    }

    @Test
    void recommendsWithoutUserInterests() {
        when(userService.listInterestNames(CURRENT_USER_ID)).thenReturn(List.of());
        stubCandidates(List.of(activity(50L, "无兴趣冷启动", "SIGNING", "周末")));

        RecommendedActivityVO result = service.recommendActivities(decimal(116.4), decimal(39.9), 1).get(0);

        assertThat(result.getScoreDetail().getInterest()).isNull();
        assertThat(result.getRecommendationScore()).isBetween(decimal(0), decimal(100));
    }

    @Test
    void recommendsWithoutLocation() {
        stubCandidates(List.of(activity(51L, "无定位推荐", "SIGNING", "周末")));

        RecommendedActivityVO result = service.recommendActivities(null, null, 1).get(0);

        assertThat(result.getDistanceKm()).isNull();
        assertThat(result.getScoreDetail().getDistance()).isNull();
    }

    @Test
    void coldStartWorksWithoutInterestAndLocation() {
        when(userService.listInterestNames(CURRENT_USER_ID)).thenReturn(List.of());
        stubCandidates(List.of(activity(52L, "完整冷启动", "SIGNING", "其他")));

        RecommendedActivityVO result = service.recommendActivities(null, null, 1).get(0);

        assertThat(result.getScoreDetail().getInterest()).isNull();
        assertThat(result.getScoreDetail().getDistance()).isNull();
        assertThat(result.getRecommendationScore()).isGreaterThanOrEqualTo(decimal(0));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11})
    void rejectsLimitOutsideAllowedRange(int limit) {
        assertThatThrownBy(() -> service.recommendActivities(null, null, limit))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    void breaksEqualScoreTiesByStartTimeThenActivityId() {
        Activity later = activity(60L, "稍晚", "SIGNING", "周末");
        later.setStartTime(NOW.plusDays(2));
        later.setSignupDeadline(NOW.plusDays(1));
        Activity earlyHigherId = activity(62L, "较早大 ID", "SIGNING", "周末");
        Activity earlyLowerId = activity(61L, "较早小 ID", "SIGNING", "周末");
        stubCandidates(List.of(later, earlyHigherId, earlyLowerId));

        List<RecommendedActivityVO> result = service.recommendActivities(null, null, 3);

        assertThat(result).extracting(item -> item.getActivity().getId()).containsExactly(61L, 62L, 60L);
    }

    @Test
    void redisFailureFallsBackToRealTimeCalculation() {
        reset(redis);
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("redis down"));
        stubCandidates(List.of(activity(70L, "Redis 降级", "SIGNING", "周末")));

        List<RecommendedActivityVO> result = service.recommendActivities(null, null, 1);

        assertThat(result).singleElement().extracting(item -> item.getActivity().getId()).isEqualTo(70L);
    }

    @Test
    void invalidCachedJsonFallsBackToRealTimeCalculation() {
        when(values.get(anyString())).thenReturn("not-json");
        stubCandidates(List.of(activity(71L, "缓存反序列化降级", "SIGNING", "周末")));

        List<RecommendedActivityVO> result = service.recommendActivities(null, null, 1);

        assertThat(result).singleElement().extracting(item -> item.getActivity().getId()).isEqualTo(71L);
    }

    @Test
    void cacheHitAvoidsCandidateAndCreatorQueries() throws Exception {
        RecommendedActivityVO cached = new RecommendedActivityVO();
        ActivityVO activity = new ActivityVO();
        activity.setId(80L);
        cached.setActivity(activity);
        cached.setRecommendationScore(decimal(88.88));
        cached.setReasons(List.of("匹配你的兴趣：周末"));
        cached.setScoreDetail(new RecommendationScoreDetailVO());
        when(values.get(anyString())).thenReturn(objectMapper.writeValueAsString(List.of(cached)));

        List<RecommendedActivityVO> result = service.recommendActivities(decimal(116.4), decimal(39.9), 3);

        assertThat(result).singleElement().extracting(item -> item.getActivity().getId()).isEqualTo(80L);
        verifyNoInteractions(activityMapper, userMapper, activityService);
    }

    @Test
    void missingCreatorCreditDoesNotFailRecommendation() {
        Activity candidate = activity(90L, "发起人缺失", "SIGNING", "周末");
        when(activityMapper.selectList(any())).thenReturn(List.of(candidate));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of());

        RecommendedActivityVO result = service.recommendActivities(null, null, 1).get(0);

        assertThat(result.getScoreDetail().getCredit()).isNull();
    }

    @SuppressWarnings("unchecked")
    private void stubCandidates(List<Activity> candidates) {
        when(activityMapper.selectList(any())).thenReturn(candidates);
        when(userMapper.selectBatchIds(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> creatorIds = invocation.getArgument(0);
            return creatorIds.stream().map(id -> user(id, 100)).toList();
        });
    }

    private Activity activity(Long id, String title, String status, String tags) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setCreatorId(1_000L + id);
        activity.setTitle(title);
        activity.setCategory("其他");
        activity.setTags(tags);
        activity.setStartTime(NOW.plusDays(1));
        activity.setEndTime(NOW.plusDays(1).plusHours(2));
        activity.setSignupDeadline(NOW.plusHours(12));
        activity.setLongitude(decimal(116.4));
        activity.setLatitude(decimal(39.9));
        activity.setMaxParticipants(10);
        activity.setApprovedCount(2);
        activity.setFavoriteCount(1);
        activity.setStatus(status);
        activity.setDeleted(0);
        return activity;
    }

    private ActivitySignup signup(Long activityId, String status) {
        ActivitySignup signup = new ActivitySignup();
        signup.setActivityId(activityId);
        signup.setUserId(CURRENT_USER_ID);
        signup.setStatus(status);
        signup.setDeleted(0);
        return signup;
    }

    private User user(Long id, int creditScore) {
        User user = new User();
        user.setId(id);
        user.setStatus("NORMAL");
        user.setCreditScore(creditScore);
        user.setDeleted(0);
        return user;
    }

    private ActivityVO toVO(Activity source) {
        ActivityVO vo = new ActivityVO();
        vo.setId(source.getId());
        vo.setTitle(source.getTitle());
        vo.setStartTime(source.getStartTime());
        return vo;
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
