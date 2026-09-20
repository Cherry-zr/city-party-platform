package com.cityparty.common.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class ClientIpResolver {

    @Value("${city-party.security.trusted-proxies:}")
    private String trustedProxyConfig;

    private Set<String> trustedProxies = Set.of();

    @PostConstruct
    void initialize() {
        if (!StringUtils.hasText(trustedProxyConfig)) {
            trustedProxies = Set.of();
            return;
        }
        Set<String> parsed = new HashSet<>();
        Arrays.stream(trustedProxyConfig.split(","))
                .map(String::trim)
                .map(this::normalize)
                .filter(this::isIpLiteral)
                .forEach(parsed::add);
        trustedProxies = Set.copyOf(parsed);
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = normalize(request.getRemoteAddr());
        if (!trustedProxies.contains(remoteAddress)) {
            return remoteAddress;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(forwardedFor)) {
            return remoteAddress;
        }
        return Arrays.stream(forwardedFor.split(","))
                .map(String::trim)
                .map(this::normalize)
                .filter(this::isIpLiteral)
                .findFirst()
                .orElse(remoteAddress);
    }

    private String normalize(String value) {
        if (value == null) {
            return "unknown";
        }
        String normalized = value.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isIpLiteral(String value) {
        if (!StringUtils.hasText(value) || value.length() > 45) {
            return false;
        }
        if (value.contains(":")) {
            return value.matches("[0-9a-fA-F:.]+");
        }
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (!part.matches("\\d{1,3}")) {
                return false;
            }
            int number = Integer.parseInt(part);
            if (number > 255) {
                return false;
            }
        }
        return true;
    }
}
