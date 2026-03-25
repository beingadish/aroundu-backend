# Auth Service

> Authentication and JWT token management for the AroundU platform.

---

## Overview

The Auth module handles user login across all three roles (Client, Worker, Admin). It authenticates credentials via Spring Security's `AuthenticationManager`, generates JWT tokens via `JwtTokenProvider`, and resolves the user's role-specific ID from the corresponding repository.

**Package:** `com.beingadish.AroundU.user`

---

## File Inventory

| File | Type | Description |
|------|------|-------------|
| `service/AuthService.java` | Interface | Login contract |
| `service/impl/AuthServiceImpl.java` | Implementation | Credential validation, JWT generation, role-based ID resolution |
| `controller/AuthController.java` | REST Controller | Login endpoint |
| `dto/auth/LoginRequestDTO.java` | DTO | Request body: `email`, `password` |
| `dto/auth/LoginResponseDTO.java` | DTO | Response: `userId`, `token`, `tokenType`, `email`, `role` |

---

## Service Methods

### `AuthService`

| Method | Signature | Description |
|--------|-----------|-------------|
| `authenticate` | `LoginResponseDTO authenticate(LoginRequestDTO loginRequest)` | Validates credentials, generates JWT, returns session payload |

### Internal Methods (`AuthServiceImpl`)

| Method | Description |
|--------|-------------|
| `resolveUserId(email, role)` | Looks up the user ID in the role-specific repository (`ClientReadRepository`, `WorkerReadRepository`, or `AdminRepository`). Falls back to trying all three if the role is unknown. |

---

## REST Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/v1/auth/login` | Public | Authenticates user and returns JWT token |

### Request Body

```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

### Response

```json
{
  "userId": 1,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "email": "user@example.com",
  "role": "ROLE_CLIENT"
}
```

---

## Authentication Flow

```
LoginRequest → AuthenticationManager.authenticate()
     │
     ├─ Success → extract role from GrantedAuthority
     │            → resolveUserId(email, role)
     │            → JwtTokenProvider.generateToken(userId, email, role)
     │            → return LoginResponseDTO
     │
     └─ Failure → Spring Security throws AuthenticationException
                 → GlobalExceptionHandler returns 401
```

---

## Security Details

- **Password encoding:** BCrypt (handled by Spring Security config)
- **JWT contents:** userId, email, role
- **Token provider:** `JwtTokenProvider` (in `infrastructure/security/`)
- **Role mapping:** `ROLE_CLIENT`, `ROLE_WORKER`, `ROLE_ADMIN`

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `JwtTokenProvider` | JWT generation |
| `AuthenticationManager` | Spring Security credential validation |
| `ClientReadRepository` | Client ID resolution |
| `WorkerReadRepository` | Worker ID resolution |
| `AdminRepository` | Admin ID resolution |
