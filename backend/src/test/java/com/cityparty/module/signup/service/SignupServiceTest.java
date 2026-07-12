package com.cityparty.module.signup.service;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.security.LoginUser;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.activity.vo.ActivityVO;
import com.cityparty.module.signup.dto.SignupCreateDTO;
import com.cityparty.module.signup.entity.ActivitySignup;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.user.service.UserService;
import com.cityparty.module.user.vo.UserMeVO;
import com.cityparty.module.waitlist.service.ActivityWaitlistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    @Mock
    private ActivitySignupMapper signupMapper;
    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private ActivityService activityService;
    @Mock
    private UserService userService;
    @Mock
    private ActivityWaitlistService waitlistService;

    private SignupService signupService;

    @BeforeEach
    void setUp() {
        signupService = new SignupService(signupMapper, activityMapper, activityService, userService, waitlistService);
        UserContext.set(new LoginUser(2L, "user02", "USER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createsApprovedSignupWhenSeatIsAvailable() {
        when(activityService.requireActivity(10L)).thenReturn(joinableActivity());
        when(signupMapper.selectOne(any())).thenReturn(null);
        when(activityService.increaseApprovedCountIfAvailable(10L)).thenReturn(true);
        doAnswer(invocation -> {
            ActivitySignup signup = invocation.getArgument(0);
            signup.setId(101L);
            return 1;
        }).when(signupMapper).insert(any(ActivitySignup.class));
        prepareVoMapping();

        SignupCreateDTO dto = new SignupCreateDTO();
        dto.setApplyMessage("I can arrive on time.");
        var result = signupService.signup(10L, dto);

        ArgumentCaptor<ActivitySignup> captor = ArgumentCaptor.forClass(ActivitySignup.class);
        verify(signupMapper).insert(captor.capture());
        ActivitySignup saved = captor.getValue();
        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(saved.getStatus()).isEqualTo("APPROVED");
        assertThat(saved.getActivityId()).isEqualTo(10L);
        assertThat(saved.getUserId()).isEqualTo(2L);
        assertThat(saved.getApplyMessage()).isEqualTo("I can arrive on time.");
        assertThat(saved.getDeleted()).isZero();
    }

    @Test
    void rejectsDuplicateActiveSignup() {
        when(activityService.requireActivity(10L)).thenReturn(joinableActivity());
        when(signupMapper.selectOne(any())).thenReturn(signup("APPROVED"));

        assertThatThrownBy(() -> signupService.signup(10L, new SignupCreateDTO()))
                .isInstanceOf(BusinessException.class);

        verify(signupMapper, never()).insert(any(ActivitySignup.class));
        verify(activityService, never()).increaseApprovedCountIfAvailable(anyLong());
    }

    @Test
    void rejectsWhenConcurrentUserTakesLastSeatFirst() {
        Activity activity = joinableActivity();
        activity.setApprovedCount(1);
        activity.setMaxParticipants(2);
        when(activityService.requireActivity(10L)).thenReturn(activity);
        when(signupMapper.selectOne(any())).thenReturn(null);
        when(activityService.increaseApprovedCountIfAvailable(10L)).thenReturn(false);

        assertThatThrownBy(() -> signupService.signup(10L, new SignupCreateDTO()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(409);
    }

    @Test
    void reviewApprovesPendingSignupAndRecordsReviewTime() {
        UserContext.set(new LoginUser(9L, "creator", "USER"));
        ActivitySignup pending = signup("PENDING");
        pending.setId(301L);
        when(signupMapper.selectById(301L)).thenReturn(pending);
        Activity activity = joinableActivity();
        activity.setCreatorId(9L);
        activity.setApprovedCount(1);
        activity.setMaxParticipants(2);
        when(activityService.requireActivity(10L)).thenReturn(activity);
        when(activityService.increaseApprovedCountIfAvailable(10L)).thenReturn(true);
        prepareVoMapping();

        var result = signupService.review(301L, "APPROVED");

        ArgumentCaptor<ActivitySignup> captor = ArgumentCaptor.forClass(ActivitySignup.class);
        verify(signupMapper).updateById(captor.capture());
        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(captor.getValue().getStatus()).isEqualTo("APPROVED");
        assertThat(captor.getValue().getReviewedAt()).isNotNull();
        verify(activityService).increaseApprovedCountIfAvailable(10L);
    }

    @Test
    void rejectsReviewByNonCreator() {
        ActivitySignup pending = signup("PENDING");
        pending.setId(301L);
        when(signupMapper.selectById(301L)).thenReturn(pending);
        Activity activity = joinableActivity();
        activity.setCreatorId(9L);
        when(activityService.requireActivity(10L)).thenReturn(activity);

        assertThatThrownBy(() -> signupService.review(301L, "APPROVED"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    @Test
    void cancelApprovedSignupFreesSeatAndPromotesWaitlist() {
        ActivitySignup approved = signup("APPROVED");
        approved.setId(401L);
        when(signupMapper.selectOne(any())).thenReturn(approved);
        Activity activity = joinableActivity();
        activity.setApprovedCount(1);
        when(activityService.requireActivity(10L)).thenReturn(activity);
        prepareVoMapping();

        var result = signupService.cancel(10L);

        ArgumentCaptor<ActivitySignup> captor = ArgumentCaptor.forClass(ActivitySignup.class);
        verify(signupMapper).updateById(captor.capture());
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(captor.getValue().getStatus()).isEqualTo("CANCELLED");
        verify(activityService).decreaseApprovedCount(10L);
        verify(waitlistService).promoteNextIfAvailable(10L);
    }

    @Test
    void rejectsSignupAfterActivityCancelledOrFinished() {
        Activity activity = joinableActivity();
        activity.setStatus("CANCELLED");
        when(activityService.requireActivity(10L)).thenReturn(activity);

        assertThatThrownBy(() -> signupService.signup(10L, new SignupCreateDTO()))
                .isInstanceOf(BusinessException.class);
    }

    private void prepareVoMapping() {
        ActivityVO activityVO = new ActivityVO();
        activityVO.setId(10L);
        when(activityMapper.selectById(10L)).thenReturn(joinableActivity());
        when(activityService.toVO(any(Activity.class))).thenReturn(activityVO);
        when(userService.getMe(anyLong())).thenReturn(new UserMeVO());
    }

    private Activity joinableActivity() {
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setCreatorId(1L);
        activity.setStatus("SIGNING");
        activity.setNeedApproval(0);
        activity.setApprovedCount(0);
        activity.setMaxParticipants(2);
        activity.setDeleted(0);
        return activity;
    }

    private ActivitySignup signup(String status) {
        ActivitySignup signup = new ActivitySignup();
        signup.setActivityId(10L);
        signup.setUserId(2L);
        signup.setStatus(status);
        signup.setDeleted(0);
        return signup;
    }
}
