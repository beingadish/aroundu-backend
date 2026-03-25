# Job Service

> Job CRUD, status machine, worker feed, client job management, and geo-integrated search.

---

## Overview

The Job module is the core of AroundU. Clients create jobs; workers discover them via a geo-enabled feed; jobs transition through a strict state machine from creation to completion and payment.

**Package:** `com.beingadish.AroundU.job`

---

## File Inventory

| File | Type | Description |
|------|------|-------------|
| `entity/Job.java` | Entity | Core job entity with title, descriptions, price, location, status, urgency, skills, assigned worker |
| `entity/JobConfirmationCode.java` | Entity | OTP codes for job start/release (see [JOB_CODE_SERVICE.md](JOB_CODE_SERVICE.md)) |
| `repository/JobRepository.java` | Repository | CRUD + status queries, geo-filtered queries, client/worker job lists |
| `service/JobService.java` | Interface | Full job lifecycle contract |
| `service/impl/JobServiceImpl.java` | Implementation | ~600 lines — CRUD, state machine, geo-sync, feed, caching |
| `controller/JobController.java` | Controller | 14 REST endpoints |
| `dto/JobCreateRequest.java` | DTO | Title, descriptions, price, location, urgency, skills, payment mode |
| `dto/JobUpdateRequest.java` | DTO | Partial update fields |
| `dto/JobStatusUpdateRequest.java` | DTO | `status` (target status string) |
| `dto/JobDetailDTO.java` | DTO | Full job detail response |
| `dto/JobSummaryDTO.java` | DTO | Compact job response for lists |
| `dto/JobFilterRequest.java` | DTO | Client job filtering (status, pagination, sorting) |
| `dto/WorkerJobFeedRequest.java` | DTO | Worker feed params (radius, skills, pagination, sorting) |
| `dto/WorkerBriefDTO.java` | DTO | Brief worker info for job cards |
| `mapper/JobMapper.java` | Mapper | Entity ↔ DTO (MapStruct) |
| `event/JobModifiedEvent.java` | Event | Published after CREATE, UPDATE, DELETE, STATUS_CHANGE |
| `event/JobExpiredEvent.java` | Event | Published when job expires |
| `exception/JobNotFoundException.java` | Exception | 404 |
| `exception/JobValidationException.java` | Exception | 400 |
| `model/JobModel.java` | Model | Internal service model |

---

## Service Methods

### `JobService`

| Method | Signature | Description |
|--------|-----------|-------------|
| `createJob` | `JobDetailDTO createJob(Long clientId, JobCreateRequest request)` | Creates job, syncs to geo-index, fires event |
| `updateJob` | `JobDetailDTO updateJob(Long jobId, Long clientId, JobUpdateRequest request)` | Updates job fields; fires event with `locationChanged` flag |
| `getJobDetail` | `JobDetailDTO getJobDetail(Long jobId)` | Returns full job detail (cached 30 min) |
| `listJobs` | `List<JobSummaryDTO> listJobs(city, area, skillIds)` | Search by city/area/skills |
| `getClientJobs` | `PageResponse<JobSummaryDTO> getClientJobs(clientId, filterRequest)` | Paginated client job list (cached 10 min) |
| `getClientPastJobs` | `PageResponse<JobSummaryDTO> getClientPastJobs(clientId, page, size)` | Client's completed/cancelled jobs |
| `getJobForClient` | `JobDetailDTO getJobForClient(jobId, clientId)` | Client-specific job view |
| `updateJobStatus` | `JobDetailDTO updateJobStatus(jobId, clientId, request)` | Client updates status (validated by state machine) |
| `updateJobStatusByWorker` | `JobDetailDTO updateJobStatusByWorker(jobId, workerId, request)` | Worker updates status (restricted to `IN_PROGRESS` / `COMPLETED_PENDING_PAYMENT`) |
| `getWorkerFeed` | `PageResponse<JobSummaryDTO> getWorkerFeed(workerId, request)` | Geo-radius search → PG validation → skill filter → distance enrichment → sorting (cached 5 min) |
| `getJobForWorker` | `JobDetailDTO getJobForWorker(jobId, workerId)` | Worker-specific job view |
| `cancelJobByWorker` | `JobDetailDTO cancelJobByWorker(jobId, workerId)` | Worker cancels; triggers penalty |
| `getWorkerMyJobs` | `List<JobSummaryDTO> getWorkerMyJobs(workerId, statuses)` | Worker's jobs filtered by status |
| `deleteJob` | `void deleteJob(jobId, clientId)` | Deletes job; removes from geo-index |

