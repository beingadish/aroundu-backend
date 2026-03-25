# User Service

> Client, Worker, and Admin management — registration, profiles, penalties, and image uploads.

---

## Overview

The User module manages the three user types in AroundU. Clients post jobs and manage payments; Workers accept jobs and earn; Admins oversee the platform. Each user type has its own entity (extending `User`), dedicated service, and controller.

**Package:** `com.beingadish.AroundU.user`

---

## File Inventory

### Entities

| File | Description |
|------|-------------|
| `entity/User.java` | Base entity: email, password, name, phone, profileImageUrl, currentAddress |
| `entity/Client.java` | Extends `User`: `postedJobs`, `savedAddresses` |
| `entity/Worker.java` | Extends `User`: `isOnDuty`, `cancellationCount`, `blockedUntil`, `engagedJobList`, `overallRating`, `payoutAccount` |
| `entity/Admin.java` | Extends `User`: admin-specific configuration |

### Services

| File | Type | Description |
|------|------|-------------|
| `service/ClientService.java` | Interface | Client CRUD + saved addresses |
| `service/impl/ClientServiceImpl.java` | Implementation | Full client lifecycle |
| `service/WorkerService.java` | Interface | Worker CRUD + duty status |
| `service/impl/WorkerServiceImpl.java` | Implementation | Full worker lifecycle |
| `service/WorkerPenaltyService.java` | Interface | Cancellation penalty system |
| `service/impl/WorkerPenaltyServiceImpl.java` | Implementation | Block/unblock logic |
| `service/UserProfileService.java` | Interface | Profile image upload/delete |
| `service/impl/UserProfileServiceImpl.java` | Implementation | Image storage integration |
| `service/RegistrationValidationService.java` | Interface | Pre-registration validation (email uniqueness, etc.) |
| `service/impl/NoOpRegistrationValidationService.java` | NoOp fallback | Used when validation service is disabled |

### Controllers

| File | Description |
|------|-------------|
| `controller/ClientController.java` | Client registration, CRUD, saved addresses |
| `controller/WorkerController.java` | Worker registration, CRUD, duty status |
| `controller/AdminController.java` | Admin operations |
| `controller/PublicProfileController.java` | Non-sensitive public profile views |
| `controller/UserProfileController.java` | Profile image management |

### DTOs

| File | Description |
|------|-------------|
| `dto/client/ClientRegisterRequestDTO.java` | Registration request |
| `dto/client/ClientDetailsResponseDTO.java` | Full profile response |
| `dto/client/ClientUpdateRequestDTO.java` | Profile update fields |
| `dto/worker/WorkerSignupRequestDTO.java` | Worker registration request |
| `dto/worker/WorkerDetailDTO.java` | Full worker profile response |
| `dto/worker/WorkerUpdateRequestDTO.java` | Worker update fields |
| `dto/PublicWorkerProfileDTO.java` | Non-sensitive worker profile |
| `dto/PublicClientProfileDTO.java` | Non-sensitive client profile |
| `dto/UserDetailDTO.java` | Generic user detail |
| `dto/UserSummaryDTO.java` | Compact user summary |

---

## Service Methods

### `ClientService`

| Method | Description |
|--------|-------------|
| `registerClient(request)` | Creates a new client account |
| `getClientDetails(clientId)` | Returns full client profile |
| `getAllClients(page, size)` | Paginated list of all clients (admin) |
| `updateClientDetails(clientId, request)` | Updates client profile fields |
| `deleteClient(clientId)` | Soft/hard deletes client |
| `addSavedAddress(clientId, addressDTO)` | Adds an address to client's saved list |
| `deleteSavedAddress(clientId, addressId)` | Removes a saved address |

### `WorkerService`

| Method | Description |
|--------|-------------|
| `registerWorker(request)` | Creates a new worker account |
| `getWorkerDetails(workerId)` | Returns full worker profile |
| `getAllWorkers(page, size)` | Paginated list of all workers (admin) |
| `updateWorkerDetails(workerId, request)` | Updates worker profile fields |
| `updateDutyStatus(workerId, isOnDuty)` | Toggles worker on/off duty |
| `deleteWorker(workerId)` | Soft/hard deletes worker |

### `WorkerPenaltyService`

| Method | Description |
|--------|-------------|
| `recordCancellation(workerId)` | Increments cancellation count; blocks if threshold (3) reached for 7 days |
| `isBlocked(workerId)` | Checks whether a worker is currently blocked from accepting jobs |
| `unblockExpiredWorkers()` | Scheduler-called: unblocks workers whose block period has expired |

### `UserProfileService`

| Method | Description |
|--------|-------------|
| `uploadProfileImage(userId, file)` | Uploads/replaces profile image; returns public URL |
| `deleteProfileImage(userId)` | Removes profile image |

---

## REST Endpoints

### Client Endpoints (`/api/v1/client`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/register` | Public | Register new client |
| `GET` | `/me` | Client | Get own profile |
| `GET` | `/{clientId}` | Admin/Self | Get client by ID |
| `GET` | `/all` | Admin | List all clients (paginated) |
| `PATCH` | `/update/{clientId}` | Self | Update profile |
| `DELETE` | `/{clientId}` | Self/Admin | Delete client |

### Worker Endpoints (`/api/v1/worker`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/register` | Public | Register new worker |
| `GET` | `/me` | Worker | Get own profile |
| `GET` | `/{workerId}` | Admin/Self | Get worker by ID |
| `GET` | `/all` | Admin | List all workers (paginated) |
| `PATCH` | `/update/{workerId}` | Self | Update profile |
| `DELETE` | `/{workerId}` | Self/Admin | Delete worker |

### Public Profile Endpoints (`/api/v1/public`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/worker/{workerId}` | Any authenticated | Non-sensitive worker profile |
| `GET` | `/client/{clientId}` | Any authenticated | Non-sensitive client profile |

---

## Worker Penalty System

```
Worker cancels a job
     │
     ├─ cancellationCount++
     │
     └─ if cancellationCount >= 3
          → blockedUntil = now + 7 days
          → Worker cannot place bids or accept handshakes
          │
          └─ WorkerPenaltyScheduler runs periodically
               → unblockExpiredWorkers()
               → resets blockedUntil when period expires
```

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `ClientRepository/ReadRepository/WriteRepository` | Client persistence (CQRS) |
| `WorkerRepository/ReadRepository/WriteRepository` | Worker persistence (CQRS) |
| `AdminRepository` | Admin persistence |
| `ImageStorageService` | Profile image storage |
| `RegistrationValidationService` | Pre-registration email/phone checks |
| `WorkerPenaltyScheduler` | Scheduled penalty expiry |
