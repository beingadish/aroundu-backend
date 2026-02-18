package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.Config.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Monitors all Resilience4j circuit breakers and retries, publishing custom
 * Micrometer metrics and sending admin alerts when breakers stay open.
 *
 * <h3>Exposed metrics</h3>
 * <ul>
 * <li>{@code aroundu.resilience.cb.state} — gauge per breaker (0=closed,
 * 1=open, 2=half-open)</li>
 * <li>{@code aroundu.resilience.cb.open.alerts} — counter of admin alerts
 * sent</li>
 * <li>{@code aroundu.resilience.retry.exhausted} — counter per service</li>
 * </ul>
 *
 * <h3>Scheduled checks</h3>
 * Every 60 seconds, inspects each breaker. If any breaker has been OPEN for
 * &gt; 5 minutes, sends a single admin alert (rate-limited to avoid spam).
 */
@Service
@Profile("!test")
@Slf4j
public class ResilienceMonitoringService {

    private final CircuitBreakerRegistry cbRegistry;
    private final RetryRegistry retryRegistry;
    private final EmailService emailService;

    /**
     * State gauge backing values: 0=CLOSED, 1=OPEN, 2=HALF_OPEN, 3=DISABLED,
     * 4=FORCED_OPEN
     */
    private final Map<String, AtomicInteger> cbStateGauges = new ConcurrentHashMap<>();

    /**
     * Tracks the epoch-millis when a breaker last transitioned to OPEN.
     */
    private final Map<String, Long> openSince = new ConcurrentHashMap<>();

    /**
     * De-duplication: don't spam alerts — at most once per open period.
     */
    private final Map<String, Boolean> alertSent = new ConcurrentHashMap<>();

    @Getter
    private final Counter cbOpenAlertCounter;

    @Getter
    private final Counter retryExhaustedCounter;

    /**
     * Timeout (ms) after which an open breaker triggers an admin alert.
     */
    private static final long OPEN_ALERT_THRESHOLD_MS = 5 * 60 * 1000L; // 5 minutes

    public ResilienceMonitoringService(CircuitBreakerRegistry cbRegistry,
            RetryRegistry retryRegistry,
            EmailService emailService,
            MeterRegistry meterRegistry) {
        this.cbRegistry = cbRegistry;
        this.retryRegistry = retryRegistry;
        this.emailService = emailService;

        // ── Custom counters ──
        this.cbOpenAlertCounter = Counter.builder("aroundu.resilience.cb.open.alerts")
                .description("Number of admin alerts sent for open circuit breakers")
                .register(meterRegistry);

        this.retryExhaustedCounter = Counter.builder("aroundu.resilience.retry.exhausted")
                .description("Number of times all retry attempts were exhausted")
                .register(meterRegistry);

        // ── State gauges per breaker ──
        cbRegistry.getAllCircuitBreakers().forEach(cb -> {
            AtomicInteger gauge = new AtomicInteger(stateToInt(cb.getState()));
            cbStateGauges.put(cb.getName(), gauge);

            Gauge.builder("aroundu.resilience.cb.state", gauge, AtomicInteger::doubleValue)
                    .tag("name", cb.getName())
                    .description("Circuit breaker state: 0=CLOSED, 1=OPEN, 2=HALF_OPEN")
                    .register(meterRegistry);

            // Listen for transitions to keep gauge updated and track open time
            cb.getEventPublisher().onStateTransition(event -> {
                var to = event.getStateTransition().getToState();
                gauge.set(stateToInt(to));

                if (to == CircuitBreaker.State.OPEN) {
                    openSince.put(cb.getName(), System.currentTimeMillis());
                    alertSent.put(cb.getName(), false);
                } else {
                    openSince.remove(cb.getName());
                    alertSent.remove(cb.getName());
                }
            });
        });

        // ── Retry event listeners ──
        retryRegistry.getAllRetries().forEach(r
                -> r.getEventPublisher().onError(event -> {
                    log.warn("Retry [{}] exhausted all {} attempts: {}",
                            r.getName(), event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().getMessage());
                    retryExhaustedCounter.increment();
                })
        );
    }

    // ── Scheduled health check ───────────────────────────────────────────
    /**
     * Runs every 60 seconds. For each breaker that is OPEN for more than 5
     * minutes, sends a single admin alert.
     */
    @Scheduled(fixedDelayString = "${resilience.monitoring.check-interval-ms:60000}")
    public void checkCircuitBreakers() {
        cbRegistry.getAllCircuitBreakers().forEach(cb -> {
            CircuitBreaker.State state = cb.getState();
            String name = cb.getName();

            // Update gauge (in case of race)
            AtomicInteger gauge = cbStateGauges.get(name);
            if (gauge != null) {
                gauge.set(stateToInt(state));
            }

            if (state == CircuitBreaker.State.OPEN) {
                Long since = openSince.get(name);
                boolean alreadyAlerted = Boolean.TRUE.equals(alertSent.get(name));

                if (since != null && !alreadyAlerted) {
                    long openDuration = System.currentTimeMillis() - since;
                    if (openDuration > OPEN_ALERT_THRESHOLD_MS) {
                        log.error("ALERT: CircuitBreaker [{}] has been OPEN for {} seconds!",
                                name, openDuration / 1000);
                        emailService.sendAdminAlert(
                                "CircuitBreaker OPEN > 5 min: " + name,
                                String.format("Circuit breaker '%s' has been OPEN for %d seconds. "
                                        + "External dependency may be down. Investigate immediately.",
                                        name, openDuration / 1000));
                        cbOpenAlertCounter.increment();
                        alertSent.put(name, true);
                    }
                }
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private static int stateToInt(CircuitBreaker.State state) {
        return switch (state) {
            case CLOSED ->
                0;
            case OPEN ->
                1;
            case HALF_OPEN ->
                2;
            case DISABLED ->
                3;
            case FORCED_OPEN ->
                4;
            case METRICS_ONLY ->
                5;
        };
    }
}
