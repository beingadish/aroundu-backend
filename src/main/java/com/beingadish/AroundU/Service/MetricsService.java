package com.beingadish.AroundU.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Centralized metrics service that wraps Micrometer counters, timers, and
 * gauges for AroundU business operations. Inject this service wherever you need
 * to record metrics.
 * <p>
 * Also provides thread-pool instrumentation so executor metrics are exposed via
 * Actuator at:
 * <ul>
 * <li>{@code /actuator/metrics/executor.active}</li>
 * <li>{@code /actuator/metrics/executor.completed}</li>
 * <li>{@code /actuator/metrics/executor.queued}</li>
 * <li>{@code /actuator/metrics/executor.pool.size}</li>
 * </ul>
 */
@Service
@Getter
@Slf4j
public class MetricsService {

    private final MeterRegistry registry;

    // ── Job metrics ──────────────────────────────────────────────────────
    private final Counter jobsCreatedCounter;
    private final Counter jobsCompletedCounter;
    private final Counter jobsCancelledCounter;
    private final Timer jobCreationTimer;

    // ── Bid metrics ──────────────────────────────────────────────────────
    private final Counter bidsPlacedCounter;
    private final Counter bidsAcceptedCounter;
    private final Counter bidsRejectedCounter;
    private final Timer bidPlacementTimer;

    // ── Payment metrics ──────────────────────────────────────────────────
    private final Counter escrowLockedCounter;
    private final Counter escrowReleasedCounter;
    private final Counter paymentFailureCounter;
    private final Timer escrowLockTimer;
    private final Timer escrowReleaseTimer;

    // ── Auth metrics ─────────────────────────────────────────────────────
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter registrationCounter;

    // ── Async / notification metrics ─────────────────────────────────────
    private final Counter notificationsSentCounter;
    private final Counter notificationsFailedCounter;
    private final Timer notificationDispatchTimer;

    // ── Gauge backing fields ─────────────────────────────────────────────
    private final AtomicInteger activeJobsGauge = new AtomicInteger(0);

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;

        // Jobs
        this.jobsCreatedCounter = Counter.builder("aroundu.jobs.created")
                .description("Total jobs created")
                .register(registry);
        this.jobsCompletedCounter = Counter.builder("aroundu.jobs.completed")
                .description("Total jobs completed")
                .register(registry);
        this.jobsCancelledCounter = Counter.builder("aroundu.jobs.cancelled")
                .description("Total jobs cancelled")
                .register(registry);
        this.jobCreationTimer = Timer.builder("aroundu.jobs.creation.duration")
                .description("Time to create a job")
                .register(registry);

        // Bids
        this.bidsPlacedCounter = Counter.builder("aroundu.bids.placed")
                .description("Total bids placed")
                .register(registry);
        this.bidsAcceptedCounter = Counter.builder("aroundu.bids.accepted")
                .description("Total bids accepted")
                .register(registry);
        this.bidsRejectedCounter = Counter.builder("aroundu.bids.rejected")
                .description("Total bids rejected")
                .register(registry);
        this.bidPlacementTimer = Timer.builder("aroundu.bids.placement.duration")
                .description("Time to place a bid")
                .register(registry);

        // Payments
        this.escrowLockedCounter = Counter.builder("aroundu.payments.escrow.locked")
                .description("Total escrow locks")
                .register(registry);
        this.escrowReleasedCounter = Counter.builder("aroundu.payments.escrow.released")
                .description("Total escrow releases")
                .register(registry);
        this.paymentFailureCounter = Counter.builder("aroundu.payments.failures")
                .description("Total payment failures")
                .register(registry);
        this.escrowLockTimer = Timer.builder("aroundu.payments.escrow.lock.duration")
                .description("Time to lock escrow")
                .register(registry);
        this.escrowReleaseTimer = Timer.builder("aroundu.payments.escrow.release.duration")
                .description("Time to release escrow")
                .register(registry);

        // Auth
        this.loginSuccessCounter = Counter.builder("aroundu.auth.login.success")
                .description("Successful logins")
                .register(registry);
        this.loginFailureCounter = Counter.builder("aroundu.auth.login.failure")
                .description("Failed login attempts")
                .register(registry);
        this.registrationCounter = Counter.builder("aroundu.auth.registrations")
                .description("Total user registrations")
                .register(registry);

        // Notifications
        this.notificationsSentCounter = Counter.builder("aroundu.notifications.sent")
                .description("Total notifications sent successfully")
                .register(registry);
        this.notificationsFailedCounter = Counter.builder("aroundu.notifications.failed")
                .description("Total notification failures")
                .register(registry);
        this.notificationDispatchTimer = Timer.builder("aroundu.notifications.dispatch.duration")
                .description("Time to dispatch a batch of notifications")
                .register(registry);

        // Gauges
        registry.gauge("aroundu.jobs.active", activeJobsGauge);
    }

    // ── Thread pool instrumentation ──────────────────────────────────────
    /**
     * Instruments a {@link ThreadPoolTaskExecutor} or {@link ExecutorService}
     * with Micrometer metrics. Call this from config classes after executor
     * initialization.
     * <p>
     * Exposes: {@code executor.active}, {@code executor.completed},
     * {@code executor.queued}, {@code executor.pool.size},
     * {@code executor.pool.core}, {@code executor.pool.max}.
     *
     * @param executor the executor to instrument
     * @param name logical name (e.g. "notification", "database")
     */
    public void instrumentExecutor(Executor executor, String name) {
        ExecutorService es = unwrapExecutorService(executor);
        if (es != null) {
            new ExecutorServiceMetrics(es, name, List.of()).bindTo(registry);
            log.info("Instrumented executor '{}' for Micrometer metrics", name);
        } else {
            log.debug("Cannot instrument executor '{}' — not an ExecutorService (may be virtual threads)", name);
        }
    }

    private ExecutorService unwrapExecutorService(Executor executor) {
        if (executor instanceof ExecutorService es) {
            return es;
        }
        if (executor instanceof ThreadPoolTaskExecutor tpte) {
            return tpte.getThreadPoolExecutor();
        }
        return null;
    }

    // ── Convenience helpers ──────────────────────────────────────────────
    /**
     * Record the result of a timed operation, returning the result.
     */
    public <T> T recordTimer(Timer timer, Supplier<T> supplier) {
        return timer.record(supplier);
    }

    /**
     * Adjust active-jobs gauge up/down.
     */
    public void incrementActiveJobs() {
        activeJobsGauge.incrementAndGet();
    }

    public void decrementActiveJobs() {
        activeJobsGauge.decrementAndGet();
    }
}
