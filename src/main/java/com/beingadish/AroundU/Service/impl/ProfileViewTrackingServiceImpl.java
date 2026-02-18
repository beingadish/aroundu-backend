package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.Service.BloomFilterMetricsService;
import com.beingadish.AroundU.Service.ProfileViewTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tracks profile views using a Bloom filter to dedup repeated views by the same
 * viewer within the same hour.
 * <p>
 * The Bloom filter key includes the current hour bucket, e.g.:
 * {@code view:42:profile:99:hour:2026021814}
 * <p>
 * A Redis counter ({@code profile:views:{profileId}}) holds the total count.
 * The counter is only incremented for genuinely new views.
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class ProfileViewTrackingServiceImpl implements ProfileViewTrackingService {

    private static final String VIEW_COUNT_KEY_PREFIX = "profile:views:";
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final RBloomFilter<String> profileViewBloomFilter;
    private final StringRedisTemplate stringRedisTemplate;
    private final BloomFilterMetricsService bloomFilterMetricsService;

    @Override
    public boolean trackView(Long viewerId, Long profileId) {
        String hourBucket = LocalDateTime.now().format(HOUR_FORMATTER);
        String key = buildKey(viewerId, profileId, hourBucket);

        if (profileViewBloomFilter.contains(key)) {
            // Duplicate view in this hour – skip incrementing
            log.debug("Repeated profile view detected: viewer={}, profile={}, hour={}",
                    viewerId, profileId, hourBucket);
            return false;
        }

        // New view in this hour – record in Bloom filter and increment counter
        profileViewBloomFilter.add(key);
        stringRedisTemplate.opsForValue().increment(VIEW_COUNT_KEY_PREFIX + profileId);

        log.debug("New profile view recorded: viewer={}, profile={}, hour={}",
                viewerId, profileId, hourBucket);
        return true;
    }

    @Override
    public long getViewCount(Long profileId) {
        String countStr = stringRedisTemplate.opsForValue().get(VIEW_COUNT_KEY_PREFIX + profileId);
        return countStr != null ? Long.parseLong(countStr) : 0L;
    }

    private String buildKey(Long viewerId, Long profileId, String hourBucket) {
        return "view:" + viewerId + ":profile:" + profileId + ":hour:" + hourBucket;
    }
}
