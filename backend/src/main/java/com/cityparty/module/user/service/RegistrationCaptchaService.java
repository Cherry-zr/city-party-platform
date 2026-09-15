package com.cityparty.module.user.service;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.module.user.dto.RegistrationCaptchaVerifyDTO;
import com.cityparty.module.user.vo.RegistrationCaptchaChallengeVO;
import com.cityparty.module.user.vo.RegistrationCaptchaVerifyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationCaptchaService {

    private static final String CHALLENGE_PREFIX = "city-party:security:register-captcha:challenge:";
    private static final String TOKEN_PREFIX = "city-party:security:register-captcha:token:";
    private static final int IMAGE_WIDTH = 320;
    private static final int IMAGE_HEIGHT = 160;
    private static final int SLIDER_SIZE = 44;
    private static final int MAX_CHALLENGE_ATTEMPTS = 5;
    private static final int POSITION_TOLERANCE = 6;
    private static final long CHALLENGE_TTL_SECONDS = 120;
    private static final long TOKEN_TTL_SECONDS = 300;

    private final StringRedisTemplate redis;
    private final RegistrationRateLimitService rateLimitService;
    private final SecureRandom secureRandom = new SecureRandom();

    public RegistrationCaptchaChallengeVO createChallenge(String clientIp) {
        rateLimitService.checkChallenge(clientIp);
        String challengeId = randomId();
        int targetX = 70 + secureRandom.nextInt(IMAGE_WIDTH - SLIDER_SIZE - 100);
        int targetY = 25 + secureRandom.nextInt(IMAGE_HEIGHT - SLIDER_SIZE - 50);
        CaptchaImages images = generateImages(targetX, targetY);
        ChallengeState state = new ChallengeState(
                targetX,
                fingerprint(clientIp),
                Instant.now().getEpochSecond(),
                0
        );
        try {
            redis.opsForValue().set(
                    CHALLENGE_PREFIX + challengeId,
                    state.serialize(),
                    Duration.ofSeconds(CHALLENGE_TTL_SECONDS)
            );
        } catch (RuntimeException e) {
            log.error("Unable to persist registration captcha challenge", e);
            throw new BusinessException(503, "安全校验服务暂不可用，请稍后再试");
        }
        return new RegistrationCaptchaChallengeVO(
                challengeId,
                images.backgroundImage(),
                images.sliderImage(),
                IMAGE_WIDTH,
                IMAGE_HEIGHT,
                SLIDER_SIZE,
                SLIDER_SIZE,
                targetY,
                CHALLENGE_TTL_SECONDS
        );
    }

    public RegistrationCaptchaVerifyVO verify(RegistrationCaptchaVerifyDTO dto, String clientIp) {
        rateLimitService.checkVerification(clientIp);
        if (dto == null || dto.getChallengeId() == null
                || !dto.getChallengeId().matches("[0-9a-f]{32}")) {
            throw new BusinessException(400, "滑块挑战无效");
        }
        String key = CHALLENGE_PREFIX + dto.getChallengeId();
        ChallengeState state = readState(key);
        validateOwner(state, clientIp);
        if (!isHumanLike(dto) || !isPositionCorrect(dto.getOffsetX(), state.targetX())) {
            recordFailure(key, state);
            throw new BusinessException(400, "滑块验证失败，请刷新后重试");
        }

        String consumed;
        try {
            consumed = redis.opsForValue().getAndDelete(key);
        } catch (RuntimeException e) {
            log.error("Unable to consume registration captcha challenge", e);
            throw new BusinessException(503, "安全校验服务暂不可用，请稍后再试");
        }
        if (consumed == null) {
            throw new BusinessException(400, "滑块挑战已过期或已使用");
        }
        ChallengeState consumedState = ChallengeState.parse(consumed);
        validateOwner(consumedState, clientIp);
        if (!isPositionCorrect(dto.getOffsetX(), consumedState.targetX())) {
            throw new BusinessException(400, "滑块验证失败，请刷新后重试");
        }

        String token = randomId();
        try {
            redis.opsForValue().set(
                    TOKEN_PREFIX + token,
                    fingerprint(clientIp),
                    Duration.ofSeconds(TOKEN_TTL_SECONDS)
            );
        } catch (RuntimeException e) {
            log.error("Unable to persist registration captcha token", e);
            throw new BusinessException(503, "安全校验服务暂不可用，请稍后再试");
        }
        return new RegistrationCaptchaVerifyVO(token, TOKEN_TTL_SECONDS);
    }

    public void consumeToken(String token, String clientIp) {
        if (token == null || !token.matches("[0-9a-f]{32}")) {
            throw new BusinessException(400, "注册验证令牌无效");
        }
        String storedFingerprint;
        try {
            storedFingerprint = redis.opsForValue().getAndDelete(TOKEN_PREFIX + token);
        } catch (RuntimeException e) {
            log.error("Unable to consume registration captcha token", e);
            throw new BusinessException(503, "安全校验服务暂不可用，请稍后再试");
        }
        if (storedFingerprint == null) {
            throw new BusinessException(400, "注册验证令牌已过期或已使用");
        }
        if (!secureEquals(storedFingerprint, fingerprint(clientIp))) {
            throw new BusinessException(400, "注册验证令牌与当前客户端不匹配");
        }
    }

    private ChallengeState readState(String key) {
        try {
            String stored = redis.opsForValue().get(key);
            if (stored == null) {
                throw new BusinessException(400, "滑块挑战已过期或已使用");
            }
            return ChallengeState.parse(stored);
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Unable to read registration captcha challenge", e);
            throw new BusinessException(503, "安全校验服务暂不可用，请稍后再试");
        }
    }

    private void validateOwner(ChallengeState state, String clientIp) {
        if (!secureEquals(state.clientFingerprint(), fingerprint(clientIp))) {
            throw new BusinessException(400, "滑块挑战与当前客户端不匹配");
        }
    }

    private boolean isPositionCorrect(Integer offsetX, int targetX) {
        return offsetX != null && Math.abs(offsetX - targetX) <= POSITION_TOLERANCE;
    }

    private boolean isHumanLike(RegistrationCaptchaVerifyDTO dto) {
        Long duration = dto.getDurationMs();
        List<Integer> trace = dto.getTrace();
        if (duration == null || duration < 400 || duration > 15_000
                || trace == null || trace.size() < 3 || trace.size() > 200) {
            return false;
        }
        int maxOffset = IMAGE_WIDTH - SLIDER_SIZE;
        int violations = 0;
        Set<Integer> distinct = new HashSet<>();
        Integer previous = null;
        for (Integer point : trace) {
            if (point == null || point < 0 || point > maxOffset) {
                return false;
            }
            distinct.add(point);
            if (previous != null && point + 3 < previous) {
                violations++;
            }
            previous = point;
        }
        int last = trace.get(trace.size() - 1);
        return distinct.size() >= 3
                && last >= 20
                && Math.abs(last - dto.getOffsetX()) <= 3
                && violations <= Math.max(2, trace.size() / 10);
    }

    private void recordFailure(String key, ChallengeState state) {
        int attempts = state.attempts() + 1;
        try {
            if (attempts >= MAX_CHALLENGE_ATTEMPTS) {
                redis.delete(key);
                return;
            }
            Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            if (ttl != null && ttl > 0) {
                ChallengeState updated = new ChallengeState(
                        state.targetX(),
                        state.clientFingerprint(),
                        state.createdAtEpochSecond(),
                        attempts
                );
                redis.opsForValue().set(key, updated.serialize(), Duration.ofSeconds(ttl));
            }
        } catch (RuntimeException e) {
            log.warn("Unable to record registration captcha failure ({})", e.getClass().getSimpleName());
        }
    }

    private CaptchaImages generateImages(int targetX, int targetY) {
        BufferedImage background = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = background.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color start = randomPastel();
            Color end = randomPastel();
            graphics.setPaint(new GradientPaint(0, 0, start, IMAGE_WIDTH, IMAGE_HEIGHT, end));
            graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
            for (int i = 0; i < 22; i++) {
                graphics.setColor(randomTranslucentColor(90));
                int diameter = 12 + secureRandom.nextInt(46);
                graphics.fillOval(
                        secureRandom.nextInt(IMAGE_WIDTH),
                        secureRandom.nextInt(IMAGE_HEIGHT),
                        diameter,
                        diameter
                );
            }
            graphics.setStroke(new BasicStroke(2f));
            for (int i = 0; i < 8; i++) {
                graphics.setColor(randomTranslucentColor(120));
                graphics.drawLine(
                        secureRandom.nextInt(IMAGE_WIDTH),
                        secureRandom.nextInt(IMAGE_HEIGHT),
                        secureRandom.nextInt(IMAGE_WIDTH),
                        secureRandom.nextInt(IMAGE_HEIGHT)
                );
            }
        } finally {
            graphics.dispose();
        }

        BufferedImage slider = new BufferedImage(SLIDER_SIZE, SLIDER_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sliderGraphics = slider.createGraphics();
        try {
            sliderGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            sliderGraphics.setClip(1, 1, SLIDER_SIZE - 2, SLIDER_SIZE - 2);
            sliderGraphics.drawImage(
                    background,
                    0,
                    0,
                    SLIDER_SIZE,
                    SLIDER_SIZE,
                    targetX,
                    targetY,
                    targetX + SLIDER_SIZE,
                    targetY + SLIDER_SIZE,
                    null
            );
            sliderGraphics.setClip(null);
            sliderGraphics.setColor(new Color(255, 255, 255, 210));
            sliderGraphics.setStroke(new BasicStroke(2f));
            sliderGraphics.drawRoundRect(1, 1, SLIDER_SIZE - 3, SLIDER_SIZE - 3, 8, 8);
        } finally {
            sliderGraphics.dispose();
        }

        Graphics2D slotGraphics = background.createGraphics();
        try {
            slotGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            slotGraphics.setColor(new Color(20, 20, 20, 115));
            slotGraphics.fillRoundRect(targetX, targetY, SLIDER_SIZE, SLIDER_SIZE, 8, 8);
            slotGraphics.setColor(new Color(255, 255, 255, 190));
            slotGraphics.setStroke(new BasicStroke(2f));
            slotGraphics.drawRoundRect(targetX, targetY, SLIDER_SIZE, SLIDER_SIZE, 8, 8);
        } finally {
            slotGraphics.dispose();
        }
        return new CaptchaImages(toDataUrl(background), toDataUrl(slider));
    }

    private Color randomPastel() {
        return new Color(
                120 + secureRandom.nextInt(100),
                120 + secureRandom.nextInt(100),
                120 + secureRandom.nextInt(100)
        );
    }

    private Color randomTranslucentColor(int alpha) {
        return new Color(
                secureRandom.nextInt(256),
                secureRandom.nextInt(256),
                secureRandom.nextInt(256),
                alpha
        );
    }

    private String toDataUrl(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate captcha image", e);
        }
    }

    private String fingerprint(String clientIp) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(clientIp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private boolean secureEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String randomId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private record CaptchaImages(String backgroundImage, String sliderImage) {
    }

    private record ChallengeState(int targetX,
                                  String clientFingerprint,
                                  long createdAtEpochSecond,
                                  int attempts) {

        String serialize() {
            return targetX + "|" + clientFingerprint + "|" + createdAtEpochSecond + "|" + attempts;
        }

        static ChallengeState parse(String value) {
            try {
                String[] fields = value.split("\\|", -1);
                if (fields.length != 4) {
                    throw new IllegalArgumentException("Unexpected field count");
                }
                return new ChallengeState(
                        Integer.parseInt(fields[0]),
                        fields[1],
                        Long.parseLong(fields[2]),
                        Integer.parseInt(fields[3])
                );
            } catch (RuntimeException e) {
                throw new BusinessException(400, "滑块挑战数据无效");
            }
        }
    }
}
