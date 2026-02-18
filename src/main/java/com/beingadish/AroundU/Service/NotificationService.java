package com.beingadish.AroundU.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for sending notifications (email, push, SMS) asynchronously.
 * <p>
 * All notification methods are fire-and-forget — failures are logged and
 * persisted for retry, but never propagate to the caller. This ensures that
 * user-facing operations succeed even when the notification infrastructure is
 * degraded.
 */
public interface NotificationService {

    /**
     * Sends all notifications for a job event (bid accepted, status changed,
     * etc.) in parallel. Individual channel failures are recorded for retry.
     *
     * @param jobId the job that triggered the notification
     * @param clientEmail the client's email address (may be null)
     * @param workerEmail the worker's email address (may be null)
     * @param clientId the client's user ID for push notifications
     * @param workerId the worker's user ID for push notifications
     * @param clientPhone the client's phone number for SMS (may be null)
     * @param workerPhone the worker's phone number for SMS (may be null)
     * @param subject the notification subject / title
     * @param body the notification body / message
     */
    void sendJobNotifications(Long jobId,
            String clientEmail, String workerEmail,
            Long clientId, Long workerId,
            String clientPhone, String workerPhone,
            String subject, String body);

    /**
     * Sends an email asynchronously.
     *
     * @return a CompletableFuture that completes when the email is sent
     */
    CompletableFuture<Boolean> sendEmailAsync(String to, String subject, String body);

    /**
     * Sends a push notification asynchronously.
     *
     * @return a CompletableFuture that completes when the push is delivered
     */
    CompletableFuture<Boolean> sendPushAsync(Long userId, String title, String message);

    /**
     * Sends an SMS asynchronously.
     *
     * @return a CompletableFuture that completes when the SMS is sent
     */
    CompletableFuture<Boolean> sendSmsAsync(String phoneNumber, String message);
}
