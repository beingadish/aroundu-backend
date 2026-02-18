package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.Service.EmailService;
import com.beingadish.AroundU.Service.MetricsService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Email service with Resilience4j circuit breaker + retry + async queue
 * fallback.
 * <p>
 * Execution order: {@code CircuitBreaker(Retry(actualSend))}.
 * <ul>
 * <li>If the email provider is healthy → send immediately.</li>
 * <li>If retries are exhausted → queue for async retry.</li>
 * <li>If the circuit is open → fast-fail and queue.</li>
 * <li>User-facing operations still succeed (email is "best effort").</li>
 * </ul>
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final MetricsService metricsService;

    @Value("${admin.email:admin@aroundu.com}")
    private String adminEmail;

    /**
     * In-memory queue for failed emails; in production this would be a
     * persistent queue (e.g. Redis list, SQS, RabbitMQ).
     */
    private final Queue<QueuedEmail> emailRetryQueue = new ConcurrentLinkedQueue<>();

    public EmailServiceImpl(@Qualifier("emailServiceCircuitBreaker") CircuitBreaker circuitBreaker,
            @Qualifier("emailServiceRetry") Retry retry,
            MetricsService metricsService) {
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.metricsService = metricsService;
    }

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
