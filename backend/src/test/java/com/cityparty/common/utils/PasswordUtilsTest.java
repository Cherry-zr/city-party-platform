package com.cityparty.common.utils;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordUtilsTest {

    @Test
    void encodesNewPasswordsWithPbkdf2AndVerifiesThem() {
        PasswordUtils passwordUtils = new PasswordUtils();

        String encoded = passwordUtils.encode("secret123");

        assertThat(encoded).startsWith("pbkdf2$");
        assertThat(passwordUtils.matches("secret123", encoded)).isTrue();
        assertThat(passwordUtils.matches("wrong", encoded)).isFalse();
    }

    @Test
    void keepsLegacySha256HashesCompatible() throws Exception {
        PasswordUtils passwordUtils = new PasswordUtils();
        ReflectionTestUtils.setField(passwordUtils, "salt", "cityparty");
        String legacyHash = sha256("secret123:cityparty");

        assertThat(passwordUtils.matches("secret123", legacyHash)).isTrue();
        assertThat(passwordUtils.matches("wrong", legacyHash)).isFalse();
    }

    @Test
    void identifiesOnlyLegacySha256HashesForUpgrade() {
        PasswordUtils passwordUtils = new PasswordUtils();
        String pbkdf2Hash = passwordUtils.encode("secret123");

        assertThat(passwordUtils.needsUpgrade("a".repeat(64))).isTrue();
        assertThat(passwordUtils.needsUpgrade(pbkdf2Hash)).isFalse();
        assertThat(passwordUtils.needsUpgrade("pbkdf2$invalid")).isFalse();
        assertThat(passwordUtils.needsUpgrade("not-a-password-hash")).isFalse();
        assertThat(passwordUtils.needsUpgrade("")).isFalse();
        assertThat(passwordUtils.needsUpgrade(null)).isFalse();
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        for (byte b : hash) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
