package com.cityparty.common.websocket;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.security.JwtUtils;
import com.cityparty.common.security.LoginUser;
import com.cityparty.module.user.entity.User;
import com.cityparty.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            LoginUser loginUser = refreshLoginUser(jwtUtils.parseToken(token));
            attributes.put(WebSocketUserAttributes.LOGIN_USER, loginUser);
            attributes.put(WebSocketUserAttributes.USER_ID, loginUser.getUserId());
            return true;
        } catch (BusinessException e) {
            response.setStatusCode(e.getCode() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    private LoginUser refreshLoginUser(LoginUser tokenUser) {
        User user = userMapper.selectById(tokenUser.getUserId());
        if (user == null || Integer.valueOf(1).equals(user.getDeleted()) || !"NORMAL".equals(user.getStatus())) {
            throw new BusinessException(401, "User is disabled or does not exist.");
        }
        return new LoginUser(user.getId(), user.getUsername(), user.getRole());
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // No cleanup needed. WebSocket session lifecycle is handled by the message handler.
    }
}
