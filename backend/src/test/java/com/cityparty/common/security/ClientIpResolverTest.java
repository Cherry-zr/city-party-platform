package com.cityparty.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientIpResolverTest {

    @Test
    void untrustedRemoteCannotSpoofForwardedFor() {
        ClientIpResolver resolver = resolver("10.0.0.10");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("198.51.100.5");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.8");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.5");
    }

    @Test
    void configuredTrustedProxyUsesFirstValidForwardedAddress() {
        ClientIpResolver resolver = resolver("10.0.0.10");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.10");
        when(request.getHeader("X-Forwarded-For"))
                .thenReturn("203.0.113.8, 10.0.0.20");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.8");
    }

    private ClientIpResolver resolver(String trustedProxies) {
        ClientIpResolver resolver = new ClientIpResolver();
        ReflectionTestUtils.setField(resolver, "trustedProxyConfig", trustedProxies);
        resolver.initialize();
        return resolver;
    }
}
