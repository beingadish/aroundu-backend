package com.beingadish.AroundU.user.service.impl;

import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import com.beingadish.AroundU.user.service.WorkerPenaltyService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implements worker cancellation penalty logic.
 * <p>
 * Configurable via:
 * <ul>
 * <li>{@code worker.penalty.cancellation-threshold} — number of cancellations
 * before block (default 3)</li>
 * <li>{@code worker.penalty.block-days} — duration of block in days (default
 * 7)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkerPenaltyServiceImpl implements WorkerPenaltyService {

    @Value("${worker.penalty.cancellation-threshold:3}")
    private int cancellationThreshold;

    @Value("${worker.penalty.block-days:7}")
    private int blockDays;

    private final WorkerRepository workerRepository;

    @Override
    public Worker recordCancellation(Long workerId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new EntityNotFoundException("Worker not found"));

        int newCount = worker.incrementCancellationCount();
        log.info("Worker {} cancellation count: {}/{}", workerId, newCount, cancellationThreshold);

        if (newCount >= cancellationThreshold) {
            worker.setBlockedUntil(LocalDateTime.now().plusDays(blockDays));
            worker.setIsOnDuty(false);
            log.warn("Worker {} blocked for {} days due to {} cancellations",
                    workerId, blockDays, newCount);
        }

        return workerRepository.save(worker);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(Long workerId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new EntityNotFoundException("Worker not found"));
        return worker.isBlocked();
    }

    @Override
    public void unblockExpiredWorkers() {
        List<Worker> blocked = workerRepository.findBlockedWorkersWithExpiredPenalty(LocalDateTime.now());
        for (Worker worker : blocked) {
            worker.setBlockedUntil(null);
            worker.setCancellationCount(0);
            workerRepository.save(worker);
            log.info("Unblocked worker {} after penalty period expired", worker.getId());
        }
        if (!blocked.isEmpty()) {
            log.info("Unblocked {} workers with expired penalties", blocked.size());
        }
    }
}
