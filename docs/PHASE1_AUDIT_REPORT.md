# AroundU — Phase 1 Audit & Alignment Report

## 1. Feature – Endpoint Mapping Table

### Flutter Mobile ↔ Backend API

| Flutter Feature / Screen | API Call in Flutter | Backend Controller | Backend Endpoint | Status |
|---|---|---|---|---|
| **Login** | `POST /api/v1/auth/login` | AuthController | `POST /api/v1/auth/login` | ✅ Aligned |
| **Client Registration** | `POST /api/v1/client/register` | ClientController | `POST /api/v1/client/register` | ✅ Aligned |
| **Worker Registration** | `POST /api/v1/worker/register` | WorkerController | `POST /api/v1/worker/register` | ✅ Aligned |
| **Client Profile (self)** | `GET /api/v1/client/me` | ClientController | `GET /api/v1/client/me` | ✅ Aligned |
| **Worker Profile (self)** | `GET /api/v1/worker/me` | WorkerController | `GET /api/v1/worker/me` | ✅ Aligned |
| **Client Profile (by id)** | `GET /api/v1/client/{id}` | ClientController | `GET /api/v1/client/{clientId}` | ✅ Aligned (ADMIN/self only) |
| **Worker Profile (by id)** | `GET /api/v1/worker/{id}` | WorkerController | `GET /api/v1/worker/{workerId}` | ✅ Aligned (ADMIN/self only) |
| **Update Client Profile** | `PATCH /api/v1/client/update/{id}` | ClientController | `PATCH /api/v1/client/update/{clientId}` | ✅ Aligned |
| **Update Worker Profile** | `PATCH /api/v1/worker/update/{id}` | WorkerController | `PATCH /api/v1/worker/update/{workerId}` | ✅ Aligned |
| **Delete Client** | `DELETE /api/v1/client/{id}` | ClientController | `DELETE /api/v1/client/{clientId}` | ✅ Aligned |
| **Delete Worker** | `DELETE /api/v1/worker/{id}` | WorkerController | `DELETE /api/v1/worker/{workerId}` | ✅ Aligned |
| **List All Clients (admin)** | `GET /api/v1/client/all` | ClientController | `GET /api/v1/client/all` | ✅ Aligned |
| **List All Workers (admin)** | `GET /api/v1/worker/all` | WorkerController | `GET /api/v1/worker/all` | ✅ Aligned |
| **Create Job** | `POST /api/v1/jobs` | JobController | `POST /api/v1/jobs` | ✅ Aligned |
| **Get Job by ID** | `GET /api/v1/jobs/{jobId}` | JobController | `GET /api/v1/jobs/{jobId}` | ✅ Aligned |
| **Search Jobs** | `GET /api/v1/jobs` | JobController | `GET /api/v1/jobs` | ✅ Aligned |
| **Client's Jobs** | `GET /api/v1/jobs/client/{clientId}` | JobController | `GET /api/v1/jobs/client/{clientId}` | ✅ Aligned |
| **Client's Past Jobs** | `GET /api/v1/jobs/client/{clientId}/past` | JobController | `GET /api/v1/jobs/client/{clientId}/past` | ✅ Aligned |
| **Client's Specific Job** | `GET /api/v1/jobs/client/{clientId}/{jobId}` | JobController | `GET /api/v1/jobs/client/{clientId}/{jobId}` | ✅ Aligned |
| **Worker Feed** | `GET /api/v1/jobs/worker/{workerId}/feed` | JobController | `GET /api/v1/jobs/worker/{workerId}/feed` | ✅ Aligned |
| **Worker's Specific Job** | `GET /api/v1/jobs/worker/{workerId}/{jobId}` | JobController | `GET /api/v1/jobs/worker/{workerId}/{jobId}` | ✅ Aligned |
| **Update Job** | `PATCH /api/v1/jobs/{jobId}` | JobController | `PATCH /api/v1/jobs/{jobId}` | ✅ Aligned |
| **Update Job Status** | `PATCH /api/v1/jobs/{id}/status` | JobController | `PATCH /api/v1/jobs/{jobId}/status` | ✅ Aligned |
| **Delete Job** | `DELETE /api/v1/jobs/{id}` | JobController | `DELETE /api/v1/jobs/{jobId}` | ✅ Aligned |
| **Place Bid** | `POST /api/v1/bid/jobs/{jobId}/bids` | BidController | `POST /api/v1/bid/jobs/{jobId}/bids` | ✅ Aligned |
| **Get Bids for Job** | `GET /api/v1/bid/jobs/{jobId}/bids` | BidController | `GET /api/v1/bid/jobs/{jobId}/bids` | ✅ Aligned |
| **Accept Bid** | `POST /api/v1/bid/bids/{bidId}/accept` | BidController | `POST /api/v1/bid/bids/{bidId}/accept` | ✅ Aligned |
| **Bid Handshake** | `POST /api/v1/bid/bids/{bidId}/handshake` | BidController | `POST /api/v1/bid/bids/{bidId}/handshake` | ✅ Aligned |
| **Generate Job Code** | `POST /api/v1/jobs/{id}/codes` | JobCodeController | `POST /api/v1/jobs/{jobId}/codes` | ✅ Aligned |
| **Start Job Code** | `POST /api/v1/jobs/{id}/codes/start` | JobCodeController | `POST /api/v1/jobs/{jobId}/codes/start` | ✅ Aligned |
| **Release Job Code** | `POST /api/v1/jobs/{id}/codes/release` | JobCodeController | `POST /api/v1/jobs/{jobId}/codes/release` | ✅ Aligned |
| **Lock Payment** | `POST /api/v1/jobs/{id}/payments/lock` | PaymentController | `POST /api/v1/jobs/{jobId}/payments/lock` | ✅ Aligned |
| **Release Payment** | `POST /api/v1/jobs/{id}/payments/release` | PaymentController | `POST /api/v1/jobs/{jobId}/payments/release` | ✅ Aligned |
| **Submit Review** | *Not yet in Flutter* | ReviewController | `POST /api/v1/reviews/jobs/{jobId}` | 🆕 New |
| **Get Worker Reviews** | *Not yet in Flutter* | ReviewController | `GET /api/v1/reviews/workers/{workerId}` | 🆕 New |
| **Get Job Review** | *Not yet in Flutter* | ReviewController | `GET /api/v1/reviews/jobs/{jobId}` | 🆕 New |
| **Send Chat Message** | *Not yet in Flutter* | ChatController | `POST /api/v1/chat/jobs/{jobId}/messages` | 🆕 New |
| **Get Conversation Messages** | *Not yet in Flutter* | ChatController | `GET /api/v1/chat/conversations/{id}/messages` | 🆕 New |
| **List Conversations** | *Not yet in Flutter* | ChatController | `GET /api/v1/chat/conversations` | 🆕 New |
| **Mark Messages Read** | *Not yet in Flutter* | ChatController | `POST /api/v1/chat/conversations/{id}/read` | 🆕 New |
| **Public Worker Profile** | *Not yet in Flutter* | PublicProfileController | `GET /api/v1/public/worker/{workerId}` | 🆕 New |
| **Public Client Profile** | *Not yet in Flutter* | PublicProfileController | `GET /api/v1/public/client/{clientId}` | 🆕 New |

