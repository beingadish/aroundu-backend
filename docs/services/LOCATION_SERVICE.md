# Location Service

> Redis geo-index management, address entities, and PostgreSQL ↔ Redis synchronisation.

---

## Overview

The Location module maintains a Redis geo-index (`geo:jobs:open`) for fast proximity-based job discovery. PostgreSQL is always the source of truth; Redis is an index only. The module includes startup sync, daily cleanup, and a failed-sync retry mechanism.

**Package:** `com.beingadish.AroundU.location`

---

## File Inventory

| File | Type | Description |
|------|------|-------------|
| `entity/Address.java` | Entity | `name`, `place`, `city`, `area`, `postalCode`, `country`, `latitude`, `longitude` |
| `entity/FailedGeoSync.java` | Entity | Failed Redis sync record for retry |
| `repository/AddressRepository.java` | Repository | Address CRUD |
| `repository/FailedGeoSyncRepository.java` | Repository | Failed sync queries (unresolved, by jobId) |
| `service/JobGeoService.java` | Interface | Redis geo operations contract |
| `service/impl/RedisJobGeoService.java` | Implementation | `GEOADD`, `GEORADIUS`, `ZREM` via Spring Data Redis |
| `service/impl/NoOpJobGeoService.java` | NoOp fallback | Used when Redis is unavailable |
| `service/JobGeoSyncService.java` | Interface | Sync operations |

---

## Service Methods

### `JobGeoService`

| Method | Description |
|--------|-------------|
| `addOrUpdateOpenJob(jobId, lat, lon)` | `GEOADD geo:jobs:open lon lat jobId` |
| `removeJob(jobId)` | `ZREM geo:jobs:open jobId` |
| `findNearbyOpenJobs(lat, lon, radiusKm, limit)` | `GEORADIUS geo:jobs:open lon lat radiusKm km ASC COUNT limit` |

### `JobGeoSyncService`

| Method | Description |
|--------|-------------|
| `syncOpenJobsToGeoIndex()` | Startup: loads all `OPEN_FOR_BIDS` jobs from PG → `GEOADD` each |
| `cleanupStaleGeoEntries()` | Daily: cross-references Redis members with PG → removes stale entries |
| `retryFailedGeoSyncs()` | Every 5 min: retries unresolved `FailedGeoSync` records (max 5 attempts) |

---

## Sync Mechanisms

| Mechanism | Trigger | Description |
|-----------|---------|-------------|
| Startup sync | `ApplicationReadyEvent` | Full re-index from PG |
| Daily cleanup | `@Scheduled` (02:00 AM) | Remove orphaned Redis entries |
| Failed sync retry | `@Scheduled` (every 5 min) | Re-attempt failed ADD/REMOVE operations |
| Event-driven eviction | `@TransactionalEventListener(AFTER_COMMIT)` | Evict caches on job mutations |

---

## Failed Geo Sync Entity

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT (PK) | Auto-generated |
| `job_id` | BIGINT | The affected job |
| `operation` | ENUM(`ADD`, `REMOVE`, `UPDATE`) | What failed |
| `latitude` | DOUBLE | Coordinates for ADD/UPDATE |
| `longitude` | DOUBLE | Coordinates for ADD/UPDATE |
| `retry_count` | INT | Attempts so far (max 5) |
| `last_error` | VARCHAR(1000) | Last failure message |
| `created_at` | TIMESTAMP | When the failure occurred |
| `resolved` | BOOLEAN | Whether it's been fixed |

---

## NoOp Fallback

When Redis is unavailable, `NoOpJobGeoService` is activated:
- `addOrUpdateOpenJob()` → no-op
- `removeJob()` → no-op
- `findNearbyOpenJobs()` → returns empty list (triggers skill-based fallback in `getWorkerFeed()`)

---

## Related Documentation

For the full geo-search architecture, data flow, performance tuning, and debugging commands, see [GEOSEARCH.md](../GEOSEARCH.md).

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| Spring Data Redis | `GeoOperations` for `GEOADD`, `GEORADIUS`, `ZREM` |
| `FailedGeoSyncRepository` | Failed sync persistence |
| `JobRepository` | Source of truth for job data |
