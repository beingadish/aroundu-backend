# Notification Service

> Fire-and-forget multi-channel notifications — email, push, and SMS with retry persistence.

---

## Overview

The Notification module sends notifications across multiple channels (email, push, SMS) asynchronously. All methods are fire-and-forget — failures are logged and persisted in a `FailedNotification` table for retry, but never propagate to the caller. This ensures user-facing operations succeed even when notification infrastructure is degraded.

**Package:** `com.beingadish.AroundU.notification`

---

## File Inventory

| File | Type | Description |
|------|------|-------------|
| `service/NotificationService.java` | Interface | Multi-channel notification contract |
| `service/impl/NotificationServiceImpl.java` | Implementation | Parallel delivery with error isolation |
| `service/EmailService.java` | Interface | Email-specific operations |
| `service/impl/EmailServiceImpl.java` | Implementation | SMTP/template email sending |
| `entity/FailedNotification.java` | Entity | Failed notification record for retry |
| `repository/FailedNotificationRepository.java` | Repository | Failed notification queries |

---

## Service Methods

### `NotificationService`

| Method | Signature | Description |
|--------|-----------|-------------|
| `sendJobNotifications` | `void sendJobNotifications(jobId, clientEmail, workerEmail, clientId, workerId, clientPhone, workerPhone, subject, body)` | Sends all channels in parallel; individual failures recorded for retry |
| `sendEmailAsync` | `CompletableFuture<Boolean> sendEmailAsync(to, subject, body)` | Async email delivery |
| `sendPushAsync` | `CompletableFuture<Boolean> sendPushAsync(userId, title, message)` | Async push notification |
| `sendSmsAsync` | `CompletableFuture<Boolean> sendSmsAsync(phoneNumber, message)` | Async SMS delivery |

### `EmailService`

| Method | Description |
|--------|-------------|
| `sendEmail(to, subject, body)` | Synchronous email send |
| `sendAdminAlert(subject, body)` | Send alert to admin email(s) |

---

## Architecture

```
Job Event (bid accepted, status changed, etc.)
     │
     └─ NotificationService.sendJobNotifications()
          │
          ├─ sendEmailAsync(clientEmail, ...)  → CompletableFuture
          ├─ sendEmailAsync(workerEmail, ...)  → CompletableFuture
          ├─ sendPushAsync(clientId, ...)      → CompletableFuture
          ├─ sendPushAsync(workerId, ...)      → CompletableFuture
          ├─ sendSmsAsync(clientPhone, ...)    → CompletableFuture
          └─ sendSmsAsync(workerPhone, ...)    → CompletableFuture
               │
               ├─ Success → logged
               └─ Failure → persisted to FailedNotification table
                            → retried by scheduler
```

---

## Failed Notification Retry

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Auto-generated |
| `channel` | String | `EMAIL`, `PUSH`, `SMS` |
| `recipient` | String | Email/userId/phone |
| `subject` | String | Notification subject |
| `body` | String | Notification body |
| `retryCount` | int | Attempts so far |
| `lastError` | String | Last failure message |
| `resolved` | boolean | Whether successfully sent |
| `createdAt` | Timestamp | When the failure occurred |

---

## Design Principles

1. **Never block caller** — all notifications are async and failures don't propagate
2. **Null-safe** — null email/phone/userId fields are skipped (no NPE)
3. **Parallel delivery** — all channels fire simultaneously via `CompletableFuture`
4. **Persistence** — failed notifications are saved for scheduled retry
5. **No REST endpoints** — this is an internal service triggered by domain events

---

## Notification Triggers

| Event | Triggered By | Recipients |
|-------|-------------|------------|
| Bid accepted | `BidServiceImpl.acceptBid()` | Client + Worker |
| Handshake confirmed | `BidServiceImpl.handshake()` | Client + Worker |
| Job started | `JobServiceImpl.updateJobStatusByWorker()` | Client |
| Job completed | `JobServiceImpl.updateJobStatusByWorker()` | Client |
| Payment released | `PaymentServiceImpl.releaseEscrow()` | Worker |
| Payment failure | `ResilientPaymentService.handlePaymentFailure()` | Admin |

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `FailedNotificationRepository` | Failed notification persistence |
| Spring `@Async` | Thread pool for async execution |
| SMTP (Spring Mail) | Email delivery |