---

## REST Endpoints

| Method | Path | Auth | Rate Limit | Description |
|--------|------|------|------------|-------------|
| `POST` | `/api/v1/jobs` | Client | 5/hr | Create job |
| `PATCH` | `/api/v1/jobs/{jobId}` | Client | — | Update job |
| `PATCH` | `/api/v1/jobs/{jobId}/status` | Client | — | Update job status |
| `PATCH` | `/api/v1/jobs/{jobId}/status/worker` | Worker | — | Worker status update |
| `POST` | `/api/v1/jobs/{jobId}/worker-cancel` | Worker | — | Worker cancels job |
| `DELETE` | `/api/v1/jobs/{jobId}` | Client | — | Delete job |
| `GET` | `/api/v1/jobs/{jobId}` | Any | — | Get job detail |
| `GET` | `/api/v1/jobs` | Any | — | Search jobs |
| `GET` | `/api/v1/jobs/client/{clientId}` | Client | — | Client's jobs |
| `GET` | `/api/v1/jobs/client/{clientId}/past` | Client | — | Client's past jobs |
| `GET` | `/api/v1/jobs/client/{clientId}/{jobId}` | Client | — | Client's specific job |
| `GET` | `/api/v1/jobs/worker/{workerId}/feed` | Worker | 30/min | Worker feed |
| `GET` | `/api/v1/jobs/worker/{workerId}/{jobId}` | Worker | — | Worker's specific job |
| `GET` | `/api/v1/jobs/worker/{workerId}/my-jobs` | Worker | — | Worker's jobs by status |

---

## State Machine

```
CREATED → OPEN_FOR_BIDS → BID_SELECTED_AWAITING_HANDSHAKE → READY_TO_START
     → IN_PROGRESS → COMPLETED_PENDING_PAYMENT → PAYMENT_RELEASED → COMPLETED
```

**Terminal states:** `COMPLETED`, `CANCELLED`, `JOB_CLOSED_DUE_TO_EXPIRATION`

**Client-only transitions:** Most status changes  
**Worker-only transitions:** `READY_TO_START → IN_PROGRESS`, `IN_PROGRESS → COMPLETED_PENDING_PAYMENT`

See [PRODUCTION_READINESS.md](../PRODUCTION_READINESS.md) for the full state diagram.

---

## Caching Strategy

| Cache | TTL | Evicted On |
|-------|-----|------------|
| `job:detail::{jobId}` | 30 min | Any mutation on that job |
| `job:client:list::{clientId}:*` | 10 min | Mutation by that client |
| `job:worker:feed::*` | 5 min | CREATE, DELETE, STATUS_CHANGE, or UPDATE with `locationChanged=true` |

---

## Worker Feed Pipeline

```
getWorkerFeed(workerId, request)
  │
  ├─ 1. Load worker → extract (lat, lon)
  ├─ 2. Redis GEORADIUS → nearby job IDs
  ├─ 3. PG re-validation → only OPEN_FOR_BIDS
  ├─ 4. Skill filtering (in-memory)
  ├─ 5. Distance enrichment (Haversine)
  ├─ 6. Popularity enrichment (bid count)
  ├─ 7. Virtual sorting (distance/popularity)
  └─ 8. Return PageImpl
```

See [GEOSEARCH.md](../GEOSEARCH.md) for detailed geo-search architecture.

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `JobGeoService` | Redis geo-index operations |
| `CacheEvictionService` | Cache invalidation on mutations |
| `MetricsService` | Job creation/completion/cancellation counters |
| `NotificationService` | Status change notifications |
| `WorkerPenaltyService` | Cancellation penalties on worker cancel |
| `SkillRepository` | Skill-based filtering |
