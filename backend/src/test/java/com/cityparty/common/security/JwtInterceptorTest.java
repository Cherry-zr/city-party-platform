package com.cityparty.common.security;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.mapper.UserMapper;
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
    @Mock
    private UserMapper userMapper;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void allowsAdminToAccessAdminApi() {
        JwtInterceptor interceptor = new JwtInterceptor(jwtUtils, userMapper);
        MockHttpServletRequest request = adminRequest();
        when(jwtUtils.parseToken("admin-token")).thenReturn(new LoginUser(1L, "admin", "ADMIN"));
        when(userMapper.selectById(1L)).thenReturn(user(1L, "admin", "ADMIN", "NORMAL"));

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
        assertThat(UserContext.getUserId()).isEqualTo(1L);
    }

    @Test
    void rejectsNormalUserFromAdminApi() {
        JwtInterceptor interceptor = new JwtInterceptor(jwtUtils, userMapper);
        MockHttpServletRequest request = adminRequest();
        when(jwtUtils.parseToken("admin-token")).thenReturn(new LoginUser(2L, "user01", "USER"));
        when(userMapper.selectById(2L)).thenReturn(user(2L, "user01", "USER", "NORMAL"));

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    @Test
    void rejectsAnonymousRequestToAdminApi() {
        JwtInterceptor interceptor = new JwtInterceptor(jwtUtils, userMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(401);
    }

    @Test
    void rejectsDisabledUserEvenWhenTokenIsValid() {
        JwtInterceptor interceptor = new JwtInterceptor(jwtUtils, userMapper);
        MockHttpServletRequest request = adminRequest();
        when(jwtUtils.parseToken("admin-token")).thenReturn(new LoginUser(1L, "admin", "ADMIN"));
        when(userMapper.selectById(1L)).thenReturn(user(1L, "admin", "ADMIN", "DISABLED"));

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(401);
    }

    @Test
    void rejectsOldAdminTokenAfterRoleIsRemoved() {
        JwtInterceptor interceptor = new JwtInterceptor(jwtUtils, userMapper);
        MockHttpServletRequest request = adminRequest();
        when(jwtUtils.parseToken("admin-token")).thenReturn(new LoginUser(1L, "admin", "ADMIN"));
        when(userMapper.selectById(1L)).thenReturn(user(1L, "admin", "USER", "NORMAL"));

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    private MockHttpServletRequest adminRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard");
        request.addHeader("Authorization", "Bearer admin-token");
        return request;
    }

    private User user(Long id, String username, String role, String status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setStatus(status);
        user.setDeleted(0);
        return user;
    }
}