---

## 2. Identified Gaps (Before Implementation)

| Gap | Module | Description |
|---|---|---|
| **Review system stub-only** | `review/` | Entity existed but had no fields, no repository, no service, no controller |
| **Chat/messaging missing** | — | No module existed at all |
| **Public profile view blocked** | `user/` | Existing GET endpoints require `ADMIN` or self-auth; no way for Client to view Worker profile or vice versa |

---

## 3. New Files Created

### Review Module (11 files)

| File | Type | Description |
|---|---|---|
| `review/entity/Review.java` | Entity (updated) | Added `job`, `reviewer` (Client), `rating`, `comment`, timestamps, unique constraint |
| `review/repository/ReviewRepository.java` | Repository | JPA repository with `findByWorkerId`, `findByJobIdAndReviewerId`, `existsByJobIdAndReviewerId`, `averageRatingByWorkerId` |
| `review/dto/ReviewCreateRequest.java` | DTO | Request body: `rating` (1–5), `comment` (optional, ≤1000 chars) |
| `review/dto/ReviewResponseDTO.java` | DTO | Response: id, rating, comment, reviewerName, jobId, workerId, reviewerId, timestamps |
| `review/mapper/ReviewMapper.java` | Mapper | MapStruct mapper: `toDto()`, `toDtoList()` |
| `review/exception/ReviewNotFoundException.java` | Exception | 404 |
| `review/exception/ReviewValidationException.java` | Exception | 400 |
| `review/service/ReviewService.java` | Service interface | `submitReview`, `getWorkerReviews`, `getReviewForJob` |
| `review/service/ReviewServiceImpl.java` | Service impl | Full validation (job COMPLETED, reviewer is job owner, no duplicates), updates worker avg rating |
| `review/controller/ReviewController.java` | Controller | 3 endpoints (see table above) |

### Chat Module (12 files)

| File | Type | Description |
|---|---|---|
| `chat/entity/Conversation.java` | Entity | Job-scoped, two participants, `lastMessageAt`, unique constraint |
| `chat/entity/ChatMessage.java` | Entity | Content, senderId, isRead, `createdAt` |
| `chat/repository/ConversationRepository.java` | Repository | `findByJobAndParticipants`, `findByParticipant`, `findByJobId` |
| `chat/repository/ChatMessageRepository.java` | Repository | Paginated messages, unread count, bulk `markAsRead` |
| `chat/dto/ChatMessageRequest.java` | DTO | `recipientId`, `content` |
| `chat/dto/ChatMessageResponseDTO.java` | DTO | id, conversationId, senderId, content, isRead, createdAt |
| `chat/dto/ConversationResponseDTO.java` | DTO | id, jobId, jobTitle, participant names, unreadCount, timestamps |
| `chat/mapper/ChatMessageMapper.java` | Mapper | MapStruct: `toDto()`, `toDtoList()` |
| `chat/exception/ChatValidationException.java` | Exception | 400 |
| `chat/exception/ConversationNotFoundException.java` | Exception | 404 |
| `chat/service/ChatService.java` | Service interface | `sendMessage`, `getMessages`, `getConversations`, `markAsRead` |
| `chat/service/ChatServiceImpl.java` | Service impl | Full validation (sender must be job participant), conversation auto-creation, name resolution |
| `chat/controller/ChatController.java` | Controller | 4 endpoints (see table above) |

