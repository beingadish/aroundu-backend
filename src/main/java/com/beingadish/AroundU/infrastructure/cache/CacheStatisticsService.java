package com.beingadish.AroundU.infrastructure.cache;

import com.beingadish.AroundU.infrastructure.config.RedisConfig;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Periodically logs cache hit/miss ratios per cache region and alerts when the
 * hit rate falls below the configured threshold.
 * <p>
 * Relies on {@code RedisCacheManager.enableStatistics()} which exposes
 * {@code cache.gets}, {@code cache.puts}, and {@code cache.evictions} through
 * Micrometer, also available via {@code /actuator/metrics/cache.gets}.
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class CacheStatisticsService {

    private static final double HIT_RATE_THRESHOLD = 70.0;

    private static final String[] CACHE_NAMES = {
        RedisConfig.CACHE_JOB_DETAIL,
        RedisConfig.CACHE_CLIENT_JOBS,
        RedisConfig.CACHE_WORKER_FEED,
        RedisConfig.CACHE_USER_PROFILE,
        RedisConfig.CACHE_WORKER_SKILLS
    };

    private final MeterRegistry meterRegistry;

    /**
     * Logs cache statistics every 5 minutes and alerts if the hit rate drops
     * below {@value #HIT_RATE_THRESHOLD}%.
     */
    @Scheduled(fixedRate = 300_000)
    public void logCacheStatistics() {
        for (String cacheName : CACHE_NAMES) {
            double hits = getMeterValue(cacheName, "hit");
            double misses = getMeterValue(cacheName, "miss");
            double total = hits + misses;

            if (total == 0) {
                continue; // No traffic to this cache yet
            }

            double hitRate = (hits / total) * 100.0;
            String formattedRate = String.format("%.1f", hitRate);

            log.info("Cache [{}]: hits={}, misses={}, hitRate={}%",
                    cacheName, (long) hits, (long) misses, formattedRate);

            if (hitRate < HIT_RATE_THRESHOLD) {
                log.warn("ALERT: Cache [{}] hit rate {}% is below {}% threshold",
                        cacheName, formattedRate, HIT_RATE_THRESHOLD);
            }
        }
    }

    private double getMeterValue(String cacheName, String result) {
        FunctionCounter counter = meterRegistry.find("cache.gets")
                .tag("cache", cacheName)
                .tag("result", result)
                .functionCounter();
        return counter != null ? counter.count() : 0.0;
    }
}
