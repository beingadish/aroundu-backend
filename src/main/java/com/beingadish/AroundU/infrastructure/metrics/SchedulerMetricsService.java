package com.beingadish.AroundU.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks execution metrics for scheduled tasks: last run time, duration,
 * success/failure counts. Exposes Micrometer counters and timers so Prometheus
 * / Grafana can alert if a task hasn't executed in its expected window.
 */
@Service
@Getter
public class SchedulerMetricsService {

    private final MeterRegistry registry;

    /**
     * Last successful execution instant per task name.
     */
    private final Map<String, AtomicReference<Instant>> lastExecutionTimes = new ConcurrentHashMap<>();

    /**
     * Last execution duration per task name (milliseconds).
     */
    private final Map<String, AtomicReference<Long>> lastDurations = new ConcurrentHashMap<>();

    public SchedulerMetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    // ── Recording helpers ────────────────────────────────────────────────
    /**
     * Record a successful task execution.
     */
    public void recordSuccess(String taskName, long durationMs) {
        successCounter(taskName).increment();
        timer(taskName).record(Duration.ofMillis(durationMs));
        lastExecutionTimes
                .computeIfAbsent(taskName, k -> new AtomicReference<>())
                .set(Instant.now());
        lastDurations
                .computeIfAbsent(taskName, k -> new AtomicReference<>())
                .set(durationMs);
    }

    /**
     * Record a failed task execution.
     */
    public void recordFailure(String taskName, long durationMs) {
        failureCounter(taskName).increment();
        timer(taskName).record(Duration.ofMillis(durationMs));
    }

    // ── Query helpers ────────────────────────────────────────────────────
    public Instant getLastExecutionTime(String taskName) {
        AtomicReference<Instant> ref = lastExecutionTimes.get(taskName);
        return ref != null ? ref.get() : null;
    }

    public Long getLastDurationMs(String taskName) {
        AtomicReference<Long> ref = lastDurations.get(taskName);
        return ref != null ? ref.get() : null;
    }

    // ── Internal meter factories ─────────────────────────────────────────
    private Counter successCounter(String taskName) {
        return Counter.builder("aroundu.scheduler.success")
                .tag("task", taskName)
                .description("Successful executions of " + taskName)
                .register(registry);
    }

    private Counter failureCounter(String taskName) {
        return Counter.builder("aroundu.scheduler.failure")
                .tag("task", taskName)
                .description("Failed executions of " + taskName)
                .register(registry);
    }

    private Timer timer(String taskName) {
        return Timer.builder("aroundu.scheduler.duration")
                .tag("task", taskName)
                .description("Execution duration of " + taskName)
                .register(registry);
    }
}
