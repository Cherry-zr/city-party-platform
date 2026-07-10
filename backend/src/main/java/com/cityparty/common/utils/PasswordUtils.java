package com.cityparty.common.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@Component
public class PasswordUtils {

    private static final String PBKDF2_PREFIX = "pbkdf2";
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int PBKDF2_SALT_BYTES = 16;
    private static final int PBKDF2_KEY_BITS = 256;

    @Value("${city-party.password.salt}")
    private String salt;

    private final SecureRandom secureRandom = new SecureRandom();

    public String encode(String rawPassword) {
        byte[] saltBytes = new byte[PBKDF2_SALT_BYTES];
        secureRandom.nextBytes(saltBytes);
        byte[] hash = pbkdf2(rawPassword, saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS);
        return PBKDF2_PREFIX
                + "$" + PBKDF2_ITERATIONS
                + "$" + HexFormat.of().formatHex(saltBytes)
                + "$" + HexFormat.of().formatHex(hash);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        if (encodedPassword.startsWith(PBKDF2_PREFIX + "$")) {
            return matchesPbkdf2(rawPassword, encodedPassword);
        }
        return sha256(rawPassword + ":" + salt).equals(encodedPassword);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Password encode failed", e);
        }
    }

    private boolean matchesPbkdf2(String rawPassword, String encodedPassword) {
        try {
            String[] parts = encodedPassword.split("\\$");
            if (parts.length != 4 || !PBKDF2_PREFIX.equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] saltBytes = HexFormat.of().parseHex(parts[2]);
            byte[] expectedHash = HexFormat.of().parseHex(parts[3]);
            byte[] actualHash = pbkdf2(rawPassword, saltBytes, iterations, expectedHash.length * 8);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private byte[] pbkdf2(String rawPassword, byte[] saltBytes, int iterations, int keyBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), saltBytes, iterations, keyBits);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Password encode failed", e);
        }
    }
}
