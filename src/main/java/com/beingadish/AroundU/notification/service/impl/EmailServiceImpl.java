package com.beingadish.AroundU.notification.service.impl;

import com.beingadish.AroundU.infrastructure.metrics.MetricsService;
import com.beingadish.AroundU.notification.service.EmailService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Email service with Resilience4j circuit breaker + retry + async queue
 * fallback, now enhanced with parallel async email dispatch.
 * <p>
 * Execution order: {@code CircuitBreaker(Retry(actualSend))}.
 * <ul>
 * <li>If the email provider is healthy → send immediately.</li>
 * <li>If retries are exhausted → queue for async retry.</li>
 * <li>If the circuit is open → fast-fail and queue.</li>
 * <li>User-facing operations still succeed (email is "best effort").</li>
 * </ul>
 *
 * <b>Async methods</b> use the {@code notificationExecutor} thread pool so
 * email dispatch never blocks HTTP request threads.
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final MetricsService metricsService;
    private final Executor notificationExecutor;
    /**
     * In-memory queue for failed emails; in production this would be a
     * persistent queue (e.g. Redis list, SQS, RabbitMQ).
     */
    private final Queue<QueuedEmail> emailRetryQueue = new ConcurrentLinkedQueue<>();
    @Value("${admin.email:admin@aroundu.com}")
    private String adminEmail;

    public EmailServiceImpl(@Qualifier("emailServiceCircuitBreaker") CircuitBreaker circuitBreaker,
                            @Qualifier("emailServiceRetry") Retry retry,
                            MetricsService metricsService,
                            @Qualifier("notificationExecutor") Executor notificationExecutor) {
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.metricsService = metricsService;
        this.notificationExecutor = notificationExecutor;
    }

    // ── Synchronous (blocking) ───────────────────────────────────────────
    @Override
    public boolean sendEmail(String to, String subject, String body) {
        Supplier<Boolean> decorated = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                Retry.decorateSupplier(retry, () -> doSendEmail(to, subject, body))
        );

        try {
            return decorated.get();
        } catch (Exception e) {
            log.warn("Email to {} failed after retries, queuing for async retry: {}",
                    to, e.getMessage());
            queueForRetry(to, subject, body);
            return false;
        }
    }

    @Override
    public boolean sendAdminAlert(String subject, String body) {
        return sendEmail(adminEmail, "[AroundU Alert] " + subject, body);
    }

    // ── Asynchronous (non-blocking) ──────────────────────────────────────

    /**
     * Sends a single email asynchronously on the notification thread pool.
     * <p>
     * Performance: frees the calling thread immediately; the actual send (with
     * circuit breaker + retry) runs on a background thread.
     */
    @Override
    public CompletableFuture<Boolean> sendEmailAsync(String to, String subject, String body) {
        return CompletableFuture.supplyAsync(() -> sendEmail(to, subject, body), notificationExecutor)
                .exceptionally(ex -> {
                    log.warn("Async email to {} failed: {}", to, ex.getMessage());
                    queueForRetry(to, subject, body);
                    return false;
                });
    }

    /**
     * Sends emails to multiple recipients in parallel on the notification
     * thread pool, waiting for all to complete.
     * <p>
     * Performance: N emails sent concurrently ≈ latency of the slowest one
     * instead of sum of all latencies.
     */
    @Override
    public CompletableFuture<Void> sendBulkEmailAsync(List<String> recipients, String subject, String body) {
        if (recipients == null || recipients.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        log.info("Sending bulk email to {} recipients, subject='{}'", recipients.size(), subject);
        CompletableFuture<?>[] futures = recipients.stream()
                .map(to -> sendEmailAsync(to, subject, body))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    // ── Internals ────────────────────────────────────────────────────────

    /**
     * Actual email send. In production this would call an SMTP library or REST
     * API (SendGrid, SES, etc.).
     */
    private boolean doSendEmail(String to, String subject, String body) {
        // TODO: replace with real email provider integration
        log.info("Sending email to={} subject='{}'", to, subject);
        // Simulated — always succeeds for now
        return true;
    }

    private void queueForRetry(String to, String subject, String body) {
        emailRetryQueue.offer(new QueuedEmail(to, subject, body, System.currentTimeMillis()));
        log.info("Queued email for retry (queue size={}): to={} subject='{}'",
                emailRetryQueue.size(), to, subject);
    }

    /**
     * Returns the pending-retry queue (visible for monitoring / tests).
     */
    public Queue<QueuedEmail> getEmailRetryQueue() {
        return emailRetryQueue;
    }

    /**
     * Simple value holder for a queued email.
     */
    public record QueuedEmail(String to, String subject, String body, long queuedAtMillis) {

    }
}
