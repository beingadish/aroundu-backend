# FX Service (Exchange Rate)

> Live currency exchange rates via the Frankfurter API with in-memory caching.

---

## Overview

The FX module provides real-time currency conversion for displaying job prices in the user's preferred currency. It fetches live rates from the free [Frankfurter API](https://api.frankfurter.app) and caches them in-memory for 1 hour.

**Package:** `com.beingadish.AroundU.fx`

---

## File Inventory

| File | Type | Description |
|------|------|-------------|
| `service/impl/ExchangeRateService.java` | Service | Rate fetching with caching |
| `controller/ExchangeRateController.java` | Controller | REST endpoint |
| `dto/ExchangeRateDTO.java` | DTO | `from`, `to`, `rate` |

---

## Service Methods

### `ExchangeRateService`

| Method | Signature | Description |
|--------|-----------|-------------|
| `getRate` | `ExchangeRateDTO getRate(String from, String to)` | Returns exchange rate; 1.0 if same currency; `null` rate on API failure |

---

## REST Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/v1/fx/rate?from=USD&to=INR` | Authenticated | Get exchange rate between two currencies |

---

## Caching

| Property | Value |
|----------|-------|
| Cache type | `ConcurrentHashMap<String, CachedRate>` (in-memory) |
| Cache TTL | 1 hour (3600 seconds) |
| Cache key | `FROM_TO` (e.g., `USD_INR`) |
| Same currency | Returns 1.0 immediately (no API call) |
| Null inputs | Returns rate of 1.0 |

---

## External API

| Property | Value |
|----------|-------|
| Provider | [Frankfurter API](https://api.frankfurter.app) |
| Auth | None (free, no API key) |
| Endpoint | `GET /latest?from={from}&to={to}` |
| Error handling | Returns `null` rate on failure (graceful degradation) |
| HTTP client | Spring `RestClient` |

---

## Response Example

```json
{
  "from": "USD",
  "to": "INR",
  "rate": 83.42
}
```
