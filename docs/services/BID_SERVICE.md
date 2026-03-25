# Bid Service

> Bid placement, acceptance, handshake, and anti-fraud mechanisms.

---

## Overview

The Bid module handles the bidding lifecycle — workers place bids on open jobs, clients accept bids, and workers confirm via handshake. Includes a Bloom filter for fast duplicate detection, rate limiting, and an active-job guard to prevent workers from gaming the system.

**Package:** `com.beingadish.AroundU.bid`

---

## File Inventory

| File | Type | Description |
|------|------|-------------|
| `entity/Bid.java` | Entity | `id`, `job` (Job), `worker` (Worker), `amount`, `partnerName`, `notes`, `status` (BidStatus), timestamps |
| `repository/BidRepository.java` | Repository | CRUD + `findByJobId`, `findByWorkerIdAndJobId` |
| `service/BidService.java` | Interface | Bid lifecycle contract |
| `service/impl/BidServiceImpl.java` | Implementation | Full bid logic with guards |
| `service/BidDuplicateCheckService.java` | Interface | Bloom filter duplicate checking |
| `service/impl/BidDuplicateCheckServiceImpl.java` | Implementation | Guava Bloom filter check |
| `service/impl/NoOpBidDuplicateCheckService.java` | NoOp fallback | Disabled duplicate check |
| `service/BloomFilterMetricsService.java` | Interface | Bloom filter stats |
| `service/impl/BloomFilterMetricsServiceImpl.java` | Implementation | FPP, size, counts |
| `service/impl/NoOpBloomFilterMetricsServiceImpl.java` | NoOp fallback | Disabled metrics |
| `service/ProfileViewTrackingService.java` | Interface | Track profile views on bid listing |
| `service/impl/ProfileViewTrackingServiceImpl.java` | Implementation | View counting |
| `service/impl/NoOpProfileViewTrackingService.java` | NoOp fallback | Disabled tracking |
| `controller/BidController.java` | Controller | 4 REST endpoints |
| `dto/BidCreateRequest.java` | DTO | `amount`, `partnerName`, `notes` |
| `dto/BidHandshakeRequest.java` | DTO | `accept` (boolean) |
| `dto/BidResponseDTO.java` | DTO | Full bid response |
| `mapper/BidMapper.java` | Mapper | Entity ↔ DTO (MapStruct) |
| `exception/DuplicateBidException.java` | Exception | 409 Conflict |

---

## Service Methods

### `BidService`

| Method | Signature | Description |
|--------|-----------|-------------|
| `placeBid` | `BidResponseDTO placeBid(Long jobId, Long workerId, BidCreateRequest request)` | Place a bid on an open job |
| `listBidsForJob` | `List<BidResponseDTO> listBidsForJob(Long jobId)` | List all bids for a job |
| `acceptBid` | `BidResponseDTO acceptBid(Long bidId, Long clientId)` | Client accepts a bid; rejects others |
| `handshake` | `BidResponseDTO handshake(Long bidId, Long workerId, BidHandshakeRequest request)` | Worker accepts or declines the handshake |

---

## REST Endpoints

| Method | Path | Auth | Rate Limit | Description |
|--------|------|------|------------|-------------|
| `POST` | `/api/v1/bid/jobs/{jobId}/bids` | Worker | 20/hr | Place bid |
| `GET` | `/api/v1/bid/jobs/{jobId}/bids` | Authenticated | — | List bids for job |
| `POST` | `/api/v1/bid/bids/{bidId}/accept` | Client | — | Accept a bid |
| `POST` | `/api/v1/bid/bids/{bidId}/handshake` | Worker | — | Accept/decline handshake |

---

## Business Rules & Guards

### `placeBid` Guards

1. **Job must be `OPEN_FOR_BIDS`** — rejects bid on jobs in any other status
2. **Worker must be on-duty** — `isOnDuty == true`
3. **Worker must not be blocked** — `blockedUntil` must be null or in the past
4. **No active job** — worker cannot have a job in `READY_TO_START`, `IN_PROGRESS`, `COMPLETED_PENDING_PAYMENT`, or `BID_SELECTED_AWAITING_HANDSHAKE`
5. **No duplicate bids** — Bloom filter precheck + DB unique constraint

### `acceptBid` Side Effects

1. Bid status → `SELECTED`
2. All other bids for the job → `REJECTED`
3. Job status → `BID_SELECTED_AWAITING_HANDSHAKE`
4. Job `assignedTo` → worker
5. **Chat conversation auto-created** between client and worker (fail-safe)
6. Metrics counter incremented

### `handshake` Guards

- **Accept:** Worker must have **no** active job in `READY_TO_START` / `IN_PROGRESS` / `COMPLETED_PENDING_PAYMENT`. Job transitions to `READY_TO_START`.
- **Decline:** Job returns to `OPEN_FOR_BIDS`, bid status reset, `assignedTo` cleared.

---

## Anti-Fraud Architecture

```
placeBid()
  │
  ├─ 1. BloomFilter.mightContain(workerId + jobId)
  │     Fast O(1) check — may return false positive, never false negative
  │
  ├─ 2. DB constraint check (fallback for Bloom false positives)
  │
  ├─ 3. Active job guard (WORKER_PERFORMING_STATUSES)
  │
  └─ 4. Rate limit: 20 bids/hour via @RateLimit annotation

handshake(accept)
  │
  └─ Active job guard — prevents accepting multiple handshakes simultaneously
```

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `JobRepository` | Job lookup and status updates |
| `BidRepository` | Bid persistence |
| `ChatService` | Auto-create conversation on bid acceptance |
| `MetricsService` | Bid placement and acceptance counters |
| `BidDuplicateCheckService` | Bloom filter duplicate detection |
| `NotificationService` | Notify participants on bid events |
