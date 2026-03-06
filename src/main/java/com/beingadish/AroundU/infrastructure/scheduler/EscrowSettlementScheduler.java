package com.beingadish.AroundU.infrastructure.scheduler;

import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.common.constants.enums.PaymentStatus;
import com.beingadish.AroundU.infrastructure.config.SchedulerProperties;
import com.beingadish.AroundU.infrastructure.lock.LockServiceBase;
import com.beingadish.AroundU.infrastructure.metrics.SchedulerMetricsService;
import com.beingadish.AroundU.payment.entity.PaymentTransaction;
import com.beingadish.AroundU.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * End-of-day escrow settlement.
 *
 * <p>
 * Scans for all {@code ESCROW_LOCKED} payment transactions whose associated job
 * is {@code COMPLETED} (release code verified by the worker) and marks them
 * {@code RELEASED}. This represents the system disbursing accumulated escrow
 * funds to workers in a single daily batch.
 *
 * <p>
 * Default schedule: 18:00 every day, configurable via
 * {@code scheduler.escrow-settlement-cron}.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class EscrowSettlementScheduler {

    private static final String TASK_NAME = "escrow-settlement";
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    private final LockServiceBase lockService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SchedulerProperties schedulerProperties;
    private final SchedulerMetricsService schedulerMetrics;

    @Scheduled(cron = "${scheduler.escrow-settlement-cron:0 0 18 * * ?}")
    @Transactional
    public void settleEscrowPayments() {
        if (!schedulerProperties.isEnabled()) {
            return;
        }
        if (!lockService.tryAcquireLock(TASK_NAME, LOCK_TTL)) {
            log.debug("Another instance is running {}", TASK_NAME);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            List<PaymentTransaction> pending = paymentTransactionRepository
                    .findByStatusAndJobJobStatus(PaymentStatus.ESCROW_LOCKED, JobStatus.COMPLETED);

            for (PaymentTransaction tx : pending) {
                tx.setStatus(PaymentStatus.RELEASED);
                paymentTransactionRepository.save(tx);
                log.info("Settled escrow for jobId={} workerId={} amount={}",
                        tx.getJob().getId(), tx.getWorker().getId(), tx.getAmount());
            }

            long durationMs = System.currentTimeMillis() - start;
            log.info("EOD escrow settlement: released {} payments ({}ms)", pending.size(), durationMs);
            schedulerMetrics.recordSuccess(TASK_NAME, durationMs);
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - start;
            log.error("EOD escrow settlement failed after {}ms", durationMs, ex);
            schedulerMetrics.recordFailure(TASK_NAME, durationMs);
        } finally {
            lockService.releaseLock(TASK_NAME);
        }
    }
}
