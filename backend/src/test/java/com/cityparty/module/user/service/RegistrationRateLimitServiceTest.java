package com.cityparty.module.user.service;

import com.cityparty.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class RegistrationRateLimitServiceTest {

    @Mock
    private StringRedisTemplate redis;

    private RegistrationRateLimitService service;

    @BeforeEach
    void setUp() {
        service = new RegistrationRateLimitService(redis);
        ReflectionTestUtils.setField(service, "registrationMaxAttempts", 5);
        ReflectionTestUtils.setField(service, "registrationWindowSeconds", 600L);
    }

    @Test
    void registrationIsRejectedAfterIpWindowLimit() {
        doReturn(6L).when(redis).execute(any(RedisScript.class), anyList(), any(String.class));

        assertThatThrownBy(() -> service.checkRegistration("203.0.113.10"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(429);
    }
}
