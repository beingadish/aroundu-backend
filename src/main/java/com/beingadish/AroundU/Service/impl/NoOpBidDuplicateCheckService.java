package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.Service.BidDuplicateCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * No-op implementation of {@link BidDuplicateCheckService} used under the
 * {@code test} profile where Redis/Redisson is not available.
 */
@Service
@Profile("test")
@Slf4j
public class NoOpBidDuplicateCheckService implements BidDuplicateCheckService {

    @Override
    public void validateNoDuplicateBid(Long workerId, Long jobId) {
        log.debug("NoOp: skipping Bloom filter bid duplicate check for worker={}, job={}", workerId, jobId);
    }

    @Override
    public void recordBid(Long workerId, Long jobId) {
        log.debug("NoOp: skipping Bloom filter bid recording for worker={}, job={}", workerId, jobId);
    }
}
