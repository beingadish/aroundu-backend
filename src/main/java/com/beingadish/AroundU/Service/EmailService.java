package com.beingadish.AroundU.Service;

/**
 * Abstraction for sending transactional emails.
 * <p>
 * Production implementation wraps calls with a Resilience4j circuit breaker and
 * retry; if sending fails, the email is queued for asynchronous retry.
 */
public interface EmailService {

    /**
     * Send an email immediately. On failure the implementation should queue the
     * email for retry and return {@code false}.
     *
     * @return {@code true} if sent successfully, {@code false} if queued
     */
    boolean sendEmail(String to, String subject, String body);

    /**
     * Send an admin alert (circuit-breaker-open, payment-failure, etc.).
     */
    boolean sendAdminAlert(String subject, String body);
}
