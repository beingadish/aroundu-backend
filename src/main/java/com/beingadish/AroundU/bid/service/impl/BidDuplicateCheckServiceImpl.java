package com.beingadish.AroundU.bid.service.impl;

import com.beingadish.AroundU.bid.exception.DuplicateBidException;
import com.beingadish.AroundU.bid.repository.BidRepository;
import com.beingadish.AroundU.bid.service.BidDuplicateCheckService;
import com.beingadish.AroundU.bid.service.BloomFilterMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Bloom-filter-backed duplicate bid detection.
 * <p>
 * Flow:
 * <ol>
 * <li>Check Bloom filter (O(1)) – if "definitely not present", allow bid</li>
 * <li>If "possibly present", query the database for confirmation</li>
 * <li>If DB confirms duplicate → throw {@link DuplicateBidException}</li>
 * <li>If DB says no duplicate (false positive) → allow bid, log the false
 * positive</li>
 * <li>After successful bid creation, add to Bloom filter via
 * {@link #recordBid}</li>
 * </ol>
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class BidDuplicateCheckServiceImpl implements BidDuplicateCheckService {

    private final RBloomFilter<String> bidBloomFilter;
    private final BidRepository bidRepository;
    private final BloomFilterMetricsService bloomFilterMetricsService;

    @Override
    public void validateNoDuplicateBid(Long workerId, Long jobId) {
        String key = buildKey(workerId, jobId);

        if (bidBloomFilter.contains(key)) {
            log.debug("Bloom filter reports possible duplicate bid: worker={}, job={}", workerId, jobId);

            boolean actuallyExists = bidRepository.existsByWorkerIdAndJobId(workerId, jobId);
            if (actuallyExists) {
                log.info("Duplicate bid confirmed by database: worker={}, job={}", workerId, jobId);
                throw new DuplicateBidException("Worker " + workerId + " has already bid on job " + jobId);
            }

            // False positive – Bloom filter was wrong
            log.info("Bloom filter false positive for bid check: worker={}, job={}", workerId, jobId);
            bloomFilterMetricsService.recordFalsePositive("bid");
        }
    }

    @Override
    public void recordBid(Long workerId, Long jobId) {
        String key = buildKey(workerId, jobId);
        bidBloomFilter.add(key);
        log.debug("Recorded bid in Bloom filter: worker={}, job={}", workerId, jobId);
    }

    private String buildKey(Long workerId, Long jobId) {
        return "worker:" + workerId + ":job:" + jobId;
    }
}
