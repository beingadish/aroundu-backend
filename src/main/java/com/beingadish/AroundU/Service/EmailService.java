package com.beingadish.AroundU.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Abstraction for sending transactional emails.
 * <p>
 * Production implementation wraps calls with a Resilience4j circuit breaker and
 * retry; if sending fails, the email is queued for asynchronous retry.
 * <p>
 * Async variants allow fire-and-forget or parallel email dispatch without
 * blocking the caller thread.
 */
public interface EmailService {

    /**
     * Send an email immediately (blocking). On failure the implementation
     * should queue the email for retry and return {@code false}.
     *
     * @return {@code true} if sent successfully, {@code false} if queued
     */
    boolean sendEmail(String to, String subject, String body);

    /**
     * Send an admin alert (circuit-breaker-open, payment-failure, etc.).
     */
    boolean sendAdminAlert(String subject, String body);

    /**
     * Send an email asynchronously on the notification thread pool. The
     * returned future completes with {@code true} on success, or completes
     * exceptionally on permanent failure.
     */
    CompletableFuture<Boolean> sendEmailAsync(String to, String subject, String body);

    /**
     * Sends multiple emails in parallel and waits for all to complete.
     * Individual failures are logged but do not prevent other emails from being
     * sent.
     *
     * @param recipients list of email addresses
     * @param subject shared subject line
     * @param body shared body text
     * @return future that completes when all emails have been dispatched
     */
    CompletableFuture<Void> sendBulkEmailAsync(List<String> recipients, String subject, String body);
}
