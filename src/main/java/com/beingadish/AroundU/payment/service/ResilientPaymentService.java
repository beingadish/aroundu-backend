package com.beingadish.AroundU.payment.service;

import com.beingadish.AroundU.payment.dto.PaymentLockRequest;
import com.beingadish.AroundU.payment.dto.PaymentReleaseRequest;
import com.beingadish.AroundU.payment.entity.PaymentTransaction;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import com.beingadish.AroundU.infrastructure.metrics.MetricsService;
import com.beingadish.AroundU.notification.service.EmailService;

/**
 * Resilient decorator around the core {@link PaymentService}.
 * <p>
 * Wraps every payment gateway call with
 * {@code CircuitBreaker(Retry(actualCall))}. If all attempts fail:
 * <ol>
 * <li>Logs a critical error</li>
 * <li>Queues the request for manual processing</li>
 * <li>Sends an admin alert</li>
 * <li>Returns a {@code PENDING_ESCROW} transaction so the user isn't left
 * hanging</li>
 * </ol>
 *
 * Marked {@code @Primary} so any component injecting {@link PaymentService}
 * gets the resilient version while the raw implementation is still accessible
 * via {@code @Qualifier("paymentServiceImpl")}.
 */
@Service("resilientPaymentService")
@Primary
@Slf4j
public class ResilientPaymentService implements PaymentService {

    private final PaymentService delegate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final MetricsService metricsService;
    private final EmailService emailService;

    /**
     * Queue of failed payment requests awaiting manual review.
     */
    private final Queue<FailedPaymentRecord> manualProcessingQueue = new ConcurrentLinkedQueue<>();

    public ResilientPaymentService(@Qualifier("paymentServiceImpl") PaymentService delegate,
            @Qualifier("paymentGatewayCircuitBreaker") CircuitBreaker circuitBreaker,
            @Qualifier("paymentGatewayRetry") Retry retry,
            MetricsService metricsService,
            EmailService emailService) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.metricsService = metricsService;
        this.emailService = emailService;
    }

    @Override
    public PaymentTransaction lockEscrow(Long jobId, Long clientId, PaymentLockRequest request) {
        Supplier<PaymentTransaction> supplier = () -> delegate.lockEscrow(jobId, clientId, request);

        Supplier<PaymentTransaction> decorated = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                Retry.decorateSupplier(retry, supplier)
        );

        try {
            return decorated.get();
        } catch (Exception e) {
            return handlePaymentFailure("lockEscrow", jobId, clientId, request.getAmount(), e);
        }
    }

    @Override
    public PaymentTransaction releaseEscrow(Long jobId, Long clientId, PaymentReleaseRequest request) {
        Supplier<PaymentTransaction> supplier = () -> delegate.releaseEscrow(jobId, clientId, request);

        Supplier<PaymentTransaction> decorated = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                Retry.decorateSupplier(retry, supplier)
        );

        try {
            return decorated.get();
        } catch (Exception e) {
            return handlePaymentFailure("releaseEscrow", jobId, clientId, null, e);
        }
    }

    // ── Failure handling ─────────────────────────────────────────────────
    private PaymentTransaction handlePaymentFailure(String operation,
            Long jobId,
            Long clientId,
            Double amount,
            Exception e) {
        log.error("CRITICAL: Payment {} failed for job={} client={} after all retries: {}",
                operation, jobId, clientId, e.getMessage(), e);

        // Record failure metric
        metricsService.getPaymentFailureCounter().increment();

        // Queue for manual processing
        var record = new FailedPaymentRecord(operation, jobId, clientId, amount,
                System.currentTimeMillis(), e.getMessage());
        manualProcessingQueue.offer(record);
        log.warn("Queued payment for manual processing (queue size={}): {}",
                manualProcessingQueue.size(), record);

        // Alert admin
        emailService.sendAdminAlert(
                "Payment " + operation + " failed",
                String.format("Job=%d Client=%d Amount=%s Error=%s — queued for manual review.",
                        jobId, clientId, amount, e.getMessage())
        );

        // Return a "pending" transaction so the user isn't left hanging
        return PaymentTransaction.builder()
                .status(com.beingadish.AroundU.common.constants.enums.PaymentStatus.PENDING_ESCROW)
                .amount(amount)
                .gatewayReference("QUEUED_MANUAL_" + System.currentTimeMillis())
                .build();
    }

    /**
     * Returns the manual-processing queue (visible for monitoring / tests).
     */
    public Queue<FailedPaymentRecord> getManualProcessingQueue() {
        return manualProcessingQueue;
    }

    /**
     * Record of a payment that failed and is pending manual resolution.
     */
    public record FailedPaymentRecord(String operation,
            Long jobId,
            Long clientId,
            Double amount,
            long failedAtMillis,
            String errorMessage) {

    }
}
