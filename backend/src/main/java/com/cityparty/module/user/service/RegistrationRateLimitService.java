package com.cityparty.module.user.service;

import com.cityparty.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationRateLimitService {

    private static final String PREFIX = "city-party:security:rate:";
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """,
            Long.class
    );

    private final StringRedisTemplate redis;

    @Value("${city-party.security.registration.max-attempts:5}")
    private int registrationMaxAttempts;

    @Value("${city-party.security.registration.window-seconds:600}")
    private long registrationWindowSeconds;

    @Value("${city-party.security.registration.challenge-max-attempts:20}")
    private int challengeMaxAttempts;

    @Value("${city-party.security.registration.challenge-window-seconds:300}")
    private long challengeWindowSeconds;

    @Value("${city-party.security.registration.verify-max-attempts:30}")
    private int verifyMaxAttempts;

    @Value("${city-party.security.registration.verify-window-seconds:300}")
    private long verifyWindowSeconds;

    public void checkRegistration(String clientIp) {
        check("register", clientIp, registrationMaxAttempts, registrationWindowSeconds);
    }

    public void checkChallenge(String clientIp) {
        check("challenge", clientIp, challengeMaxAttempts, challengeWindowSeconds);
    }

    public void checkVerification(String clientIp) {
        check("verify", clientIp, verifyMaxAttempts, verifyWindowSeconds);
    }

    private void check(String action, String clientIp, int maxAttempts, long windowSeconds) {
        try {
            String key = PREFIX + action + ":" + fingerprint(clientIp);
            Long attempts = redis.execute(
                    INCREMENT_SCRIPT,
                    List.of(key),
                    Long.toString(windowSeconds)
            );
            if (attempts == null) {
                throw new IllegalStateException("Redis returned no rate-limit result");
            }
            if (attempts > maxAttempts) {
                throw new BusinessException(429, "操作过于频繁，请稍后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Registration rate limiter unavailable", e);
            throw new BusinessException(503, "安全校验服务暂不可用，请稍后再试");
        }
    }

    String fingerprint(String clientIp) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(clientIp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
