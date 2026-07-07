package com.cityparty.common.security;

import com.cityparty.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Pattern PUBLIC_ACTIVITY_DETAIL_PATH = Pattern.compile("^/api/activities/\\d+/?$");

    private final JwtUtils jwtUtils;

    public JwtInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if ((token == null || token.isBlank()) && isPublicRequest(request)) {
            return true;
        }
        if (token == null || token.isBlank()) {
            throw new BusinessException(401, "请先登录");
        }
        LoginUser loginUser = jwtUtils.parseToken(token);
        UserContext.set(loginUser);
        if (request.getRequestURI().startsWith("/api/admin") && !"ADMIN".equals(loginUser.getRole())) {
            throw new BusinessException(403, "无管理员权限");
        }
        return true;
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if ("/api/activities".equals(uri) || "/api/activities/".equals(uri)) {
            return true;
        }
        return PUBLIC_ACTIVITY_DETAIL_PATH.matcher(uri).matches();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
