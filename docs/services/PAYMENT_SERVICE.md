# Payment Service

> Escrow lock/release with Resilience4j circuit breaker and automatic failure recovery.

---

## Overview

The Payment module manages the escrow lifecycle — locking funds when a job starts and releasing them on completion. A resilient decorator wraps every payment call with `CircuitBreaker(Retry(actualCall))`, ensuring payment failures don't crash user-facing operations.

**Package:** `com.beingadish.AroundU.payment`

---

## File Inventory

| File | Type | Description |
|------|------|-------------|
| `entity/PaymentTransaction.java` | Entity | `job`, `client`, `worker`, `amount`, `paymentMode`, `status`, `gatewayReference` |
| `repository/PaymentTransactionRepository.java` | Repository | `findByJob`, status queries |
| `service/PaymentService.java` | Interface | `lockEscrow`, `releaseEscrow` |
| `service/impl/PaymentServiceImpl.java` | Implementation | Core escrow logic with guards |
| `service/ResilientPaymentService.java` | Decorator (`@Primary`) | Resilience4j wrapping + failure handling |
| `controller/PaymentController.java` | Controller | 2 REST endpoints |
| `dto/PaymentLockRequest.java` | DTO | `amount` |
| `dto/PaymentReleaseRequest.java` | DTO | `releaseCode` |
| `dto/PaymentResponseDTO.java` | DTO | Transaction response |
| `mapper/PaymentTransactionMapper.java` | Mapper | Entity ↔ DTO |

---

## Service Methods

### `PaymentService`

| Method | Signature | Description |
|--------|-----------|-------------|
| `lockEscrow` | `PaymentTransaction lockEscrow(Long jobId, Long clientId, PaymentLockRequest request)` | Locks escrow funds for a job |
| `releaseEscrow` | `PaymentTransaction releaseEscrow(Long jobId, Long clientId, PaymentReleaseRequest request)` | Releases escrow; transitions job to `PAYMENT_RELEASED` |

---

## REST Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/v1/jobs/{jobId}/payments/lock` | Client | Lock escrow |
| `POST` | `/api/v1/jobs/{jobId}/payments/release` | Client | Release escrow |

---

## Escrow Flow

```
Client locks escrow (POST /lock)
     │
     ├─ Guard: job.createdBy == clientId
     ├─ Guard: job status is READY_TO_START or BID_SELECTED_AWAITING_HANDSHAKE
     ├─ Guard: no existing ESCROW_LOCKED transaction for this job
     │
     └─ Creates PaymentTransaction (status = PENDING_ESCROW → ESCROW_LOCKED)
          │
          │  ... job progresses to COMPLETED_PENDING_PAYMENT ...
          │
Client releases escrow (POST /release)
     │
     ├─ Guard: job status is COMPLETED_PENDING_PAYMENT, IN_PROGRESS, or COMPLETED
     ├─ Guard: existing ESCROW_LOCKED transaction exists
     │
     └─ Transaction status → ESCROW_RELEASED
        Job status → PAYMENT_RELEASED
```

---

## Resilience Architecture

The `ResilientPaymentService` is marked `@Primary` and decorates `PaymentServiceImpl`:

```
Any code injecting PaymentService
     │
     └─ ResilientPaymentService (@Primary)
          │
          └─ CircuitBreaker
               └─ Retry
                    └─ PaymentServiceImpl (actual logic)
```

### Failure Handling

When all retries are exhausted:

1. **Logs** a `CRITICAL` error
2. **Records** failure metric (`aroundu.payments.failures`)
3. **Queues** the request in `ConcurrentLinkedQueue<FailedPaymentRecord>` for manual processing
4. **Sends admin alert** via `EmailService.sendAdminAlert()`
5. **Returns** a `PENDING_ESCROW` transaction so the user isn't left hanging

### `FailedPaymentRecord`

```java
record FailedPaymentRecord(
    String operation,     // "lockEscrow" or "releaseEscrow"
    Long jobId,
    Long clientId,
    Double amount,
    long failedAtMillis,
    String errorMessage
)
```

---

## Edge Cases

| # | Edge Case | Guard |
|---|-----------|-------|
| 1 | Duplicate escrow lock | `findByJob()` check — rejects if `ESCROW_LOCKED` already exists |
| 2 | Non-owner tries lock | `job.getCreatedBy().getId() != clientId` |
| 3 | Lock in wrong job state | Only `READY_TO_START` or `BID_SELECTED_AWAITING_HANDSHAKE` |
| 4 | Release in wrong state | Only `COMPLETED_PENDING_PAYMENT`, `IN_PROGRESS`, or `COMPLETED` |
| 5 | Circuit breaker open | Returns `PENDING_ESCROW` + queues for manual review |

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `PaymentTransactionRepository` | Transaction persistence |
| `JobRepository` | Job status validation and transitions |
| `CircuitBreaker` (Resilience4j) | Fault tolerance |
| `Retry` (Resilience4j) | Automatic retries |
| `MetricsService` | Payment failure counter |
| `EmailService` | Admin alerts on payment failure |
