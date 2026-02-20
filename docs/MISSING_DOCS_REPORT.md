# AroundU API — Documentation Coverage Report

> Auto-generated documentation audit — last updated: 2025

---

## Summary

| Metric                         | Value              |
| ------------------------------ | ------------------ |
| Total Controllers              | 7                  |
| Controllers Already Documented | 4                  |
| Controllers Newly Documented   | 3                  |
| Total REST Endpoints           | 30+                |
| Undocumented Endpoints         | **0**              |
| Swagger Profiles Enabled       | dev, test, preprod |
| Swagger Disabled In            | prod               |

**Result: 100% endpoint documentation coverage.**

---

## Controller Audit

### Already Fully Annotated (No Changes Needed)

| Controller         | Tag     | Endpoints | Annotations Present                                               |
| ------------------ | ------- | --------- | ----------------------------------------------------------------- |
| `AuthController`   | Auth    | 1         | @Tag, @Operation, @ApiResponses, @SecurityRequirement             |
| `ClientController` | Clients | 6         | @Tag, @Operation, @ApiResponses, @SecurityRequirement, @Parameter |
| `WorkerController` | Workers | 6         | @Tag, @Operation, @ApiResponses, @SecurityRequirement, @Parameter |
| `JobController`    | Jobs    | 11        | @Tag, @Operation, @ApiResponses, @SecurityRequirement, @Parameter |

### Newly Annotated (Changes Applied)

| Controller          | Tag       | Endpoints | What Was Added                                                                              |
| ------------------- | --------- | --------- | ------------------------------------------------------------------------------------------- |
| `BidController`     | Bids      | 4         | @Tag, @Operation, @ApiResponses (201/400/404/429/403/409), @SecurityRequirement, @Parameter |
| `PaymentController` | Payments  | 2         | @Tag, @Operation, @ApiResponses (201/200/400/404/403/409), @SecurityRequirement, @Parameter |
| `JobCodeController` | Job Codes | 3         | @Tag, @Operation, @ApiResponses (200/400/404/403/409), @SecurityRequirement, @Parameter     |

---

## Annotations Added — Details

### BidController (`src/main/java/.../bid/controller/BidController.java`)

| Endpoint                       | Method           | Added                                                  |
| ------------------------------ | ---------------- | ------------------------------------------------------ |
| `POST /jobs/{jobId}/bids`      | Place bid        | @Operation, @ApiResponses(201,400,404,429), @Parameter |
| `GET /jobs/{jobId}/bids`       | List bids        | @Operation, @ApiResponses(200,404,403), @Parameter     |
| `POST /bids/{bidId}/accept`    | Accept bid       | @Operation, @ApiResponses(200,404,403,409), @Parameter |
| `POST /bids/{bidId}/handshake` | Worker handshake | @Operation, @ApiResponses(200,400,404,403), @Parameter |

### PaymentController (`src/main/java/.../payment/controller/PaymentController.java`)

| Endpoint                         | Method         | Added                                                      |
| -------------------------------- | -------------- | ---------------------------------------------------------- |
| `POST /{jobId}/payments/lock`    | Lock escrow    | @Operation, @ApiResponses(201,400,404,403,409), @Parameter |
| `POST /{jobId}/payments/release` | Release escrow | @Operation, @ApiResponses(200,400,404,403,409), @Parameter |

### JobCodeController (`src/main/java/.../job/controller/JobCodeController.java`)

| Endpoint                      | Method         | Added                                                      |
| ----------------------------- | -------------- | ---------------------------------------------------------- |
| `POST /{jobId}/codes`         | Generate codes | @Operation, @ApiResponses(200,404,403), @Parameter         |
| `POST /{jobId}/codes/start`   | Verify start   | @Operation, @ApiResponses(200,400,404,403,409), @Parameter |
| `POST /{jobId}/codes/release` | Verify release | @Operation, @ApiResponses(200,400,404,403,409), @Parameter |

---

## Configuration Changes

### `OpenApiConfig.java`

- Enhanced API description with sections: Authentication, Rate Limiting, Pagination, Sorting
- Added server URLs: localhost:20232 (dev), preprod.aroundu.com (preprod)
- Added contact info

### `application-prod.yml`

- Added explicit `springdoc.api-docs.enabled: false`
- Added explicit `springdoc.swagger-ui.enabled: false`
- Belt-and-suspenders with `@Profile({"dev", "test", "preprod"})` on `OpenApiConfig`

---

## Generated Artifacts

| Artifact                     | Path                                        | Description                                                         |
| ---------------------------- | ------------------------------------------- | ------------------------------------------------------------------- |
| OpenAPI 3.0 YAML             | `docs/openapi.yaml`                         | Full spec with all 30+ endpoints, 26 schemas, security schemes      |
| IntelliJ HTTP — User         | `docs/http/user.http`                       | Auth, Client, Worker endpoints (register, CRUD, pagination, errors) |
| IntelliJ HTTP — Job          | `docs/http/job.http`                        | Create, update, status, search, client jobs, worker feed, codes     |
| IntelliJ HTTP — Bid          | `docs/http/bid.http`                        | Place, list, accept, handshake (success + error scenarios)          |
| IntelliJ HTTP — Payment      | `docs/http/payment.http`                    | Lock escrow, release escrow (success + error scenarios)             |
| IntelliJ HTTP — Notification | `docs/http/notification.http`               | Event-driven triggers (no direct REST endpoints)                    |
| Postman Collection           | `docs/AroundU-API.postman_collection.json`  | 7 folders, env vars, pre-request auth, sample responses             |
| Postman Environment          | `docs/AroundU-DEV.postman_environment.json` | Dev environment variables                                           |

---

## Rate Limit Coverage in Test Files

| Endpoint      | Documented Limit | Covered In  |
| ------------- | ---------------- | ----------- |
| Auth login    | 5 req / 15 min   | `user.http` |
| Job creation  | 5 req / hr       | `job.http`  |
| Worker feed   | 30 req / min     | `job.http`  |
| Bid placement | 20 req / hr      | `bid.http`  |
| Profile views | 100 req / hr     | `user.http` |

---

## Remaining Notes

- **No undocumented endpoints found.** All 7 controllers, all 30+ endpoints are covered.
- Notification service (`NotificationKafkaProducer`) is event-driven with no REST endpoints — covered via trigger scenarios in `.http` files and Postman.
- Swagger UI accessible at `/docs` on dev/test/preprod profiles only.
- API docs JSON at `/api-docs`.
