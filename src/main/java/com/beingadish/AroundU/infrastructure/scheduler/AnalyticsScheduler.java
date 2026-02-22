package com.beingadish.AroundU.infrastructure.scheduler;

import com.beingadish.AroundU.bid.repository.BidRepository;
import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.common.constants.enums.PaymentStatus;
import com.beingadish.AroundU.infrastructure.analytics.entity.AggregatedMetrics;
import com.beingadish.AroundU.infrastructure.analytics.repository.AggregatedMetricsRepository;
import com.beingadish.AroundU.infrastructure.config.SchedulerProperties;
import com.beingadish.AroundU.infrastructure.lock.LockServiceBase;
import com.beingadish.AroundU.infrastructure.metrics.SchedulerMetricsService;
import com.beingadish.AroundU.job.repository.JobRepository;
import com.beingadish.AroundU.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.Optional;

/**
 * Aggregates the previous day's business metrics and stores them in the
 * {@code aggregated_metrics} table. Calculates week-over-week and
 * month-over-month growth trends.
 * <p>
 * Default schedule: daily at 03:00 AM.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsScheduler {

    private static final String TASK_NAME = "analytics-aggregation";
    private static final Duration LOCK_TTL = Duration.ofHours(1).plusMinutes(1);

    private final LockServiceBase lockService;
    private final JobRepository jobRepository;
    private final BidRepository bidRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final AggregatedMetricsRepository metricsRepository;
    private final SchedulerProperties schedulerProperties;
    private final SchedulerMetricsService schedulerMetrics;
    private final Clock clock;

    @Scheduled(cron = "${scheduler.analytics-cron:0 0 3 * * ?}")
    public void aggregateDailyMetrics() {
        if (!schedulerProperties.isEnabled()) {
            return;
        }
        if (!lockService.tryAcquireLock(TASK_NAME, LOCK_TTL)) {
            log.debug("Another instance is running {}", TASK_NAME);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            LocalDate yesterday = LocalDate.now(clock).minusDays(1);

            // Skip if already aggregated
            if (metricsRepository.existsByMetricDate(yesterday)) {
                log.info("Metrics for {} already computed, skipping", yesterday);
                schedulerMetrics.recordSuccess(TASK_NAME,
                        System.currentTimeMillis() - start);
                return;
            }

            LocalDateTime dayStart = yesterday.atStartOfDay();
            LocalDateTime dayEnd = yesterday.atTime(LocalTime.MAX);

            // ── Raw counts ───────────────────────────────────────────────
            long jobsCreated = jobRepository.countByCreatedAtBetween(dayStart, dayEnd);
            long bidsPlaced = bidRepository.countByCreatedAtBetween(dayStart, dayEnd);
            long jobsCompleted = jobRepository.countByJobStatusAndCreatedAtBetween(
                    JobStatus.COMPLETED, dayStart, dayEnd);
            double revenue = paymentRepository.sumAmountByStatusAndCreatedAtBetween(
                    PaymentStatus.RELEASED, dayStart, dayEnd);
            double avgBid = jobsCreated > 0 ? (double) bidsPlaced / jobsCreated : 0.0;

            // ── Trends ───────────────────────────────────────────────────
            double wow = computeGrowth(yesterday.minusWeeks(1), jobsCreated);
            double mom = computeGrowth(yesterday.minusMonths(1), jobsCreated);

            AggregatedMetrics metrics = AggregatedMetrics.builder()
                    .metricDate(yesterday)
                    .jobsCreated(jobsCreated)
                    .bidsPlaced(bidsPlaced)
                    .jobsCompleted(jobsCompleted)
                    .revenueTotal(revenue)
                    .averageBidPerJob(avgBid)
                    .weekOverWeekGrowth(wow)
                    .monthOverMonthGrowth(mom)
                    .build();

            metricsRepository.save(metrics);

            long durationMs = System.currentTimeMillis() - start;
            log.info("Aggregated metrics for {}: jobs={} bids={} completed={} revenue={} ({}ms)",
                    yesterday, jobsCreated, bidsPlaced, jobsCompleted, revenue, durationMs);
            schedulerMetrics.recordSuccess(TASK_NAME, durationMs);
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - start;
            log.error("Analytics aggregation failed after {}ms", durationMs, ex);
            schedulerMetrics.recordFailure(TASK_NAME, durationMs);
        } finally {
            lockService.releaseLock(TASK_NAME);
        }
    }

    /**
     * Compute growth percentage compared to a reference date. Returns 0.0 if
     * the reference data does not exist or was zero.
     */
    private double computeGrowth(LocalDate referenceDate, long currentValue) {
        Optional<AggregatedMetrics> refOpt = metricsRepository.findByMetricDate(referenceDate);
        if (refOpt.isEmpty() || refOpt.get().getJobsCreated() == 0) {
            return 0.0;
        }
        long refValue = refOpt.get().getJobsCreated();
        return ((double) (currentValue - refValue) / refValue) * 100.0;
    }
}
