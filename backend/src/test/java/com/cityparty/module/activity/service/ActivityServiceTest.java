package com.cityparty.module.activity.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.security.LoginUser;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.activity.dto.ActivityCreateDTO;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.mapper.ActivityTagMapper;
import com.cityparty.module.favorite.mapper.ActivityFavoriteMapper;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.user.mapper.UserMapper;
import com.cityparty.module.user.mapper.UserProfileMapper;
import com.cityparty.module.waitlist.mapper.ActivityWaitlistMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private ActivityTagMapper activityTagMapper;
    @Mock
    private ActivitySignupMapper signupMapper;
    @Mock
    private ActivityFavoriteMapper favoriteMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private ActivityWaitlistMapper waitlistMapper;
    @Mock
    private PublicActivityCacheService publicActivityCacheService;
    @InjectMocks
    private ActivityService activityService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                Activity.class
        );
    }

    @BeforeEach
    void setUp() {
        UserContext.set(new LoginUser(2L, "user01", "USER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"published", "joined", "waiting", "finished"})
    void queriesSupportedMyActivityType(String type) {
        Page<Activity> page = new Page<>(1, 10);
        page.setRecords(java.util.Collections.emptyList());
        page.setTotal(0);
        when(activityMapper.selectMyActivities(any(), eq(2L), eq(type), any())).thenReturn(page);

        var result = activityService.myActivities(type, 1, 10);

        assertThat(result.getRecords()).isEmpty();
        verify(activityMapper).selectMyActivities(any(), eq(2L), eq(type), any());
    }

    @Test
    void rejectsUnsupportedMyActivityType() {
        assertThatThrownBy(() -> activityService.myActivities("cancelled", 1, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("type");
    }

    @Test
    void rejectsCreateWhenEndTimeIsNotAfterStartTime() {
        ActivityCreateDTO dto = activityDto();
        dto.setEndTime(dto.getStartTime());

        assertThatThrownBy(() -> activityService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("end time");

        verify(activityMapper, never()).insert(any(Activity.class));
    }

    @Test
    void createsEveryUserActivityAsPending() {
        ActivityCreateDTO dto = activityDto();
        dto.setNeedApproval(false);

        activityService.create(dto);

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).insert(captor.capture());
        Activity saved = captor.getValue();
        assertThat(saved.getAuditStatus()).isEqualTo("PENDING");
        assertThat(saved.getRejectReason()).isNull();
        assertThat(saved.getAuditTime()).isNull();
        assertThat(saved.getReviewerId()).isNull();
    }

    @Test
    void publicListCombinesAuditKeywordCategoryAndAuditTimeOrder() {
        Page<Activity> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(java.util.Collections.emptyList());
        when(activityMapper.selectPage(any(), any())).thenReturn(emptyPage);

        activityService.page("万达", "观影", null, null, null, 1, 10);

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Activity>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(activityMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertThat(sql).contains("audit_status", "category", "address", "audit_time");
        assertThat(sql).containsIgnoringCase("ORDER BY");
    }

    @Test
    void nearbyListRequiresApprovedAuditStatus() {
        when(activityMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());

        activityService.nearby(
                java.math.BigDecimal.valueOf(116.4),
                java.math.BigDecimal.valueOf(39.9),
                java.math.BigDecimal.valueOf(5),
                null,
                null,
                null,
                1,
                10
        );

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Activity>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(activityMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("audit_status");
    }

    @Test
    void pendingDetailIsHiddenFromOtherUsers() {
        Activity activity = manageableActivity();
        activity.setCreatorId(99L);
        activity.setAuditStatus("PENDING");
        when(activityMapper.selectById(activity.getId())).thenReturn(activity);

        assertThatThrownBy(() -> activityService.detail(activity.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(404);
    }

    @Test
    void rejectedActivityEditResubmitsAsPendingAndClearsAuditMetadata() {
        Activity activity = manageableActivity();
        activity.setAuditStatus("REJECTED");
        activity.setRejectReason("地点信息不完整");
        activity.setAuditTime(LocalDateTime.now().minusDays(1));
        activity.setReviewerId(8L);
        when(activityMapper.selectById(activity.getId())).thenReturn(activity);
        when(waitlistMapper.selectCount(any())).thenReturn(0L);
        when(favoriteMapper.selectCount(any())).thenReturn(0L);

        activityService.update(activity.getId(), activityDto());

        assertThat(activity.getAuditStatus()).isEqualTo("PENDING");
        assertThat(activity.getRejectReason()).isNull();
        assertThat(activity.getAuditTime()).isNull();
        assertThat(activity.getReviewerId()).isNull();
    }

    @Test
    void rejectsUpdateWhenSignupDeadlineIsAfterStartTime() {
        Activity activity = manageableActivity();
        ActivityCreateDTO dto = activityDto();
        dto.setSignupDeadline(dto.getStartTime().plusMinutes(1));
        when(activityMapper.selectById(activity.getId())).thenReturn(activity);

        assertThatThrownBy(() -> activityService.update(activity.getId(), dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Signup deadline");

        verify(activityMapper, never()).updateById(any(Activity.class));
    }

    @Test
    void rejectsUpdateByNonCreator() {
        Activity activity = manageableActivity();
        activity.setCreatorId(99L);
        when(activityMapper.selectById(activity.getId())).thenReturn(activity);

        assertThatThrownBy(() -> activityService.update(activity.getId(), activityDto()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);

        verify(activityMapper, never()).updateById(any(Activity.class));
    }

    @Test
    void rejectsUpdateAfterActivityCancelled() {
        Activity activity = manageableActivity();
        activity.setStatus("CANCELLED");
        when(activityMapper.selectById(activity.getId())).thenReturn(activity);

        assertThatThrownBy(() -> activityService.update(activity.getId(), activityDto()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be edited");

        verify(activityMapper, never()).updateById(any(Activity.class));
    }

    @Test
    void rejectsCancelAfterActivityFinished() {
        Activity activity = manageableActivity();
        activity.setStatus("FINISHED");
        when(activityMapper.selectById(activity.getId())).thenReturn(activity);

        assertThatThrownBy(() -> activityService.cancel(activity.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Finished activity");

        verify(activityMapper, never()).updateById(any(Activity.class));
    }

    @Test
    void rejectsFinishAfterActivityCancelled() {
        Activity activity = manageableActivity();
        activity.setStatus("CANCELLED");
        when(activityMapper.selectById(activity.getId())).thenReturn(activity);

        assertThatThrownBy(() -> activityService.finish(activity.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cancelled activity");

        verify(activityMapper, never()).updateById(any(Activity.class));
    }

    @Test
    void rejectsFinishBeforeActivityStartsWithoutChangingActivity() {
        Activity activity = manageableActivity();
        activity.setStartTime(LocalDateTime.now().plusHours(1));
        activity.setEndTime(activity.getStartTime().plusHours(2));
        when(activityMapper.selectById(activity.getId())).thenReturn(activity);

        assertThatThrownBy(() -> activityService.finish(activity.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("before it starts");

        assertThat(activity.getStatus()).isEqualTo("SIGNING");
        verify(activityMapper, never()).updateById(any(Activity.class));
    }

    @Test
    void finishesStartedActivityWithoutCreatingAnInvalidInterval() {
        Activity activity = manageableActivity();
        LocalDateTime startTime = LocalDateTime.now().minusHours(1).withNano(0);
        LocalDateTime plannedEndTime = LocalDateTime.now().plusHours(1).withNano(0);
        activity.setStartTime(startTime);
        activity.setEndTime(plannedEndTime);
        when(activityMapper.selectById(activity.getId())).thenReturn(activity);

        activityService.finish(activity.getId());

        assertThat(activity.getStatus()).isEqualTo("FINISHED");
        assertThat(activity.getEndTime()).isAfter(startTime).isBefore(plannedEndTime);
        verify(activityMapper).updateById(activity);
    }

    @Test
    void repairsLegacyInvalidIntervalWhenFinishingStartedActivity() {
        Activity activity = manageableActivity();
        LocalDateTime startTime = LocalDateTime.now().minusHours(1).withNano(0);
        activity.setStartTime(startTime);
        activity.setEndTime(startTime.minusHours(1));
        when(activityMapper.selectById(activity.getId())).thenReturn(activity);

        activityService.finish(activity.getId());

        assertThat(activity.getStatus()).isEqualTo("FINISHED");
        assertThat(activity.getEndTime()).isAfter(startTime);
        verify(activityMapper).updateById(activity);
    }

    private ActivityCreateDTO activityDto() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withNano(0);
        ActivityCreateDTO dto = new ActivityCreateDTO();
        dto.setStartTime(startTime);
        dto.setEndTime(startTime.plusHours(2));
        dto.setSignupDeadline(startTime.minusHours(1));
        dto.setMinParticipants(2);
        dto.setMaxParticipants(6);
        return dto;
    }

    private Activity manageableActivity() {
        Activity activity = new Activity();
        activity.setId(25L);
        activity.setCreatorId(2L);
        activity.setStatus("SIGNING");
        activity.setApprovedCount(0);
        activity.setMaxParticipants(3);
        activity.setDeleted(0);
        return activity;
    }
}
