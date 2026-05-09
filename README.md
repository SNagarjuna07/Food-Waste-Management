![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green)
![Build](https://img.shields.io/badge/Build-Maven-success)
![Security](https://img.shields.io/badge/Security-JWT-red)
![License](https://img.shields.io/badge/License-MIT-yellow)

# 🍽️ Food Waste Management Backend

A **production-ready Spring Boot backend** for managing food waste donation workflows — built from scratch as a solo project.

Not a tutorial app. This was designed with real system constraints in mind: What happens when Redis goes down mid-request? How do you prevent rate-limit decisions from leaking into Spring Security's exception model? How do you keep Kafka failures from breaking the user flow?

> **Stack:** Java 21 · Spring Boot · Spring Security · Apache Kafka · Redis · MongoDB · JWT · OAuth2 · Maven

---

## 📐 System Architecture

```
Client Request
      │
      ▼
┌─────────────────────────────┐
│  PreAuthRateLimitingFilter  │  ← Token Bucket (Redis) — runs BEFORE Spring Security
│  Fail-open on Redis failure │    Returns 429 with X-RateLimit headers
└────────────┬────────────────┘
             │
             ▼
┌─────────────────────────────┐
│   JwtAuthenticationFilter  │  ← Validates JWT, populates SecurityContext
│   (Spring Security Chain)  │
└────────────┬────────────────┘
             │
             ▼
┌─────────────────────────────┐
│  PostAuthRateLimitingFilter │  ← Re-checks rate limit post-auth (USER identity)
└────────────┬────────────────┘
             │
             ▼
┌─────────────────────────────┐
│     Controllers / Services  │
│     MongoDB · Redis Cache   │
│     Kafka Notification Bus  │
└─────────────────────────────┘
```

---

## 🔐 Security

**JWT + Google OAuth2 — Stateless, No Server Sessions**

- JWT tokens signed with HMAC-SHA, 1-hour expiry
- Google OAuth2 flow: authorization code → access token → user profile → local JWT
- Account takeover prevention: email registered via local auth cannot be accessed via Google OAuth
- BCrypt password hashing
- Method-level security with `@PreAuthorize`
- Custom `401` / `403` handlers — no Spring default error pages

**Filter Chain Order (matters):**

```
PreAuthRateLimitingFilter → JwtAuthenticationFilter → PostAuthRateLimitingFilter → Controller
```

Rate limiting sits outside Spring Security intentionally. A `429` response from inside Spring Security would be misinterpreted as a `403`. The pre-auth filter intercepts the request before any Spring exception handling kicks in.

---

## ⚡ Rate Limiting (Token Bucket — Redis-Backed)

Four profiles, configurable per endpoint via `application.properties`:

| Profile | Use Case | Identity |
|---------|----------|----------|
| `AUTH` | Login, register, OAuth | IP address |
| `READ` | GET endpoints | Authenticated user |
| `WRITE` | POST/PUT endpoints | Authenticated user |
| `UNLIMITED` | Swagger, actuator | — |

**Key design decisions:**

- **Fail-open:** If Redis is unavailable, traffic passes through. Never block users because of infra failure.
- **TTL-safe updates:** When saving an updated bucket back to Redis, the existing TTL is preserved. Prevents buckets from becoming immortal.
- **Pre-auth vs post-auth:** IP-based limits (login brute-force) run before auth. User-based limits run after auth, once identity is known.
- **X-RateLimit headers on every response:** `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`

---

## 📨 Kafka — Async Notification Pipeline

Kafka is used for async notification delivery on key user events:

| Event | Kafka Topic |
|-------|-------------|
| Email verified | `app-notification-events` |
| Password reset requested | `app-notification-events` |
| Password reset completed | `app-notification-events` |
| Donation collected | `app-notification-events` |

**Producer behavior:** If Kafka is unavailable, the exception is caught and logged as a warning. The user-facing action (e.g. collecting a donation) is not rolled back. Kafka failure is degraded gracefully — not treated as a system error.

**Consumer behavior:** Manual acknowledgment (`ack-mode=manual`). Messages are only acknowledged after successful processing. Failed deserialization is logged but does not crash the consumer.

---

## 🗄️ MongoDB Design

**Collections:**

| Collection | Compound Indexes |
|---|---|
| `users` | unique: `username`, `email` |
| `food_donors` | `{ createdBy: 1, createdAt: -1 }` |
| `food_donations` | `{ createdAt: -1 }` |
| `collection_centers` | `{ active: 1, createdAt: -1 }` |
| `notifications` | `{ userId: 1, read: 1 }`, `{ userId: 1, createdAt: -1 }` |
| `email_verification_tokens` | unique: `token`, indexed: `expiryTime` |
| `password_reset_tokens` | unique: `token`, indexed: `expiryTime` |

**Paginated queries** on all list endpoints via Spring Data's `Pageable` with a custom `PageResponse<T>` wrapper.

**Custom repository layer** for bulk updates (e.g. `markAllAsRead` uses `MongoTemplate.updateMulti` — skips loading all documents into memory).

---

## 🧰 Redis Cache Strategy

Cache-aside pattern with TTL-based invalidation:

| Cache | TTL | Invalidated On |
|---|---|---|
| `collectionCenters` | 5 min | Create / Update / Delete center |
| `activeCollectionCenters` | 2 min | Create / Update / Delete center |
| `collectionCenterById` | 10 min | Update / Delete that center |
| `foodDonorsByUser` | 2 min | Create / Update / Delete donor |
| `foodDonorIdsByUser` | 10 min | Create / Update / Delete donor |
| `userNotifications` | 30 sec | New notification, mark as read |
| `userUnreadNotificationCount` | 15 sec | New notification, mark as read |
| `userByUsername` | 5 min | Update / Delete user |

**Fail-open cache error handler:** All cache errors (`GET`, `PUT`, `EVICT`, `CLEAR`) are caught and logged — never thrown to the caller. A cache miss just falls through to the DB.

**Redis downtime at startup:** The `CacheManager` performs a health check at startup. If Redis is unreachable, it falls back to `NoOpCacheManager` — application starts normally.

---

## 🔄 Scheduled Cleanup Jobs

| Job | Schedule | What it does |
|---|---|---|
| Email token cleanup | Sundays 18:00 | Deletes expired or used email verification tokens |
| Password reset token cleanup | Sundays 19:00 | Deletes expired or used reset tokens |
| Notification cleanup | Daily 12:00 | Deletes read notifications older than 10 days |

---

## 📡 API Overview

| Module | Endpoints |
|---|---|
| **Auth** | `POST /auth/register`, `POST /auth/login`, `GET /auth/verify-email`, `POST /auth/forgot-password`, `POST /auth/reset-password` |
| **Google OAuth** | `GET /auth/google/callback` |
| **Donors** | `POST /donors`, `GET /donors`, `PATCH /donors/{id}`, `DELETE /donors/{id}` |
| **Donations** | `POST /donations`, `GET /donations/{id}`, `GET /donations/my-donations`, `PATCH /donations/{id}`, `DELETE /donations/{id}`, `POST /donations/{id}/collect` |
| **Collection Centers** | `POST /collection-centers`, `GET /collection-centers`, `GET /collection-centers/active`, `GET /collection-centers/{id}`, `PATCH /collection-centers/{id}`, `DELETE /collection-centers/{id}` |
| **Notifications** | `GET /notifications`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all` |

Full interactive docs available via Swagger UI at `/swagger-ui/index.html`.

---

## ⚙️ Local Setup

### Prerequisites

- Java 21
- Maven
- MongoDB (local or Atlas)
- Redis
- Apache Kafka + Zookeeper
- Gmail account (for email services)
- Google OAuth2 credentials

### Environment Variables

Create a `.env` file in the project root (never committed):

```env
MONGO_URI=mongodb://localhost:27017
DB_NAME=foodwaste_db
ACTIVE_PROFILE=dev

JWT_KEY=your-256-bit-secret-key

MAIL_PORT=587
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your-app-password

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_TIMEOUT=2000ms

OAUTH2_CLIENT_ID=your-google-client-id
OAUTH2_CLIENT_SECRET=your-google-client-secret
OAUTH2_REDIRECT_URI=http://localhost:8080/auth/google/callback

KAFKA_TOPIC_GROUP_ID=app-notification-group

# Rate limit profiles
AUTH_CAPACITY=10
AUTH_REFILL=60
AUTH_TTL=3600
READ_CAPACITY=100
READ_REFILL=60
READ_TTL=3600
WRITE_CAPACITY=30
WRITE_REFILL=60
WRITE_TTL=3600
UNLIMITED_CAPACITY=999999

BASE_PATH=/manage-app
LOG_FILE=logs/app.log
```

### Run

```bash
git clone https://github.com/SNagarjuna07/food-waste-management.git
cd food-waste-management
mvn spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## 🌍 Observability

Spring Boot Actuator with custom domain health indicators:

- **`/manage-app/health`** — includes custom `MongoHealthIndicator` and `DonationDomainHealthIndicator` (checks if active collection centers exist)
- **`/manage-app/info`** — app name, version, description
- Dev profile exposes: `health`, `info`, `metrics`, `httpexchanges`, `shutdown`
- Prod profile exposes: `health`, `info` only

---

## 🔮 What I'd Build Next

- Refresh token support (sliding sessions)
- Prometheus metrics export
- Role-based admin analytics dashboard
- Docker Compose setup for one-command local dev

---

## 👤 Author

**S Nagarjuna** — Java Backend Developer  
[LinkedIn](https://www.linkedin.com/in/s-nagarjuna) · [GitHub](https://github.com/SNagarjuna07) · nagarjun2790@gmail.com

---

## 📄 License

MIT — built for portfolio and learning purposes.
