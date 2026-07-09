package com.cityparty.common.security;

import com.cityparty.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtInterceptorTest {

    @Mock
    private JwtUtils jwtUtils;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void allowsAdminToAccessAdminApi() {
        JwtInterceptor interceptor = new JwtInterceptor(jwtUtils);
        MockHttpServletRequest request = adminRequest();
        when(jwtUtils.parseToken("admin-token")).thenReturn(new LoginUser(1L, "admin", "ADMIN"));

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
        assertThat(UserContext.getUserId()).isEqualTo(1L);
    }

    @Test
    void rejectsNormalUserFromAdminApi() {
        JwtInterceptor interceptor = new JwtInterceptor(jwtUtils);
        MockHttpServletRequest request = adminRequest();
        when(jwtUtils.parseToken("admin-token")).thenReturn(new LoginUser(2L, "user01", "USER"));

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    @Test
    void rejectsAnonymousRequestToAdminApi() {
        JwtInterceptor interceptor = new JwtInterceptor(jwtUtils);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(401);
    }

    private MockHttpServletRequest adminRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard");
        request.addHeader("Authorization", "Bearer admin-token");
        return request;
    }
}
