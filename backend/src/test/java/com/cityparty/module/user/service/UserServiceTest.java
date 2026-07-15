package com.cityparty.module.user.service;

import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.notice.mapper.SystemNoticeMapper;
import com.cityparty.module.review.mapper.ActivityReviewMapper;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.entity.UserProfile;
import com.cityparty.module.user.dto.UpdateProfileDTO;
import com.cityparty.module.user.mapper.InterestTagMapper;
import com.cityparty.module.user.mapper.UserInterestMapper;
import com.cityparty.module.user.mapper.UserMapper;
import com.cityparty.module.user.mapper.UserProfileMapper;
import com.cityparty.module.waitlist.mapper.ActivityWaitlistMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private InterestTagMapper interestTagMapper;
    @Mock
    private UserInterestMapper userInterestMapper;
    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private ActivitySignupMapper signupMapper;
    @Mock
    private ActivityWaitlistMapper waitlistMapper;
    @Mock
    private ActivityReviewMapper reviewMapper;
    @Mock
    private SystemNoticeMapper noticeMapper;
    @Mock
    private ActivityService activityService;
    @InjectMocks
    private UserService userService;

    @Test
    void profileOverviewAggregatesExistingBusinessTables() {
        User user = new User();
        user.setId(2L);
        user.setUsername("user01");
        user.setCreditScore(102);
        user.setDeleted(0);
        UserProfile profile = new UserProfile();
        profile.setUserId(2L);
        profile.setNickname("周末电影搭子");
        profile.setCity("北京");
        profile.setDeleted(0);

        when(userMapper.selectById(2L)).thenReturn(user);
        when(userProfileMapper.selectOne(any())).thenReturn(profile);
        when(userInterestMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(activityMapper.selectCount(any())).thenReturn(2L);
        when(signupMapper.countJoinedActivities(2L)).thenReturn(3L);
        when(waitlistMapper.countWaitingActivities(2L)).thenReturn(1L);
        when(reviewMapper.selectCount(any())).thenReturn(4L);
        when(reviewMapper.selectAverageRatingByTargetUserId(2L)).thenReturn(new BigDecimal("4.34"));
        when(noticeMapper.selectCount(any())).thenReturn(5L);

        var overview = userService.profileOverview(2L);

        assertThat(overview.getCreditLevel()).isEqualTo("良好");
        assertThat(overview.getPublishedActivityCount()).isEqualTo(2L);
        assertThat(overview.getJoinedActivityCount()).isEqualTo(3L);
        assertThat(overview.getWaitingActivityCount()).isEqualTo(1L);
        assertThat(overview.getReceivedReviewCount()).isEqualTo(4L);
        assertThat(overview.getAverageRating()).isEqualByComparingTo("4.3");
        assertThat(overview.getUnreadNoticeCount()).isEqualTo(5L);
    }

    @Test
    void removesAvatarOnlyWhenExplicitlyRequested() {
        User user = new User();
        user.setId(2L);
        user.setUsername("user01");
        user.setDeleted(0);
        UserProfile profile = new UserProfile();
        profile.setId(5L);
        profile.setUserId(2L);
        profile.setNickname("User 01");
        profile.setAvatarUrl("/uploads/avatar/11111111-1111-1111-1111-111111111111.jpg");
        profile.setDeleted(0);
        when(userMapper.selectById(2L)).thenReturn(user);
        when(userProfileMapper.selectOne(any())).thenReturn(profile);
        when(userInterestMapper.selectList(any())).thenReturn(Collections.emptyList());
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setRemoveAvatar(true);

        var updated = userService.updateProfile(2L, dto);

        assertThat(updated.getAvatarUrl()).isNull();
        assertThat(profile.getAvatarUrl()).isNull();
        verify(userProfileMapper).updateById(profile);
    }
}
