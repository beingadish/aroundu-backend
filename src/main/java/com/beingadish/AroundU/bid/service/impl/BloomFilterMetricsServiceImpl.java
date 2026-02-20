package com.beingadish.AroundU.bid.service.impl;

import com.beingadish.AroundU.bid.service.BloomFilterMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Production implementation of {@link BloomFilterMetricsService}.
 * <p>
 * Registers Micrometer gauges for each Bloom filter's estimated size and
 * counters for false-positive events. A scheduled task runs every 5 minutes to
 * check whether any filter exceeds 80% capacity.
 */
@Service
@Profile("!test")
@Slf4j
public class BloomFilterMetricsServiceImpl implements BloomFilterMetricsService {

    private static final double CAPACITY_THRESHOLD = 0.80;
    private static final long BID_CAPACITY = 1_000_000L;
    private static final long PROFILE_VIEW_CAPACITY = 1_000_000L;
    private static final long EMAIL_CAPACITY = 1_000_000L;

    private final RBloomFilter<String> bidBloomFilter;
    private final RBloomFilter<String> profileViewBloomFilter;
    private final RBloomFilter<String> emailRegistrationBloomFilter;

    private final Map<String, Counter> falsePositiveCounters = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public BloomFilterMetricsServiceImpl(
            RBloomFilter<String> bidBloomFilter,
            RBloomFilter<String> profileViewBloomFilter,
            RBloomFilter<String> emailRegistrationBloomFilter,
            MeterRegistry meterRegistry) {

        this.bidBloomFilter = bidBloomFilter;
        this.profileViewBloomFilter = profileViewBloomFilter;
        this.emailRegistrationBloomFilter = emailRegistrationBloomFilter;
        this.meterRegistry = meterRegistry;

        // ── Gauges for estimated element counts ──────────────────────────
        meterRegistry.gauge("aroundu.bloom.bid.size",
                bidBloomFilter, bf -> bf.count());
        meterRegistry.gauge("aroundu.bloom.profileview.size",
                profileViewBloomFilter, bf -> bf.count());
        meterRegistry.gauge("aroundu.bloom.email.size",
                emailRegistrationBloomFilter, bf -> bf.count());

        // ── Capacity utilisation gauges (0.0 – 1.0) ─────────────────────
        meterRegistry.gauge("aroundu.bloom.bid.utilisation",
                bidBloomFilter, bf -> (double) bf.count() / BID_CAPACITY);
        meterRegistry.gauge("aroundu.bloom.profileview.utilisation",
                profileViewBloomFilter, bf -> (double) bf.count() / PROFILE_VIEW_CAPACITY);
        meterRegistry.gauge("aroundu.bloom.email.utilisation",
                emailRegistrationBloomFilter, bf -> (double) bf.count() / EMAIL_CAPACITY);

        log.info("Bloom filter metrics registered with Micrometer");
    }

    @Override
    public void recordFalsePositive(String filterName) {
        Counter counter = falsePositiveCounters.computeIfAbsent(filterName,
                name -> Counter.builder("aroundu.bloom.false_positives")
                        .tag("filter", name)
                        .description("Bloom filter false positives")
                        .register(meterRegistry));
        counter.increment();
        log.debug("Bloom filter false positive recorded: filter={}", filterName);
    }

    @Override
    @Scheduled(fixedRate = 300_000) // every 5 minutes
    public void checkCapacityThresholds() {
        checkSingle("bid", bidBloomFilter, BID_CAPACITY);
        checkSingle("profileView", profileViewBloomFilter, PROFILE_VIEW_CAPACITY);
        checkSingle("email", emailRegistrationBloomFilter, EMAIL_CAPACITY);
    }

    private void checkSingle(String name, RBloomFilter<String> filter, long capacity) {
        long count = filter.count();
        double utilisation = (double) count / capacity;
        if (utilisation > CAPACITY_THRESHOLD) {
            log.warn("Bloom filter '{}' exceeds {}% capacity: count={}, capacity={}",
                    name, (int) (CAPACITY_THRESHOLD * 100), count, capacity);
        }
    }
}
