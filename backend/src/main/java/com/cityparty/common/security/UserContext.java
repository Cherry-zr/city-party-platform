package com.cityparty.common.security;

import com.cityparty.common.exception.BusinessException;

public final class UserContext {

    private static final ThreadLocal<LoginUser> CURRENT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        CURRENT.set(loginUser);
    }

    public static LoginUser get() {
        LoginUser loginUser = CURRENT.get();
        if (loginUser == null) {
            throw new BusinessException(401, "请先登录");
        }
        return loginUser;
    }

    public static LoginUser getOrNull() {
        return CURRENT.get();
    }

    public static Long getUserId() {
        return get().getUserId();
    }

    public static Long getUserIdOrNull() {
        LoginUser loginUser = getOrNull();
        return loginUser == null ? null : loginUser.getUserId();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(get().getRole());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
