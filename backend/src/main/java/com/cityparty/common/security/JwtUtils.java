package com.cityparty.common.security;

import com.cityparty.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${city-party.jwt.secret}")
    private String secret;

    @Value("${city-party.jwt.expire-minutes}")
    private Long expireMinutes;

    public String generateToken(LoginUser loginUser) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userId", loginUser.getUserId());
            payload.put("username", loginUser.getUsername());
            payload.put("role", loginUser.getRole());
            payload.put("exp", Instant.now().plusSeconds(expireMinutes * 60).getEpochSecond());
            String headerPart = base64Url(OBJECT_MAPPER.writeValueAsBytes(header));
            String payloadPart = base64Url(OBJECT_MAPPER.writeValueAsBytes(payload));
            String signature = sign(headerPart + "." + payloadPart);
            return headerPart + "." + payloadPart + "." + signature;
        } catch (Exception e) {
            throw new BusinessException(500, "Token 生成失败");
        }
    }

    public LoginUser parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException(401, "Token 格式错误");
            }
            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw new BusinessException(401, "Token 签名无效");
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> payload = OBJECT_MAPPER.readValue(payloadBytes, new TypeReference<>() {});
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) {
                throw new BusinessException(401, "Token 已过期");
            }
            Long userId = ((Number) payload.get("userId")).longValue();
            String username = String.valueOf(payload.get("username"));
            String role = String.valueOf(payload.get("role"));
            return new LoginUser(userId, username, role);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(401, "Token 校验失败");
        }
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return base64Url(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
