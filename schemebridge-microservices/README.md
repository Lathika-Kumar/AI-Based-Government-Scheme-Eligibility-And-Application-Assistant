# SchemeBridge Backend - Phase 1 Foundation (MongoDB)

SchemeBridge is an AI-Powered Government Scheme Recommendation & Application Platform.

This project is built using **Java 17+**, **Spring Boot 3.3.1**, **Spring Data MongoDB**, **MongoDB 7.0**, **Spring Security**, and **JWT**.

---

## 🛠️ Tech Stack & Prerequisites

- **Java**: Java 17+ (OpenJDK 17 / Eclipse Temurin 17)
- **Framework**: Spring Boot 3.3.1
- **Database**: MongoDB 7.0+
- **Security**: Spring Security 6 & JJWT (0.12.5)
- **Documentation**: SpringDoc OpenAPI / Swagger UI
- **Build Tool**: Apache Maven 3.9+
- **Containerization**: Docker & Docker Compose

---

## ⚙️ Environment Variables

The following environment variables can be configured or left to their defaults:

| Variable | Description | Default Value |
| --- | --- | --- |
| `SPRING_DATA_MONGODB_URI` | MongoDB connection URI | `mongodb://localhost:27017/schemebridge` |
| `JWT_SECRET` | Secret key for signing JWT tokens | `SchemeBridgeSuperSecretKeyForJWTSigning...` |
| `JWT_EXPIRATION_MS` | JWT token lifespan in milliseconds | `86400000` (24 Hours) |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins (comma-separated) | `http://localhost:5173,http://localhost:3000` |

---

## 🚀 Running Locally

### Option 1: Running with Local MongoDB
1. Ensure MongoDB service is running on `localhost:27017`.
2. Run Maven compile and startup:
   ```bash
   mvn clean compile
   mvn spring-boot:run
   ```

### Option 2: Running with Docker Compose
To spin up both MongoDB 7.0 and the Spring Boot application containers simultaneously:
```bash
docker-compose up --build
```

---

## 📄 Database Collections & Auto-Seeding

- Spring Data MongoDB manages collections and auto-creates unique indexes for `@Indexed` fields (`email`, `name`).
- Collections: `roles`, `users`, `citizen_profiles`.
- Default platform roles auto-seeded on startup via `DataSeeder` (`CommandLineRunner`):
  - `CITIZEN`
  - `VERIFICATION_OFFICER`
  - `SCHEME_MANAGER`
  - `SUPER_ADMIN`

---

## 📌 REST Endpoints (API Version `/api/v1`)

| Method | Endpoint | Description | Auth Required |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Register new user (Citizen, Officer, Manager, Admin) | No |
| `POST` | `/api/v1/auth/login` | Authenticate user & generate JWT access token | No |
| `POST` | `/api/v1/auth/logout` | Clear user security session | No |
| `POST` | `/api/v1/auth/forgot-password` | Initiate password recovery token | No |
| `POST` | `/api/v1/auth/reset-password` | Reset password using token | No |
| `GET` | `/actuator/health` | Service health status | No |
| `GET` | `/actuator/info` | Service info | No |

---

## 📖 Swagger / OpenAPI Documentation

Interactive Swagger UI is available at:
👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**
OpenAPI JSON endpoint: `http://localhost:8080/v3/api-docs`

> **Testing Protected Endpoints in Swagger**:
> Click **Authorize** button in Swagger UI and input `Bearer <your_jwt_access_token>`.

---

## 🧪 Testing Commands

Execute unit tests and verification checks:
```bash
mvn clean test
```
