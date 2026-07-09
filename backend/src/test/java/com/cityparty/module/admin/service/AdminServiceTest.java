package com.cityparty.module.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.service.ActivityService;
import com.cityparty.module.credit.mapper.CreditRecordMapper;
import com.cityparty.module.notice.mapper.SystemNoticeMapper;
import com.cityparty.module.report.mapper.ReportMapper;
import com.cityparty.module.review.mapper.ActivityReviewMapper;
import com.cityparty.module.review.service.ActivityReviewService;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.signup.service.SignupService;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.mapper.UserMapper;
import com.cityparty.module.user.mapper.UserProfileMapper;
import com.cityparty.module.user.service.UserService;
import com.cityparty.module.user.vo.UserMeVO;
import com.cityparty.module.waitlist.service.ActivityWaitlistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AdminServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private ActivitySignupMapper signupMapper;
    @Mock
    private ActivityReviewMapper reviewMapper;
    @Mock
    private CreditRecordMapper creditRecordMapper;
    @Mock
    private SystemNoticeMapper noticeMapper;
    @Mock
    private ReportMapper reportMapper;
    @Mock
    private UserService userService;
    @Mock
    private ActivityService activityService;
    @Mock
    private SignupService signupService;
    @Mock
    private ActivityWaitlistService waitlistService;
    @Mock
    private ActivityReviewService reviewService;
    @InjectMocks
    private AdminService adminService;

    @Test
    void dashboardReturnsRequiredStage25Counts() {
        when(userMapper.selectCount(any())).thenReturn(5L);
        when(activityMapper.selectCount(any())).thenReturn(8L);
        when(signupMapper.selectCount(any())).thenReturn(13L);
        when(reviewMapper.selectCount(any())).thenReturn(4L);
        when(noticeMapper.selectCount(any())).thenReturn(9L);

        var result = adminService.dashboard();

        assertThat(result.getUserCount()).isEqualTo(5L);
        assertThat(result.getActivityCount()).isEqualTo(8L);
        assertThat(result.getSignupCount()).isEqualTo(13L);
        assertThat(result.getReviewCount()).isEqualTo(4L);
        assertThat(result.getNoticeCount()).isEqualTo(9L);
    }

    @Test
    void usersReturnsSafeAdminViewAndKeepsPagination() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 9, 12, 0);
        User user = new User();
        user.setId(2L);
        user.setCreatedAt(createdAt);
        user.setDeleted(0);

        UserMeVO detail = new UserMeVO();
        detail.setId(2L);
        detail.setUsername("user01");
        detail.setPhone("13800000001");
        detail.setRole("USER");
        detail.setStatus("NORMAL");
        detail.setCreditScore(100);
        detail.setNickname("周末电影搭子");
        detail.setCity("北京");
        detail.setInterestTags(List.of("电影"));

        Page<User> page = new Page<>(1, 10);
        page.setRecords(List.of(user));
        page.setTotal(1);
        when(userProfileMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(userService.getMe(2L)).thenReturn(detail);

        var result = adminService.users("user01", 1, 10);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getNickname()).isEqualTo("周末电影搭子");
        assertThat(result.getRecords().get(0).getCreatedAt()).isEqualTo(createdAt);
    }
}
