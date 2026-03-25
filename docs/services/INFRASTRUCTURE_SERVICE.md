# Infrastructure Services

> Caching, locking, metrics, rate limiting, scheduling, security, storage, health checks, and WebSocket configuration.

---

## Overview

The Infrastructure module provides cross-cutting platform services used by all business modules. It follows a consistent pattern of interface + implementation + NoOp fallback for graceful degradation when optional infrastructure (Redis, external services) is unavailable.

**Package:** `com.beingadish.AroundU.infrastructure`

---

## Submodules

### Cache (`infrastructure/cache/`)

| File | Description |
|------|-------------|
| `CacheEvictionService.java` | Interface: evict job, client list, worker feed, and profile caches |
| `impl/RedisCacheEvictionService.java` | Redis-backed cache eviction with pattern-based key deletion |
| `impl/NoOpCacheEvictionService.java` | NoOp fallback when Redis is unavailable |
| `CacheStatisticsService.java` | Cache hit/miss statistics |
| `CacheWarmupService.java` | Pre-loads frequently accessed data on startup |

**Cache Regions:**

| Region | TTL | Eviction Trigger |
|--------|-----|-------------------|
| `job:detail` | 30 min | Any mutation on that job |
| `job:client:list` | 10 min | Mutation by that client |
| `job:worker:feed` | 5 min | CREATE, DELETE, STATUS_CHANGE, or location update |
| `user:profile` | 1 hour | Profile updates |
| `worker:skills` | 6 hours | Skill changes |

---

### Lock (`infrastructure/lock/`)

| File | Description |
|------|-------------|
| `LockService.java` | Interface: distributed lock acquire/release |
| `LockServiceBase.java` | Base implementation (Redisson-backed) |
| `NoOpLockService.java` | NoOp fallback: always returns lock success |

Used by: Bid duplicate check, payment processing (single-threaded safety).

---

### Metrics (`infrastructure/metrics/`)

| File | Description |
|------|-------------|
| `MetricsService.java` | Central metrics registry — counters, timers, gauges for all business operations |
| `ResilienceMonitoringService.java` | Resilience4j circuit breaker and retry metrics |
| `SchedulerMetricsService.java` | Scheduler execution counts and timing |

See [METRICS.md](../METRICS.md) for the complete metrics catalog.

---

### Rate Limiting (`infrastructure/ratelimit/`)

| File | Description |
|------|-------------|
| `RateLimit.java` | `@RateLimit` annotation: `capacity`, `refillTokens`, `refillMinutes` |
| `RateLimitAspect.java` | AOP aspect: applies token-bucket rate limiting per IP/user |
| `RateLimitConfig.java` | Configuration for rate limit stores |

**Active Rate Limits:**

| Endpoint | Limit |
|----------|-------|
| Auth login | 5 / 15 min |
| Job creation | 5 / hr |
| Worker feed | 30 / min |
| Bid placement | 20 / hr |
| Profile views | 100 / hr |

---

### Security (`infrastructure/security/`)

| File | Description |
|------|-------------|
| `SecurityConfig.java` | Spring Security configuration, route-level auth rules, CORS, JWT filter chain |
| `JwtTokenProvider.java` | JWT token generation and validation |
| `JwtAuthenticationFilter.java` | Extracts JWT from `Authorization` header, authenticates request |
| `CustomUserDetailsService.java` | Loads user details for Spring Security from Client/Worker/Admin repositories |
| `UserPrincipal.java` | Custom `UserDetails` implementation with userId |
| `CustomAccessDeniedHandler.java` | JSON 403 response |
| `CustomAuthenticationEntryPoint.java` | JSON 401 response |
| `PayloadCryptoService.java` | AES-256 payload encryption/decryption for sensitive data |
| `CachedBodyHttpServletRequest.java` | Request body caching for logging |

---

### Schedulers (`infrastructure/scheduler/`)

| File | Schedule | Action |
|------|----------|--------|
| `JobExpirationScheduler.java` | Configurable | Expires jobs past their TTL → `JOB_CLOSED_DUE_TO_EXPIRATION` |
| `WorkerPenaltyScheduler.java` | Periodic | Unblocks workers whose penalty period has expired |
| `EscrowSettlementScheduler.java` | Periodic | Settles pending escrow transactions |
| `ChatCleanupScheduler.java` | Daily 3 AM | Archives/deletes expired conversations |
| `CacheSyncScheduler.java` | Every 5 min | Retries failed geo syncs, warms caches |
| `AnalyticsScheduler.java` | Periodic | Aggregates metrics for analytics |
| `ReminderScheduler.java` | Periodic | Sends reminders for pending actions |
| `UserCleanupScheduler.java` | Periodic | Cleanup inactive/deleted user data |

---

### Storage (`infrastructure/storage/`)

| File | Description |
|------|-------------|
| `ImageStorageService.java` | Profile image upload/delete with cloud storage integration |

---

### Interceptors (`infrastructure/interceptor/`)

| File | Description |
|------|-------------|
| `ApiVersionInterceptor.java` | Validates API version headers |
| `RequestIdInterceptor.java` | Generates unique request IDs for tracing |
| `RequestLoggingInterceptor.java` | Logs request/response metadata |
| `UserContextInterceptor.java` | Extracts user context for downstream services |

---

### Health (`infrastructure/health/`)

| File | Description |
|------|-------------|
| `RedisHealthIndicator.java` | Redis `PING` check |
| `DatabaseHealthIndicator.java` | JDBC `connection.isValid(2)` via HikariCP |
| `PaymentGatewayHealthIndicator.java` | External payment provider reachability |

---

### Configuration (`infrastructure/config/`)

| File | Description |
|------|-------------|
| `OpenApiConfig.java` | Swagger/OpenAPI configuration (dev/test/preprod only) |
| `RedisConfig.java` | Redis connection and serialization |
| `RedissonConfig.java` | Redisson distributed lock configuration |
| `ResilienceConfig.java` | Resilience4j circuit breaker and retry beans |
| `ResilienceProperties.java` | Configurable resilience parameters |
| `SchedulerProperties.java` | Configurable scheduler intervals |
| `SchedulingConfig.java` | `@EnableScheduling` configuration |
| `SecurityConfig.java` | Spring Security filter chain |
| `ThreadPoolConfig.java` | Async thread pool configuration |
| `WebMvcConfig.java` | MVC interceptor registration |
| `WebSocketConfig.java` | STOMP WebSocket endpoint configuration |
| `WebSocketAuthInterceptor.java` | JWT auth for WebSocket connections |
| `AdminSeeder.java` | Seeds default admin account on first startup |
| `ClockConfig.java` | `Clock` bean for testable time operations |
| `ProfileConfig.java` | Profile-specific bean configuration |

---

### Analytics (`infrastructure/analytics/`)

| File | Description |
|------|-------------|
| `entity/AggregatedMetrics.java` | Aggregated metric snapshots |
| `repository/AggregatedMetricsRepository.java` | Aggregated metrics persistence |
