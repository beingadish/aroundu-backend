package com.beingadish.AroundU.infrastructure.cache.impl;

import com.beingadish.AroundU.infrastructure.cache.CacheEvictionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * No-op cache eviction used during tests (no Redis available).
 */
@Service
@Profile("test")
@Slf4j
public class NoOpCacheEvictionService implements CacheEvictionService {

    @Override
    public void evictJobDetail(Long jobId) {
        log.debug("NoOp: skipping job detail cache eviction for jobId={}", jobId);
    }

    @Override
    public void evictClientJobsCaches(Long clientId) {
        log.debug("NoOp: skipping client jobs cache eviction for clientId={}", clientId);
    }

    @Override
    public void evictWorkerFeedCaches() {
        log.debug("NoOp: skipping worker feed cache eviction");
    }
}
