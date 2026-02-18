package com.beingadish.AroundU.infrastructure.cache.impl;

import com.beingadish.AroundU.infrastructure.config.RedisConfig;
import com.beingadish.AroundU.infrastructure.cache.CacheEvictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Redis-backed cache eviction using SCAN for pattern-based key removal.
 * <p>
 * Unlike {@code @CacheEvict(allEntries = true)} which clears an entire cache
 * region, this service uses granular key-based deletions:
 * <ul>
 * <li><strong>Job detail</strong> – single key delete by job ID</li>
 * <li><strong>Client jobs list</strong> – SCAN for keys matching the client's
 * prefix</li>
 * <li><strong>Worker feed</strong> – SCAN + batch delete (short TTL minimises
 * entries)</li>
 * </ul>
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class RedisCacheEvictionService implements CacheEvictionService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void evictJobDetail(Long jobId) {
        if (jobId == null) {
            return;
        }
        String key = RedisConfig.CACHE_JOB_DETAIL + "::" + jobId;
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Evicted job detail cache key={}, deleted={}", key, deleted);
        } catch (Exception ex) {
            log.warn("Failed to evict job detail cache for jobId={}: {}", jobId, ex.getMessage());
        }
    }

    @Override
    public void evictClientJobsCaches(Long clientId) {
        if (clientId == null) {
            return;
        }
        String pattern = RedisConfig.CACHE_CLIENT_JOBS + "::" + clientId + ":*";
        scanAndDelete(pattern, "client jobs for clientId=" + clientId);
    }

    @Override
    public void evictWorkerFeedCaches() {
        String pattern = RedisConfig.CACHE_WORKER_FEED + "::*";
        scanAndDelete(pattern, "worker feed");
    }

    // ── Internals ────────────────────────────────────────────────────────
    /**
     * Cursor-based SCAN + batch DELETE — non-blocking, unlike {@code KEYS}.
     */
    private void scanAndDelete(String pattern, String description) {
        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
            Set<String> keysToDelete = new HashSet<>();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                cursor.forEachRemaining(keysToDelete::add);
            }
            if (!keysToDelete.isEmpty()) {
                Long count = redisTemplate.delete(keysToDelete);
                log.debug("Evicted {} keys for {}", count, description);
            } else {
                log.debug("No cache keys matched pattern '{}' ({})", pattern, description);
            }
        } catch (Exception ex) {
            log.warn("Cache eviction scan failed for {}: {}", description, ex.getMessage());
        }
    }
}
