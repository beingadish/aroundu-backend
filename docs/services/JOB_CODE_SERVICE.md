# Job Code Service (OTP System)

> OTP-based job confirmation — start codes, release codes, expiry, and brute-force protection.

---

## Overview

The Job Code module implements a two-phase OTP verification system for job execution. A **start code** proves the worker arrived at the job site; a **release code** proves the job is complete. Both are 6-digit codes generated with `SecureRandom`.

**Package:** `com.beingadish.AroundU.job`

---

## File Inventory

| File | Type | Description |
|------|------|-------------|
| `entity/JobConfirmationCode.java` | Entity | Stores both OTP codes, statuses, attempt counts, and expiry timestamps |
| `repository/JobConfirmationCodeRepository.java` | Repository | `findByJobId`, code lookups |
| `service/JobCodeService.java` | Interface | OTP lifecycle contract |
| `service/impl/JobCodeServiceImpl.java` | Implementation | OTP generation, verification, expiry, locking |
| `controller/JobCodeController.java` | Controller | 5 REST endpoints |
| `dto/JobCodeResponseDTO.java` | DTO | Code response with contextual masking |
| `dto/JobCodeVerifyRequest.java` | DTO | `code` (6-digit string) |
| `mapper/JobConfirmationCodeMapper.java` | Mapper | Role-aware code masking |

---

## Service Methods

### `JobCodeService`

| Method | Signature | Description |
|--------|-----------|-------------|
| `generateCodes` | `JobCodeResponseDTO generateCodes(Long jobId, Long clientId)` | Creates both OTPs; returns start code only |
| `regenerateCodes` | `JobCodeResponseDTO regenerateCodes(Long jobId, Long clientId)` | Regenerates expired/locked codes (rate-limited: 1/min) |
| `verifyStartCode` | `JobCodeResponseDTO verifyStartCode(Long jobId, Long workerId, String code)` | Worker verifies start code → job moves to `IN_PROGRESS` |
| `verifyReleaseCode` | `JobCodeResponseDTO verifyReleaseCode(Long jobId, Long workerId, String code)` | Client verifies release code → job moves to `COMPLETED_PENDING_PAYMENT` |
| `fetchCodes` | `JobCodeResponseDTO fetchCodes(Long jobId, Long clientId)` | Returns contextually-relevant code based on current status |

---

## REST Endpoints

| Method | Path | Who | Description |
|--------|------|-----|-------------|
| `POST` | `/api/v1/jobs/{jobId}/codes` | Client | Generate OTP codes |
| `GET` | `/api/v1/jobs/{jobId}/codes` | Client | Fetch current code (context-aware) |
| `POST` | `/api/v1/jobs/{jobId}/codes/start` | Worker | Verify start code |
| `POST` | `/api/v1/jobs/{jobId}/codes/release` | Client | Verify release code |
| `POST` | `/api/v1/jobs/{jobId}/otp/regenerate` | Client | Regenerate codes (rate-limited: 1/min) |

---

## OTP Lifecycle

```
Client generates codes (POST /codes)
     │
     ├─ startCode = SecureRandom 6-digit (100000–999999)
     ├─ releaseCode = SecureRandom 6-digit (100000–999999)
     ├─ JobCodeStatus = START_PENDING
     └─ Returns: startCode only (releaseCode hidden)
          │
          │  Client shares start code verbally with worker
          │
     Worker verifies start code (POST /codes/start)
          │
          ├─ Match → JobCodeStatus = START_CONFIRMED
          │          Job status → IN_PROGRESS
          │          JobCodeStatus → RELEASE_PENDING
          │
          └─ Mismatch → attempts++
                        if attempts >= 5 → LOCKED (must regenerate)
          │
          │  Client retrieves release code (GET /codes)
          │  Client shares release code with worker as proof of completion
          │
     Client verifies release code (POST /codes/release)
          │
          ├─ Match → JobCodeStatus = COMPLETED
          │          Job status → COMPLETED_PENDING_PAYMENT
          │
          └─ Mismatch → attempts++ (same locking rules)
```

---

## Security Rules

| Rule | Detail |
|------|--------|
| **Generation** | `SecureRandom`, 6-digit (100000–999999) |
| **Expiry** | 30 minutes (configurable via `otp.expiry-minutes`) |
| **Max attempts** | 5 wrong attempts → locked |
| **Locked recovery** | Client must regenerate via `/otp/regenerate` |
| **Regeneration rate** | 1 per minute |
| **Code masking** | Start code hidden from client after START_CONFIRMED; release code hidden until RELEASE_PENDING |

---

## Mapper Methods (Role-Aware Masking)

| Method | When Used | Shows |
|--------|-----------|-------|
| `toDtoWithStartCodeOnly` | POST /codes | Start code only |
| `toDtoWithReleaseCodeOnly` | After START_CONFIRMED | Release code only |
| `toDtoWithoutCodes` | Worker-facing responses | Neither code |
| `toDtoForClientFetch` | GET /codes | Context-sensitive: start code when START_PENDING, release code when RELEASE_PENDING |

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `JobRepository` | Job status transitions |
| `JobConfirmationCodeRepository` | Code persistence |
| `JobConfirmationCodeMapper` | Role-aware DTO mapping |
