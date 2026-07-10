package com.cityparty.common.websocket;

import com.cityparty.common.security.JwtUtils;
import com.cityparty.common.security.LoginUser;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtHandshakeInterceptorTest {

    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ServerHttpRequest request;
    @Mock
    private ServerHttpResponse response;
    @Mock
    private WebSocketHandler webSocketHandler;

    @InjectMocks
    private JwtHandshakeInterceptor interceptor;

    @Test
    void refreshesRoleFromDatabaseBeforeHandshake() {
        Map<String, Object> attributes = new HashMap<>();
        prepareRequest();
        when(jwtUtils.parseToken("test-token")).thenReturn(new LoginUser(1L, "demo-user", "USER"));
        when(userMapper.selectById(1L)).thenReturn(user("ADMIN", "NORMAL", 0));

        boolean allowed = interceptor.beforeHandshake(request, response, webSocketHandler, attributes);

        assertThat(allowed).isTrue();
        assertThat(attributes.get(WebSocketUserAttributes.USER_ID)).isEqualTo(1L);
        assertThat(attributes.get(WebSocketUserAttributes.LOGIN_USER))
                .isInstanceOfSatisfying(LoginUser.class, loginUser -> assertThat(loginUser.getRole()).isEqualTo("ADMIN"));
    }

    @Test
    void rejectsDisabledUserBeforeHandshake() {
        prepareRequest();
        when(jwtUtils.parseToken("test-token")).thenReturn(new LoginUser(1L, "demo-user", "USER"));
        when(userMapper.selectById(1L)).thenReturn(user("USER", "DISABLED", 0));

        boolean allowed = interceptor.beforeHandshake(request, response, webSocketHandler, new HashMap<>());

        assertThat(allowed).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsDeletedUserBeforeHandshake() {
        prepareRequest();
        when(jwtUtils.parseToken("test-token")).thenReturn(new LoginUser(1L, "demo-user", "USER"));
        when(userMapper.selectById(1L)).thenReturn(user("USER", "NORMAL", 1));

        boolean allowed = interceptor.beforeHandshake(request, response, webSocketHandler, new HashMap<>());

        assertThat(allowed).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    private void prepareRequest() {
        when(request.getURI()).thenReturn(URI.create("ws://localhost/ws?token=test-token"));
    }

    private User user(String role, String status, int deleted) {
        User user = new User();
        user.setId(1L);
        user.setUsername("demo-user");
        user.setRole(role);
        user.setStatus(status);
        user.setDeleted(deleted);
        return user;
    }
}
