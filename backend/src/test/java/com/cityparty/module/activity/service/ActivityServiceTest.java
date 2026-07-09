package com.cityparty.module.activity.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.security.LoginUser;
import com.cityparty.common.security.UserContext;
import com.cityparty.module.activity.entity.Activity;
import com.cityparty.module.activity.mapper.ActivityMapper;
import com.cityparty.module.activity.mapper.ActivityTagMapper;
import com.cityparty.module.favorite.mapper.ActivityFavoriteMapper;
import com.cityparty.module.signup.mapper.ActivitySignupMapper;
import com.cityparty.module.user.mapper.UserMapper;
import com.cityparty.module.user.mapper.UserProfileMapper;
import com.cityparty.module.waitlist.mapper.ActivityWaitlistMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @InjectMocks
    private ActivityService activityService;

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
}
