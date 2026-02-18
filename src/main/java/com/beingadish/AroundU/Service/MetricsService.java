package com.beingadish.AroundU.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Centralized metrics service that wraps Micrometer counters, timers, and
 * gauges for AroundU business operations. Inject this service wherever you need
 * to record metrics.
 */
@Service
@Getter
public class MetricsService {

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

    // ── Gauge backing fields ─────────────────────────────────────────────
    private final AtomicInteger activeJobsGauge = new AtomicInteger(0);

    public MetricsService(MeterRegistry registry) {

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

        // Gauges
        registry.gauge("aroundu.jobs.active", activeJobsGauge);
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
