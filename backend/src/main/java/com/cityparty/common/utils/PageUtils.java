package com.cityparty.common.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public final class PageUtils {

    public static final long MAX_PAGE_SIZE = 100;
    public static final long DEFAULT_PAGE_SIZE = 10;

    private PageUtils() {
    }

    public static long safeCurrent(long current) {
        return Math.max(current, 1);
    }

    public static long safeSize(long size) {
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    public static <T> Page<T> page(long current, long size) {
        return new Page<>(safeCurrent(current), safeSize(size));
    }
}
