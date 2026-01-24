![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green)
![Build](https://img.shields.io/badge/Build-Maven-success)
![Security](https://img.shields.io/badge/Security-JWT-red)
![License](https://img.shields.io/badge/License-MIT-yellow)

# 🍽️ Food Waste Management Backend

A **secure, scalable, and production-ready Spring Boot backend** designed to manage food waste donation workflows efficiently.  
The system supports **JWT-based authentication**, **role-aware authorization**, **Redis-backed rate limiting**, **MongoDB persistence**, **Kafka-based notifications**, and **environment-specific configuration** for development and production deployments.

---

## 🚀 Key Features

### 🔐 Security
- JWT-based stateless authentication
- Role-based authorization (ADMIN / USER)
- Custom **401 Unauthorized** and **403 Forbidden** handling
- BCrypt password hashing
- Method-level security using `@PreAuthorize`

---

### 🌐 OAuth2 Authentication (Google)
- Secure login using Google OAuth2
- Automatic user provisioning on first login
- Seamless integration with existing JWT-based security
- Stateless OAuth2 flow (no server-side sessions
- Suitable for web and mobile clients

---

### ⚡ Rate Limiting (Redis-Backed)
- Profile-based rate limits: **AUTH, READ, WRITE, UNLIMITED**
- IP-based and User-based throttling
- Configurable per endpoint
- Prevents brute-force and abuse attacks
- Implemented outside Spring Security for clean separation of concerns

---

### 🗄️ Data & Messaging
- MongoDB for persistent storage
- Redis for caching and rate-limiting counters
- Apache Kafka for asynchronous notifications
- Optimized producer and consumer configurations

---

### 📊 Observability & Monitoring
- Spring Boot Actuator
  - `/health`
  - `/info`
- Environment-specific exposure (open in dev, restricted in prod)
- Graceful shutdown support
- Production-ready logging configuration

---

### 🧩 API Documentation
- Swagger / OpenAPI 3
- Interactive API documentation
- JWT Bearer authentication support
- Easy testing of secured endpoints

---

### 🛠️ Environment & Configuration
- Profile-based configuration (`dev`, `prod`)
- Secrets managed via environment variables
- `.env` support for local development
- Clear separation of common, dev, and prod configs

---

## 🧪 Tech Stack

| Layer | Technology |
|------|-----------|
| Language | Java 17 |
| Framework | Spring Boot |
| Security | Spring Security, JWT |
| Database | MongoDB |
| Cache / Rate Limiting | Redis |
| Messaging | Apache Kafka |
| API Docs | Swagger / OpenAPI |
| Build Tool | Maven |
| Monitoring | Spring Boot Actuator |

---

## ⚙️ Swagger Config

<img width="1799" height="2351" alt="Swagger" src="https://github.com/user-attachments/assets/d04adc9b-32f3-4966-a97f-e4d6e6b3f2c5" />

---

## 🌍 Production Readiness

- Secrets excluded from Git
- Clean .gitignore
- Profile-specific configuration
- Stateless and horizontally scalable
- Cloud-ready (Render, Railway, AWS, etc.)

---

## 📌 Future Enhancements

- Refresh token support
- Admin dashboards
- Metrics export (Prometheus)
- Role-based analytics

--- 

## 👤 Author

### S Nagarjuna
### Backend Developer | Spring Boot | Secure API Design

---

## 📄 License

This project is intended for educational and portfolio purposes.
