![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?logo=springboot)
![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-green?logo=mongodb)
![Redis](https://img.shields.io/badge/Redis-Cache%20%2B%20Rate%20Limit-red?logo=redis)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-Async%20Messaging-black?logo=apachekafka)
![Security](https://img.shields.io/badge/Auth-JWT%20%2B%20OAuth2-blue?logo=jsonwebtokens)
![Build](https://img.shields.io/badge/Build-Maven-purple?logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-yellow)

# 🍽️ Food Waste Management System — Backend

> A production-grade Spring Boot backend connecting food donors with recipients.
> Built solo. Designed with real failure scenarios in mind.

---

## ⚡ Performance — Load Tested

> Tested locally using Apache JMeter · 100 concurrent users · 10s ramp-up · 20 loops

| Endpoint | Requests | Min (ms) | Avg (ms) | Throughput | Error % |
|---|---|---|---|---|---|
| /donors | 2,000 | 176 | 3,254 | 7.4/sec | **0.00%** |
| /donations | 2,000 | 206 | 3,780 | 7.4/sec | **0.00%** |
| /collection-centers | 2,000 | 138 | 2,664 | 7.4/sec | **0.00%** |
| /notifications | 2,000 | 173 | 3,231 | 7.4/sec | **0.00%** |
| **TOTAL** | **8,000** | **138** | **3,232** | **29.3/sec** | **0.00%** |

**Key result: 8,000 requests across 4 endpoints — zero failures under 100 concurrent users.**

> ℹ️ High average latency reflects local development environment (MongoDB + Redis running on same machine).
> Minimum response times (138ms) reflect actual endpoint performance.

---

## 🤔 Why This Exists

Most Spring Boot tutorials stop at "it works on my machine."

This project asks different questions:

- What happens when **Redis goes down mid-request**? Does the app crash or serve traffic gracefully?
- How do you prevent **rate-limit 429 responses from leaking into Spring Security's exception model** as 403s?
- How do you keep **Kafka failures from rolling back a user's successful action**?
- How do you design a **filter chain where IP-based limits run before auth, and user-based limits run after**?

Every design decision in this codebase has a reason. The reasons are documented below.

---

## 🛠️ Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Language | Java 17 | Modern records |
| Framework | Spring Boot 3.x | Production defaults, autoconfiguration |
| Security | Spring Security + JWT + OAuth2 | Stateless, no server sessions |
| Messaging | Apache Kafka | Async notifications, decoupled from request lifecycle |
| Cache + Rate Limit | Redis | Sub-millisecond ops, atomic Token Bucket |
| Database | MongoDB | Flexible schema, compound indexes |
| Documentation | Swagger / OpenAPI | Auto-generated, always in sync |
| Build | Maven | Standard, reproducible builds |
| Load Testing | Apache JMeter | Real concurrency simulation |

---

## 📐 Architecture

```
Client Request
      │
      ▼
┌──────────────────────────────────┐
│   PreAuthRateLimitingFilter      │  Token Bucket (Redis) — before Spring Security
│   Fail-open on Redis failure     │  Returns 429 with full X-RateLimit headers
└───────────────┬──────────────────┘
                │
                ▼
┌──────────────────────────────────┐
│   JwtAuthenticationFilter       │  Validates JWT, populates SecurityContext
│   (Spring Security Chain)       │  Custom 401/403 — no Spring HTML error pages
└───────────────┬──────────────────┘
                │
                ▼
┌──────────────────────────────────┐
│   PostAuthRateLimitingFilter     │  Re-checks rate limit by USER identity
│   After identity is known        │  Per-user throttling on write endpoints
└───────────────┬──────────────────┘
                │
                ▼
┌──────────────────────────────────┐
│   Controllers → Services        │
│                                 │
│   ┌─────────┐  ┌─────────────┐  │
│   │ MongoDB │  │ Redis Cache │  │
│   └─────────┘  └─────────────┘  │
│                                 │
│   ┌──────────────────────────┐  │
│   │ Kafka Notification Bus   │  │  Async — never blocks API response
│   └──────────────────────────┘  │
└──────────────────────────────────┘
```

**Why this filter order matters:**
A `429` from inside Spring Security is treated as a `403 Forbidden` by clients. The pre-auth filter fires before Spring Security processes the request — keeping rate limit responses clean and semantically correct.

---

## 🔐 Security

### Dual Authentication — JWT + Google OAuth2

```
Local Auth:                           Google OAuth2:
  POST /auth/register                   GET /auth/google/callback
  POST /auth/login                        ↓
        ↓                             Authorization Code
  BCrypt hash verify                      ↓
        ↓                             Google Access Token
  JWT issued (1hr expiry)                 ↓
        ↓                             User Profile fetched
  Stateless filter chain                  ↓
                                      Local JWT issued
                                          ↓
                                      Stateless filter chain
```

**Account takeover prevention:** Email registered via local auth cannot be accessed via Google OAuth2 — enforced at the service layer before any token is issued.

### Security Features
- HMAC-SHA signed JWT, 1-hour expiry
- BCrypt password hashing (cost factor 10)
- `@PreAuthorize` method-level role enforcement
- Custom `401` / `403` JSON handlers — clients always get structured error responses
- Zero secrets in version control — full `.env` driven configuration

---

## 🚦 Rate Limiting — Custom Token Bucket on Redis

Four profiles, fully configurable via environment variables:

| Profile | Scope | Applied To | Identity Used |
|---|---|---|---|
| `AUTH` | IP-based | Login, Register, OAuth | Remote IP |
| `READ` | User-based | All GET endpoints | Authenticated user |
| `WRITE` | User-based | POST / PATCH / DELETE | Authenticated user |
| `UNLIMITED` | Bypass | Swagger, Actuator | None |

### Response Headers (every request)
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1716023460
Retry-After: 42        ← only on 429
```

### Key Design Decisions

**Fail-open:** Redis unavailable → traffic passes through. System availability > strict rate enforcement.

**TTL preservation:** When updating a Token Bucket in Redis, the existing TTL is explicitly preserved.
Without this, every request would reset the bucket's lifetime — making limits trivially bypassable.

**Pre-auth vs post-auth split:** Login brute-force protection runs on IP before auth.
Per-user write limits run after auth once identity is confirmed.
Same filter class, two instances, different configurations.

---

## 📨 Kafka — Async Notification Pipeline

Notifications are fully decoupled from the request lifecycle.

### Event → Topic Mapping

| User Event | Kafka Topic | Consumer Action |
|---|---|---|
| Email verification | `app-notification-events` | Send verification email |
| Password reset requested | `app-notification-events` | Send reset link email |
| Password reset completed | `app-notification-events` | Send confirmation email |
| Donation collected | `app-notification-events` | Notify donor + recipient |

### Design Decisions

**Producer — graceful degradation:**
Kafka unavailability is caught and logged as a warning. The triggering action (e.g. collecting a donation) is **not rolled back**. Users complete their action. Notifications are the side effect, not the core operation.

**Consumer — manual acknowledgment:**
`ack-mode=manual` — messages are only acknowledged after successful processing. A crash mid-processing re-delivers the message. Failed deserialization is logged and skipped — never crashes the consumer.

**Why Kafka and not a direct email call?**
A synchronous email call adds 200-500ms to every response and creates a hard dependency on an external service. Kafka makes notifications eventually consistent — fast core API, reliable delivery.

---

## 🗄️ MongoDB Design

### Collections and Indexes

| Collection | Compound Indexes | Purpose |
|---|---|---|
| `users` | unique: `email`, `username` | Fast auth lookup |
| `food_donors` | `{ createdBy: 1, createdAt: -1 }` | Paginated user-scoped list |
| `food_donations` | `{ createdAt: -1 }` | Recent donations sorted |
| `collection_centers` | `{ active: 1, createdAt: -1 }` | Active center filter |
| `notifications` | `{ userId: 1, read: 1 }` · `{ userId: 1, createdAt: -1 }` | Unread count + sorted list |
| `email_verification_tokens` | unique: `token`, indexed: `expiryTime` | Fast token lookup + TTL queries |
| `password_reset_tokens` | unique: `token`, indexed: `expiryTime` | Fast token lookup + TTL queries |

### Design Decisions

**Compound indexes on every paginated endpoint:**
Every list endpoint filters + sorts. Single-field indexes would require in-memory sorting after filtering. Compound indexes let MongoDB satisfy the full query from the index alone.

**Custom `MongoTemplate` for bulk updates:**
`markAllAsRead` uses `MongoTemplate.updateMulti` instead of loading all documents, updating each, and saving back. One DB round trip vs N round trips.

**Custom `PageResponse<T>` wrapper:**
Spring Data's default `Page<T>` serialises to a verbose nested JSON with Spring internals. `PageResponse<T>` controls exactly what the client sees: `content`, `page`, `size`, `totalElements`, `totalPages`.

---

## 🧰 Redis Cache Strategy

Cache-aside pattern. All cache errors are caught and logged — never thrown to the caller.

| Cache Key | TTL | Evicted On |
|---|---|---|
| `collectionCenters` | 5 min | Create / Update / Delete |
| `activeCollectionCenters` | 2 min | Create / Update / Delete |
| `collectionCenterById` | 10 min | Update / Delete that ID |
| `foodDonorsByUser` | 2 min | Create / Update / Delete |
| `foodDonorIdsByUser` | 10 min | Create / Update / Delete |
| `userNotifications` | 30 sec | New notification / mark read |
| `userUnreadNotificationCount` | 15 sec | New notification / mark read |
| `userByUsername` | 5 min | Update / Delete user |

**Redis unavailable at startup:**
`CacheManager` performs a health check. If Redis is unreachable, falls back to `NoOpCacheManager`. Application starts normally — cache misses fall through to MongoDB.

---

## 📡 API Reference

| Module | Method | Endpoint | Auth |
|---|---|---|---|
| **Auth** | POST | `/auth/register` | Public |
| | POST | `/auth/login` | Public |
| | GET | `/auth/verify-email` | Public |
| | POST | `/auth/forgot-password` | Public |
| | POST | `/auth/reset-password` | Public |
| **Google OAuth** | GET | `/auth/google/callback` | Public |
| **Donors** | POST | `/donors` | USER |
| | GET | `/donors` | USER |
| | PATCH | `/donors/{id}` | USER (owner) |
| | DELETE | `/donors/{id}` | USER (owner) |
| **Donations** | POST | `/donations` | USER |
| | GET | `/donations/{id}` | USER |
| | GET | `/donations/my-donations` | USER |
| | PATCH | `/donations/{id}` | USER (owner) |
| | DELETE | `/donations/{id}` | USER (owner) |
| | POST | `/donations/{id}/collect` | USER |
| **Collection Centers** | POST | `/collection-centers` | ADMIN |
| | GET | `/collection-centers` | USER |
| | GET | `/collection-centers/active` | USER |
| | GET | `/collection-centers/{id}` | USER |
| | PATCH | `/collection-centers/{id}` | ADMIN |
| | DELETE | `/collection-centers/{id}` | ADMIN |
| **Notifications** | GET | `/notifications` | USER |
| | GET | `/notifications/unread-count` | USER |
| | PATCH | `/notifications/{id}/read` | USER |
| | PATCH | `/notifications/read-all` | USER |

---

## 🔄 Scheduled Cleanup Jobs

| Job | Schedule | Action |
|---|---|---|
| Email token cleanup | Sundays 18:00 | Deletes expired / used verification tokens |
| Password reset cleanup | Sundays 19:00 | Deletes expired / used reset tokens |
| Notification cleanup | Daily 12:00 | Deletes read notifications older than 10 days |

---

## 🌍 Observability

Spring Boot Actuator with custom health indicators:

| Endpoint | Profile | What it checks |
|---|---|---|
| `/manage-app/health` | Both | MongoDB connectivity + active collection centers exist |
| `/manage-app/info` | Both | App name, version, description |
| `/manage-app/metrics` | Dev only | JVM, HTTP, DB metrics |
| `/manage-app/httpexchanges` | Dev only | Recent HTTP request trace |
| `/manage-app/shutdown` | Dev only | Graceful shutdown |

**Custom `DonationDomainHealthIndicator`:**
Goes beyond "is MongoDB up" — checks if the system has active collection centers. A system with zero active centers is technically running but functionally broken. This distinction surfaces in the health check.

---

## 🚀 Local Setup

### Prerequisites
```
Java 17, Maven, MongoDB, Redis, Apache Kafka + Zookeeper
Gmail account (for email), Google OAuth2 credentials
```

### Environment Variables
Create `.env` in project root (never committed):

```env
# Database
MONGO_URI=mongodb://localhost:27017
DB_NAME=foodwaste_db
ACTIVE_PROFILE=dev

# Auth
JWT_KEY=your-256-bit-secret

# Email
MAIL_PORT=587
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your-app-password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_TIMEOUT=2000ms

# Google OAuth2
OAUTH2_CLIENT_ID=your-client-id
OAUTH2_CLIENT_SECRET=your-client-secret
OAUTH2_REDIRECT_URI=http://localhost:8080/auth/google/callback

# Kafka
KAFKA_TOPIC_GROUP_ID=app-notification-group

# Rate Limits (requests/window)
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

# App
BASE_PATH=/manage-app
LOG_FILE=logs/app.log
```

### Run
```bash
git clone https://github.com/SNagarjuna07/Food-Waste-Management.git
cd Food-Waste-Management
mvn spring-boot:run
```

Swagger UI → `http://localhost:8080/swagger-ui/index.html`

---

## 🔮 What I'd Add Next

| Feature | Why |
|---|---|
| Refresh token rotation | Current 1-hour JWT expiry forces re-login. Sliding sessions improve UX. |
| Prometheus metrics export | Actuator metrics need a scraper to be useful in production. |
| Docker Compose setup | Single command local dev — eliminates manual MongoDB/Redis/Kafka setup. |
| Admin analytics dashboard | Aggregate donation data by region, time, donor — currently possible via JPQL, not exposed. |
| Distributed tracing (Zipkin) | Current logs don't correlate across Kafka consumer ↔ producer spans. |

---

## 👤 Author

**S Nagarjuna** — Java Backend Developer
Building production-grade systems, not tutorial apps.

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-blue?logo=linkedin)](https://www.linkedin.com/in/s-nagarjuna)
[![GitHub](https://img.shields.io/badge/GitHub-Follow-black?logo=github)](https://github.com/SNagarjuna07)
[![Email](https://img.shields.io/badge/Email-Contact-red?logo=gmail)](mailto:nagarjun2790@gmail.com)

---

## 📄 License

MIT — built for portfolio and learning purposes.
