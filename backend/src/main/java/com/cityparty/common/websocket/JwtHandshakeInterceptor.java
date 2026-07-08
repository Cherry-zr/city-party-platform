package com.cityparty.common.websocket;

import com.cityparty.common.exception.BusinessException;
import com.cityparty.common.security.JwtUtils;
import com.cityparty.common.security.LoginUser;
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
            LoginUser loginUser = jwtUtils.parseToken(token);
            attributes.put(WebSocketUserAttributes.LOGIN_USER, loginUser);
            attributes.put(WebSocketUserAttributes.USER_ID, loginUser.getUserId());
            return true;
        } catch (BusinessException e) {
            response.setStatusCode(e.getCode() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // No cleanup needed. WebSocket session lifecycle is handled by the message handler.
    }
}
