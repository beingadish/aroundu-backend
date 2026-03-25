# Review Service

> Bi-directional post-job reviews with eligibility guards and rating aggregation.

---

## Overview

The Review module allows both clients and workers to leave reviews after a job is completed. Reviews are gated by job status eligibility (`PAYMENT_RELEASED` or `COMPLETED`) and a one-review-per-user-per-job constraint enforced by both business logic and a database unique constraint.

**Package:** `com.beingadish.AroundU.review`

---

## File Inventory

| File | Type | Description |
|------|------|-------------|
| `entity/Review.java` | Entity | `job`, `reviewer` (User), `reviewee` (User), `rating` (1–5), `comment`, `createdAt`; unique constraint on `(job_id, reviewer_id)` |
| `repository/ReviewRepository.java` | Repository | `findByWorkerId`, `findByJobIdAndReviewerId`, `existsByJobIdAndReviewerId`, `averageRatingByWorkerId` |
| `service/ReviewService.java` | Interface | 5 methods |
| `service/ReviewServiceImpl.java` | Implementation | Validation, rating aggregation, duplicate prevention |
| `controller/ReviewController.java` | Controller | 3 REST endpoints |
| `dto/ReviewCreateRequest.java` | DTO | `rating` (1–5, required), `comment` (optional, ≤1000 chars) |
| `dto/ReviewResponseDTO.java` | DTO | Full review response |
| `mapper/ReviewMapper.java` | Mapper | Entity ↔ DTO (MapStruct) |
| `exception/ReviewNotFoundException.java` | Exception | 404 |
| `exception/ReviewValidationException.java` | Exception | 400 |

---

## Service Methods

### `ReviewService`

| Method | Signature | Description |
|--------|-----------|-------------|
| `submitReview` | `ReviewResponseDTO submitReview(Long jobId, Long clientId, ReviewCreateRequest request)` | Client reviews the worker |
| `submitWorkerReview` | `ReviewResponseDTO submitWorkerReview(Long jobId, Long workerId, ReviewCreateRequest request)` | Worker reviews the client |
| `hasReviewedJob` | `boolean hasReviewedJob(Long jobId, Long reviewerId)` | Check if user already reviewed this job |
| `getWorkerReviews` | `List<ReviewResponseDTO> getWorkerReviews(Long workerId)` | All reviews for a worker |
| `getReviewForJob` | `ReviewResponseDTO getReviewForJob(Long jobId)` | Get the review for a specific job |

---

## REST Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/v1/reviews/jobs/{jobId}` | Client or Worker | Submit review |
| `GET` | `/api/v1/reviews/workers/{workerId}` | Authenticated | Get worker's reviews |
| `GET` | `/api/v1/reviews/jobs/{jobId}` | Authenticated | Get review for a job |

---

## Business Rules

| Rule | Enforcement |
|------|-------------|
| Job must be review-eligible | `job.getJobStatus().isReviewEligible()` — only `PAYMENT_RELEASED` or `COMPLETED` |
| One review per reviewer per job | `existsByJobIdAndReviewerId()` + DB unique constraint (`DataIntegrityViolationException` catch) |
| Reviewer must be the job owner (client review) | `job.getCreatedBy().getId() == clientId` |
| Reviewer must be the assigned worker (worker review) | `job.getAssignedTo().getId() == workerId` |
| Rating range | `@Min(1) @Max(5)` on `ReviewCreateRequest.rating` |
| Comment length | `@Size(max = 1000)` on `ReviewCreateRequest.comment` |
| Worker rating update | After review submission, recalculates `worker.overallRating` via `averageRatingByWorkerId()` |

---

## Rating Aggregation

```
submitReview()
     │
     ├─ Save review to DB
     │
     └─ Update worker.overallRating
          = ReviewRepository.averageRatingByWorkerId(workerId)
          → Stored on Worker entity for fast access
```

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `ReviewRepository` | Review persistence and aggregation |
| `JobRepository` | Job eligibility and ownership checks |
| `WorkerRepository` | Rating update on worker entity |
