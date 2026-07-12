package com.cityparty.module.waitlist.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.security.LoginUser;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.notice.service.SystemNoticeService;
import com.cityparty.module.signup.entity.ActivitySignup;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.user.service.UserService;
import com.cityparty.module.user.vo.UserMeVO;
import com.cityparty.module.waitlist.entity.ActivityWaitlist;
import com.cityparty.module.waitlist.mapper.ActivityWaitlistMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityWaitlistServiceTest {

    @Mock
    private ActivityWaitlistMapper waitlistMapper;
    @Mock
    private ActivitySignupMapper signupMapper;
    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private ActivityService activityService;
    @Mock
    private UserService userService;
    @Mock
    private SystemNoticeService noticeService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ListOperations<String, String> listOperations;

    private ActivityWaitlistService waitlistService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                ActivityWaitlist.class
        );
    }

    @BeforeEach
    void setUp() {
        waitlistService = new ActivityWaitlistService(
                waitlistMapper,
                signupMapper,
                activityMapper,
                activityService,
                userService,
                noticeService,
                stringRedisTemplate
        );
        UserContext.set(new LoginUser(3L, "wait-user", "USER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void joinWaitlistCreatesWaitingSignupAndQueueEntry() {
        Activity activity = fullActivity();
        when(activityService.requireActivity(10L)).thenReturn(activity);
        when(signupMapper.selectOne(any())).thenReturn(null);
        when(waitlistMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            ActivityWaitlist waitlist = invocation.getArgument(0);
            waitlist.setId(501L);
            return 1;
        }).when(waitlistMapper).insert(any(ActivityWaitlist.class));
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        when(activityMapper.selectById(10L)).thenReturn(activity);
        when(userService.getMe(3L)).thenReturn(new UserMeVO());

        var result = waitlistService.joinWaitlist(10L);

        ArgumentCaptor<ActivitySignup> signupCaptor = ArgumentCaptor.forClass(ActivitySignup.class);
        ArgumentCaptor<ActivityWaitlist> waitlistCaptor = ArgumentCaptor.forClass(ActivityWaitlist.class);
        verify(signupMapper).insert(signupCaptor.capture());
        verify(waitlistMapper).insert(waitlistCaptor.capture());
        ActivitySignup savedSignup = signupCaptor.getValue();
        ActivityWaitlist savedWaitlist = waitlistCaptor.getValue();
        assertThat(savedSignup.getStatus()).isEqualTo("WAITING");
        assertThat(savedSignup.getActivityId()).isEqualTo(10L);
        assertThat(savedSignup.getUserId()).isEqualTo(3L);
        assertThat(savedWaitlist.getStatus()).isEqualTo("WAITING");
        assertThat(savedWaitlist.getQueueNo()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("WAITING");
        verify(listOperations).rightPush("activity:waitlist:10", "501");
    }

    @Test
    void joinWaitlistRejectsWhenNewSignupCanStillTakeSeat() {
        Activity activity = fullActivity();
        activity.setApprovedCount(1);
        activity.setMaxParticipants(2);
        when(activityService.requireActivity(10L)).thenReturn(activity);

        assertThatThrownBy(() -> waitlistService.joinWaitlist(10L))
                .isInstanceOf(BusinessException.class);

        verify(signupMapper, never()).insert(any(ActivitySignup.class));
        verify(waitlistMapper, never()).insert(any(ActivityWaitlist.class));
    }

    @Test
    void promoteNextFallsBackToMysqlWhenRedisIsUnavailable() {
        Activity activity = fullActivity();
        activity.setApprovedCount(1);
        activity.setMaxParticipants(2);
        ActivityWaitlist waiting = waitlist(501L, "WAITING");
        ActivitySignup signup = signup("WAITING");
        when(activityService.requireActivity(10L)).thenReturn(activity);
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("activity:waitlist:10")).thenThrow(new RuntimeException("redis down"));
        when(waitlistMapper.selectOne(any())).thenReturn(waiting);
        when(activityService.increaseApprovedCountIfAvailable(10L)).thenReturn(true);
        when(waitlistMapper.update(any(), any())).thenReturn(1);
        when(signupMapper.selectOne(any())).thenReturn(signup);
        Activity latest = fullActivity();
        latest.setTitle("Demo activity");
        when(activityService.requireActivity(10L)).thenReturn(activity, latest);

        waitlistService.promoteNextIfAvailable(10L);

        ArgumentCaptor<ActivitySignup> captor = ArgumentCaptor.forClass(ActivitySignup.class);
        verify(signupMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("APPROVED");
        assertThat(captor.getValue().getReviewedAt()).isNotNull();
        verify(noticeService).createWaitlistPromotedNotice(3L, 10L, "Demo activity");
    }

    @Test
    void promoteNextRollsBackSeatWhenWaitlistRowWasAlreadyChanged() {
        Activity activity = fullActivity();
        activity.setApprovedCount(1);
        activity.setMaxParticipants(2);
        ActivityWaitlist waiting = waitlist(501L, "WAITING");
        when(activityService.requireActivity(10L)).thenReturn(activity);
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("activity:waitlist:10")).thenReturn("501");
        when(waitlistMapper.selectById(501L)).thenReturn(waiting);
        when(activityService.increaseApprovedCountIfAvailable(10L)).thenReturn(true);
        when(waitlistMapper.update(any(), any())).thenReturn(0);

        waitlistService.promoteNextIfAvailable(10L);

        verify(activityService).decreaseApprovedCount(10L);
        verify(signupMapper, never()).insert(any(ActivitySignup.class));
        verify(signupMapper, never()).updateById(any(ActivitySignup.class));
    }

    private Activity fullActivity() {
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setCreatorId(1L);
        activity.setTitle("Activity");
        activity.setStatus("FULL");
        activity.setApprovedCount(2);
        activity.setMaxParticipants(2);
        activity.setDeleted(0);
        return activity;
    }

    private ActivitySignup signup(String status) {
        ActivitySignup signup = new ActivitySignup();
        signup.setId(301L);
        signup.setActivityId(10L);
        signup.setUserId(3L);
        signup.setStatus(status);
        signup.setDeleted(0);
        return signup;
    }

    private ActivityWaitlist waitlist(Long id, String status) {
        ActivityWaitlist waitlist = new ActivityWaitlist();
        waitlist.setId(id);
        waitlist.setActivityId(10L);
        waitlist.setUserId(3L);
        waitlist.setStatus(status);
        waitlist.setQueueNo(1L);
        waitlist.setDeleted(0);
        return waitlist;
    }
}
