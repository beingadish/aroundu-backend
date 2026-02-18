package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.Service.BloomFilterMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * No-op implementation of {@link BloomFilterMetricsService} used under the
 * {@code test} profile where Redis/Redisson is not available.
 */
@Service
@Profile("test")
@Slf4j
public class NoOpBloomFilterMetricsService implements BloomFilterMetricsService {

    @Override
    public void recordFalsePositive(String filterName) {
        log.debug("NoOp: skipping Bloom filter false positive recording for {}", filterName);
    }

    @Override
    public void checkCapacityThresholds() {
        log.debug("NoOp: skipping Bloom filter capacity threshold check");
    }
}