### Public Profile (3 files)

| File | Type | Description |
|---|---|---|
| `user/dto/PublicWorkerProfileDTO.java` | DTO | Non-sensitive: id, name, profileImageUrl, rating, experience, certifications, isOnDuty, skills |
| `user/dto/PublicClientProfileDTO.java` | DTO | Non-sensitive: id, name, profileImageUrl |
| `user/controller/PublicProfileController.java` | Controller | 2 endpoints accessible to any authenticated user |

### Infrastructure Updates (3 files modified)

| File | Change |
|---|---|
| `common/constants/URIConstants.java` | Added `REVIEW_BASE`, `CHAT_BASE`, `PUBLIC_BASE` |
| `common/exception/GlobalExceptionHandler.java` | Added 4 exception handlers for review & chat exceptions |
| `infrastructure/config/SecurityConfig.java` | Whitelisted `/api/v1/public/**`, `/api/v1/reviews/**`, `/api/v1/chat/**` |

---

## 4. Complete Endpoint Inventory (Post-Implementation)

### Existing (32 endpoints)
```
POST   /api/v1/auth/login
POST   /api/v1/client/register
GET    /api/v1/client/{clientId}
GET    /api/v1/client/me
GET    /api/v1/client/all
PATCH  /api/v1/client/update/{clientId}
DELETE /api/v1/client/{clientId}
POST   /api/v1/worker/register
GET    /api/v1/worker/{workerId}
GET    /api/v1/worker/me
GET    /api/v1/worker/all
PATCH  /api/v1/worker/update/{workerId}
DELETE /api/v1/worker/{workerId}
POST   /api/v1/jobs
GET    /api/v1/jobs/{jobId}
GET    /api/v1/jobs
GET    /api/v1/jobs/client/{clientId}
GET    /api/v1/jobs/client/{clientId}/past
GET    /api/v1/jobs/client/{clientId}/{jobId}
GET    /api/v1/jobs/worker/{workerId}/feed
GET    /api/v1/jobs/worker/{workerId}/{jobId}
PATCH  /api/v1/jobs/{jobId}
PATCH  /api/v1/jobs/{jobId}/status
DELETE /api/v1/jobs/{jobId}
POST   /api/v1/bid/jobs/{jobId}/bids
GET    /api/v1/bid/jobs/{jobId}/bids
POST   /api/v1/bid/bids/{bidId}/accept
POST   /api/v1/bid/bids/{bidId}/handshake
POST   /api/v1/jobs/{jobId}/codes
POST   /api/v1/jobs/{jobId}/codes/start
POST   /api/v1/jobs/{jobId}/codes/release
POST   /api/v1/jobs/{jobId}/payments/lock
POST   /api/v1/jobs/{jobId}/payments/release
```

### New (9 endpoints)
```
POST   /api/v1/reviews/jobs/{jobId}             — Submit review
GET    /api/v1/reviews/workers/{workerId}        — Get worker's reviews
GET    /api/v1/reviews/jobs/{jobId}              — Get review for a job
POST   /api/v1/chat/jobs/{jobId}/messages        — Send message
GET    /api/v1/chat/conversations/{id}/messages  — Get conversation messages
GET    /api/v1/chat/conversations                — List user's conversations
POST   /api/v1/chat/conversations/{id}/read      — Mark messages as read
GET    /api/v1/public/worker/{workerId}           — Public worker profile
GET    /api/v1/public/client/{clientId}           — Public client profile
```

**Total: 42 endpoints**

---

## 5. Architecture Quality Checklist

| Principle | Status |
|---|---|
| No business logic in controllers | ✅ All controllers delegate to service layer |
| All service write methods `@Transactional` | ✅ |
| Read-only methods `@Transactional(readOnly = true)` | ✅ |
| Consistent `ApiResponse<T>` wrapper | ✅ |
| MapStruct for entity→DTO mapping | ✅ |
| Proper validation (Bean Validation + business rules) | ✅ |
| Custom exceptions with `GlobalExceptionHandler` handlers | ✅ |
| Security rules in `SecurityConfig` | ✅ |
| Existing patterns followed (CQRS repos, DTO separation, etc.) | ✅ |

---

## 6. Build Status

**BUILD SUCCESS** — 228 source files compiled with JDK 21 (zero errors, zero warnings aside from standard annotation processing note).
