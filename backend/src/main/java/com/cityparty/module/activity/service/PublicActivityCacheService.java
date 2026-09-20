package com.cityparty.module.activity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicActivityCacheService {

    private static final String RECOMMENDATION_CACHE_PATTERN =
            "city-party:recommendation:activities:*";

    private final StringRedisTemplate redis;

    public void evictRecommendationCachesAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictRecommendationCaches();
                }
            });
            return;
        }
        evictRecommendationCaches();
    }

    public void evictRecommendationCaches() {
        try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions()
                .match(RECOMMENDATION_CACHE_PATTERN)
                .count(100)
                .build())) {
            Set<String> keys = new HashSet<>();
            cursor.forEachRemaining(keys::add);
            if (!keys.isEmpty()) {
                redis.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Recommendation cache eviction failed ({})", e.getClass().getSimpleName());
        }
    }
}
