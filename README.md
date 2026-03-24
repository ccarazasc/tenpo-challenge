# Tenpo Challenge — REST API (Spring Boot 3 + WebFlux)

[![Java](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen)](https://spring.io/projects/spring-boot)
[![WebFlux](https://img.shields.io/badge/WebFlux-Reactive-green)](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue)](https://docs.docker.com/compose/)

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Technical Decisions](#technical-decisions)
- [Project Structure](#project-structure)
- [Running Locally](#running-locally)
- [API Reference](#api-reference)
- [Running Tests](#running-tests)
- [Scalability](#scalability)

---

## Overview

A production-grade REST API that demonstrates:

| Requirement | Implementation |
|---|---|
| Calculation with dynamic external percentage | `GET /api/v1/calculate?num1=X&num2=Y` |
| 30-minute percentage cache + fallback | Redis `SET`/`GET` with configurable TTL |
| 3-attempt retry on external service failure | Reactor `retryWhen(Retry.fixedDelay(2, 500ms))` |
| Async call history (paginated) | PostgreSQL via R2DBC + async fire-and-forget write |
| 3 RPM rate limiting | Redis fixed-window counter in a `WebFilter` |
| Structured error responses (4XX / 5XX) | `@RestControllerAdvice` with a consistent JSON envelope |
| Reactive programming (bonus) | Spring WebFlux throughout, zero blocking calls |

---

## Architecture

The project follows **Hexagonal Architecture** (also known as Ports & Adapters):

```
                   ┌─────────────────────────────────────────┐
  HTTP Clients     │              Infrastructure              │
  ─────────────► ──┤  Web (Controllers, Filters, DTOs)       │
                   │                  │                       │
                   │       ┌──────────▼────────────┐         │
                   │       │    Application Layer   │         │
                   │       │  (Use Case Services)   │         │
                   │       └──────────┬────────────┘         │
                   │                  │  Ports (interfaces)   │
                   │       ┌──────────▼────────────┐         │
                   │       │     Domain Layer       │         │
                   │       │  (Models, Port defs)   │         │
                   │       └──────────┬────────────┘         │
                   │                  │                       │
                   │  ┌───────────────┼──────────────────┐   │
                   │  │  Out-Adapters │                   │   │
                   │  │  ┌───────────┤  ┌────────────┐   │   │
                   │  │  │ PostgreSQL│  │   Redis     │   │   │
                   │  │  │ (R2DBC)   │  │  (Reactive) │   │   │
                   │  │  └───────────┘  └────────────┘   │   │
                   │  │  ┌────────────────────────────┐   │   │
                   │  │  │  External % Service (mock) │   │   │
                   │  │  └────────────────────────────┘   │   │
                   └─────────────────────────────────────────┘
```

### Layers

| Layer | Responsibility | Framework dependency |
|---|---|---|
| **Domain** | Models (`ApiCall`, `Page`) and Port interfaces | None |
| **Application** | Use-case orchestration (`CalculationApplicationService`, `ApiCallHistoryApplicationService`) | None |
| **Infrastructure** | Spring Web, R2DBC, Redis, WebClient adapters, Filters, Config | Spring Boot |

---

## Technical Decisions

### 1. Spring WebFlux (Reactive) — Bonus ✅
The entire stack is reactive end-to-end:
- **WebFlux** instead of Spring MVC → non-blocking I/O, better throughput under concurrent load.
- **Spring Data R2DBC** → reactive PostgreSQL client, no thread-per-request blocking.
- **Reactive Redis** (`ReactiveStringRedisTemplate`) → cache and rate limiter are non-blocking.
- **WebClient** → non-blocking HTTP calls to the external percentage service.

### 2. Hexagonal Architecture
Chosen for testability and adaptability:
- The domain and application layers have **zero framework dependencies** — they can be unit-tested without Spring context.
- Swapping PostgreSQL → another DB, or Redis → in-memory cache, only touches the infrastructure adapters.
- Ports (interfaces) in the domain enforce the dependency inversion principle.

### 3. Distributed Redis Cache (Scalability)
> _"Se pidio 'Diseña la aplicación para que pueda ejecutarse en un entorno con múltiples réplicas.'"_

Both the **percentage cache** and the **rate limiter** live in Redis, not in-process memory.
This means any number of replicas share the same state automatically. No additional coordination needed.

### 4. Rate Limiting Strategy
A **fixed-window counter** per minute (`tenpo:rate_limit:<epoch_minute>`) in Redis:
- Atomic `INCR` + TTL-based expiry.
- Works correctly across replicas.
- Returns **HTTP 429** with a descriptive JSON body when the limit is exceeded.
- Infra/docs paths (Swagger, Actuator) are excluded.

### 5. Retry Logic
`reactor.util.retry.Retry.fixedDelay(2, 500ms)`:
- 2 retries = **3 total attempts**, matching the requirement.
- Only 5xx / transient errors are retried; 4xx errors surface immediately.
- After exhaustion, the application falls back to the Redis cache.

### 6. Async Call Logging
`ApiCallLoggingFilter` uses `doFinally` + `subscribeOn(Schedulers.boundedElastic())`:
- History write is fire-and-forget — it does **not** add to the response latency.
- Errors in the write are logged but **never propagate** to the caller.
- The history endpoint itself is excluded from logging to prevent infinite recursion.

### 7. Flyway with R2DBC
Flyway only speaks JDBC. A thin JDBC `DataSource` is configured solely for migrations (`FlywayConfig`). The PostgreSQL JDBC driver is on the classpath only for this purpose; all runtime queries use R2DBC.

---

## Project Structure

```
src/main/java/com/tenpo/challenge/
├── TenpoChallengeApplication.java
├── domain/
│   ├── model/
│   │   ├── ApiCall.java          # Immutable domain record
│   │   └── Page.java             # Framework-agnostic pagination
│   └── port/
│       ├── in/
│       │   ├── CalculationUseCase.java
│       │   └── ApiCallHistoryUseCase.java
│       └── out/
│           ├── ApiCallRepositoryPort.java
│           ├── PercentageServicePort.java
│           └── PercentageCachePort.java
├── application/
│   └── service/
│       ├── CalculationApplicationService.java
│       └── ApiCallHistoryApplicationService.java
└── infrastructure/
    ├── adapter/
    │   ├── in/web/
    │   │   ├── CalculationController.java
    │   │   ├── ApiCallHistoryController.java
    │   │   ├── MockPercentageController.java
    │   │   └── dto/
    │   │       ├── CalculationResponse.java
    │   │       ├── ApiCallResponse.java
    │   │       ├── PagedResponse.java
    │   │       └── ErrorResponse.java
    │   └── out/
    │       ├── persistence/
    │       │   ├── ApiCallRepositoryAdapter.java
    │       │   ├── entity/ApiCallEntity.java
    │       │   └── repository/R2dbcApiCallRepository.java
    │       ├── external/
    │       │   └── PercentageServiceAdapter.java
    │       └── cache/
    │           └── RedisPercentageCacheAdapter.java
    ├── filter/
    │   ├── RateLimiterFilter.java
    │   └── ApiCallLoggingFilter.java
    ├── exception/
    │   ├── PercentageServiceUnavailableException.java
    │   ├── RateLimitExceededException.java
    │   ├── ExternalServiceException.java
    │   └── handler/GlobalExceptionHandler.java
    └── config/
        ├── AppConfig.java
        ├── FlywayConfig.java
        └── OpenApiConfig.java
```

---

## Running Locally

### Prerequisites
- Docker + Docker Compose v2

### Option A — Docker Compose (recommended)

```bash
# Clone the repo and enter the directory
git clone <repo-url> && cd tenpo-challenge

# (Optional) copy and customise environment variables
cp .env.example .env

# Build and start all services
docker compose up --build
```

The API is available at **http://localhost:8080**.

### Option B — Maven (local JDK 21 required)

```bash
# Start infrastructure only
docker compose up postgres redis -d

# Build and run
./mvnw spring-boot:run
```

### Simulate external service failures (test retry + cache)

```bash
# Enable mock failures (every 2nd call fails) and restart
MOCK_FAILURE_ENABLED=true docker compose up --build
```

---

## API Reference

Interactive Swagger UI: **http://localhost:8080/swagger-ui.html**

### `GET /api/v1/calculate`

Calculates `(num1 + num2) + percentage%` where the percentage is fetched from the mock external service.

| Query Param | Type | Required | Description |
|---|---|---|---|
| `num1` | `double` | ✅ | First number |
| `num2` | `double` | ✅ | Second number |

**Example:**
```bash
curl "http://localhost:8080/api/v1/calculate?num1=5&num2=5"
```
```json
{"num1": 5.0, "num2": 5.0, "result": 11.0}
```

**Error responses:**

| Status | Cause |
|---|---|
| `400` | Missing or invalid parameters |
| `429` | Rate limit exceeded (> 3 RPM) |
| `503` | External service unavailable and no cached value |

---

### `GET /api/v1/history`

Returns a paginated list of all API calls made to the service.

| Query Param | Type | Default | Description |
|---|---|---|---|
| `page` | `int` | `0` | Zero-based page index |
| `size` | `int` | `10` | Page size |

**Example:**
```bash
curl "http://localhost:8080/api/v1/history?page=0&size=5"
```
```json
{
  "content": [
    {
      "id": 1,
      "endpoint": "/api/v1/calculate",
      "parameters": "{num1=[5.0], num2=[5.0]}",
      "response": "{\"num1\":5.0,\"num2\":5.0,\"result\":11.0}",
      "success": true,
      "timestamp": "2026-03-22T12:00:00"
    }
  ],
  "page": 0,
  "size": 5,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

### `GET /api/v1/mock/percentage`

Simulated external percentage provider. Returns the configured percentage (default 10%).

```bash
curl "http://localhost:8080/api/v1/mock/percentage"
# → {"percentage": 10.0}
```

---

## Running Tests

```bash
./mvnw test
```

Test coverage includes:

| Test Class | What it covers |
|---|---|
| `CalculationApplicationServiceTest` | Happy path, cache fallback, no-cache 503, cache write failure, zero/100% |
| `CalculationControllerTest` | 200 success, 503 propagation, 400 missing param |
| `ApiCallHistoryControllerTest` | Paginated response, empty history |
| `PercentageServiceAdapterTest` | Successful call, 3-attempt retry, retry success, 4xx not retried |
| `RateLimiterFilterTest` | Allows within limit, blocks on 4th request, boundary (3rd allowed) |
| `RedisPercentageCacheAdapterTest` | Cache hit, cache miss, save with correct TTL |

---

## Scalability

To run multiple replicas:

```bash
docker compose up --build --scale api=3
```

Both Redis-backed components handle multi-replica correctly:

- **Rate limiter**: All replicas share the same Redis key → the global 3 RPM limit is enforced even across replicas.
- **Percentage cache**: Any replica can read/write the same key → no duplicate external calls.
- **PostgreSQL R2DBC pool**: Each replica maintains its own connection pool; the database serialises writes.
- **Stateless application**: No in-process state, so replicas can be added or removed without coordination.

For production, a reverse proxy (NGINX, AWS ALB) in front of the replicas handles load balancing.
