package com.cityparty.module.review.service;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.security.LoginUser;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.credit.entity.CreditRecord;
import com.cityparty.module.credit.mapper.CreditRecordMapper;
import com.cityparty.module.notice.service.SystemNoticeService;
import com.cityparty.module.review.dto.ReviewCreateDTO;
import com.cityparty.module.review.entity.ActivityReview;
import com.cityparty.module.review.mapper.ActivityReviewMapper;
import com.cityparty.module.review.vo.ActivityReviewVO;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.mapper.UserMapper;
import com.cityparty.module.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityReviewServiceTest {

    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private ActivitySignupMapper signupMapper;
    @Mock
    private ActivityReviewMapper reviewMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private CreditRecordMapper creditRecordMapper;
    @Mock
    private SystemNoticeService noticeService;
    @InjectMocks
    private ActivityReviewService reviewService;

    @BeforeEach
    void setUp() {
        UserContext.set(new LoginUser(2L, "user01", "USER"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void rejectsReviewBeforeActivityEnds() {
        Activity activity = activity(LocalDateTime.now().plusHours(1));
        when(activityMapper.selectById(1L)).thenReturn(activity);

        assertThatThrownBy(() -> reviewService.create(1L, request(3L, 5)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("尚未结束");

        verify(reviewMapper, never()).insert(any(ActivityReview.class));
    }

    @Test
    void rejectsSelfReview() {
        when(activityMapper.selectById(1L)).thenReturn(activity(LocalDateTime.now().minusHours(1)));

        assertThatThrownBy(() -> reviewService.create(1L, request(2L, 5)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能评价自己");
    }

    @Test
    void rejectsDuplicateReview() {
        prepareMemberChecks();
        when(reviewMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> reviewService.create(1L, request(3L, 5)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能重复评价");
    }

    @Test
    void rejectsInvalidRating() {
        assertThatThrownBy(() -> reviewService.create(1L, request(3L, 6)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1 到 5");

        verify(activityMapper, never()).selectById(any());
    }

    @Test
    void rejectsTargetWhoIsNotActivityMember() {
        when(activityMapper.selectById(1L)).thenReturn(activity(LocalDateTime.now().minusHours(1)));
        when(signupMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> reviewService.create(1L, request(3L, 5)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不是该活动成员");
    }

    @Test
    void savesReviewUpdatesCreditWritesLogAndCreatesNotice() {
        prepareSuccessfulCreate(user(3L, 96));

        ActivityReviewVO result = reviewService.create(1L, request(3L, 5));

        assertThat(result.getId()).isEqualTo(101L);
        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getCreditDelta()).isEqualTo(2);

        ArgumentCaptor<ActivityReview> reviewCaptor = ArgumentCaptor.forClass(ActivityReview.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().getTargetUserId()).isEqualTo(3L);
        assertThat(reviewCaptor.getValue().getTags()).isEqualTo("准时,友好");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertThat(userCaptor.getValue().getCreditScore()).isEqualTo(98);

        ArgumentCaptor<CreditRecord> creditCaptor = ArgumentCaptor.forClass(CreditRecord.class);
        verify(creditRecordMapper).insert(creditCaptor.capture());
        assertThat(creditCaptor.getValue().getBeforeScore()).isEqualTo(96);
        assertThat(creditCaptor.getValue().getAfterScore()).isEqualTo(98);
        assertThat(creditCaptor.getValue().getChangeScore()).isEqualTo(2);
        assertThat(creditCaptor.getValue().getSourceType()).isEqualTo("ACTIVITY_REVIEW");
        assertThat(creditCaptor.getValue().getSourceId()).isEqualTo(101L);

        verify(noticeService).createActivityReviewNotice(3L, 1L, "Stage2.3 Review Acceptance", 5, 2);
    }

    @Test
    void clampsCreditAtUpperBoundaryAndStillWritesZeroChangeLog() {
        prepareSuccessfulCreate(user(3L, 120));

        ActivityReviewVO result = reviewService.create(1L, request(3L, 5));

        assertThat(result.getCreditDelta()).isZero();
        ArgumentCaptor<CreditRecord> creditCaptor = ArgumentCaptor.forClass(CreditRecord.class);
        verify(creditRecordMapper).insert(creditCaptor.capture());
        assertThat(creditCaptor.getValue().getBeforeScore()).isEqualTo(120);
        assertThat(creditCaptor.getValue().getAfterScore()).isEqualTo(120);
        assertThat(creditCaptor.getValue().getChangeScore()).isZero();
    }

    @Test
    void clampsCreditAtLowerBoundary() {
        prepareSuccessfulCreate(user(3L, 61));

        ActivityReviewVO result = reviewService.create(1L, request(3L, 1));

        assertThat(result.getCreditDelta()).isEqualTo(-1);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertThat(userCaptor.getValue().getCreditScore()).isEqualTo(60);
    }

    private void prepareMemberChecks() {
        when(activityMapper.selectById(1L)).thenReturn(activity(LocalDateTime.now().minusHours(1)));
        when(signupMapper.selectCount(any())).thenReturn(1L);
    }

    private void prepareSuccessfulCreate(User targetUser) {
        prepareMemberChecks();
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.selectOne(any())).thenReturn(targetUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        doAnswer(invocation -> {
            ActivityReview review = invocation.getArgument(0);
            review.setId(101L);
            return 1;
        }).when(reviewMapper).insert(any(ActivityReview.class));
        when(creditRecordMapper.insert(any(CreditRecord.class))).thenReturn(1);
        when(activityMapper.selectById(1L)).thenReturn(activity(LocalDateTime.now().minusHours(1)));
        when(userMapper.selectById(2L)).thenReturn(user(2L, 100));
        when(userMapper.selectById(3L)).thenReturn(targetUser);
        when(userProfileMapper.selectOne(any())).thenReturn(null);
    }

    private Activity activity(LocalDateTime endTime) {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setCreatorId(2L);
        activity.setTitle("Stage2.3 Review Acceptance");
        activity.setEndTime(endTime);
        activity.setDeleted(0);
        return activity;
    }

    private User user(Long id, Integer creditScore) {
        User user = new User();
        user.setId(id);
        user.setUsername(id.equals(2L) ? "user01" : "user02");
        user.setCreditScore(creditScore);
        user.setDeleted(0);
        return user;
    }

    private ReviewCreateDTO request(Long targetUserId, Integer rating) {
        ReviewCreateDTO dto = new ReviewCreateDTO();
        dto.setTargetUserId(targetUserId);
        dto.setRating(rating);
        dto.setContent("准时到场，沟通顺畅");
        dto.setTags(List.of("准时", "友好"));
        return dto;
    }
}
