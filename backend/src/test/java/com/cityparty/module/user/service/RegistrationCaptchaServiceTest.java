package com.cityparty.module.user.service;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.module.user.dto.RegistrationCaptchaVerifyDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationCaptchaServiceTest {

    private static final String CLIENT_IP = "127.0.0.1";
    private static final String CHALLENGE_PREFIX = "city-party:security:register-captcha:challenge:";
    private static final String TOKEN_PREFIX = "city-party:security:register-captcha:token:";

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> values;
    @Mock
    private RegistrationRateLimitService rateLimitService;

    private final Map<String, String> storage = new ConcurrentHashMap<>();
    private RegistrationCaptchaService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(invocation -> storage.get(invocation.getArgument(0)));
        lenient().when(values.getAndDelete(anyString()))
                .thenAnswer(invocation -> storage.remove(invocation.getArgument(0)));
        doAnswer(invocation -> {
            storage.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), any(Duration.class));
        lenient().when(redis.getExpire(anyString(), any(java.util.concurrent.TimeUnit.class))).thenReturn(60L);
        lenient().doAnswer(invocation -> storage.remove(invocation.getArgument(0)))
                .when(redis).delete(anyString());
        service = new RegistrationCaptchaService(redis, rateLimitService);
    }

    @Test
    void correctChallengeCreatesOneTimeRegistrationToken() {
        var challenge = service.createChallenge(CLIENT_IP);

        assertThat(challenge.getBackgroundImage()).startsWith("data:image/png;base64,");
        assertThat(challenge.getSliderImage()).startsWith("data:image/png;base64,");
        var verified = service.verify(correctDto(challenge.getChallengeId()), CLIENT_IP);

        assertThat(verified.getCaptchaToken()).hasSize(32);
        assertThat(storage).containsKey(TOKEN_PREFIX + verified.getCaptchaToken());
        service.consumeToken(verified.getCaptchaToken(), CLIENT_IP);
        assertThat(storage).doesNotContainKey(TOKEN_PREFIX + verified.getCaptchaToken());
    }

    @Test
    void wrongSliderPositionFailsVerification() {
        var challenge = service.createChallenge(CLIENT_IP);
        RegistrationCaptchaVerifyDTO dto = correctDto(challenge.getChallengeId());
        dto.setOffsetX(dto.getOffsetX() + 20);
        dto.setTrace(List.of(0, dto.getOffsetX() / 2, dto.getOffsetX()));

        assertThatThrownBy(() -> service.verify(dto, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("滑块验证失败");
    }

    @Test
    void expiredChallengeCannotBeVerified() {
        var challenge = service.createChallenge(CLIENT_IP);
        RegistrationCaptchaVerifyDTO dto = correctDto(challenge.getChallengeId());
        storage.remove(CHALLENGE_PREFIX + challenge.getChallengeId());

        assertThatThrownBy(() -> service.verify(dto, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已过期");
    }

    @Test
    void verifiedChallengeCannotBeReplayed() {
        var challenge = service.createChallenge(CLIENT_IP);
        RegistrationCaptchaVerifyDTO dto = correctDto(challenge.getChallengeId());
        service.verify(dto, CLIENT_IP);

        assertThatThrownBy(() -> service.verify(dto, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已过期");
    }

    @Test
    void registrationTokenCannotBeReplayed() {
        var challenge = service.createChallenge(CLIENT_IP);
        var verified = service.verify(correctDto(challenge.getChallengeId()), CLIENT_IP);
        service.consumeToken(verified.getCaptchaToken(), CLIENT_IP);

        assertThatThrownBy(() -> service.consumeToken(verified.getCaptchaToken(), CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已过期或已使用");
    }

    private RegistrationCaptchaVerifyDTO correctDto(String challengeId) {
        String state = storage.get(CHALLENGE_PREFIX + challengeId);
        int targetX = Integer.parseInt(state.split("\\|")[0]);
        RegistrationCaptchaVerifyDTO dto = new RegistrationCaptchaVerifyDTO();
        dto.setChallengeId(challengeId);
        dto.setOffsetX(targetX);
        dto.setDurationMs(800L);
        dto.setTrace(List.of(0, targetX / 2, targetX));
        return dto;
    }
}
