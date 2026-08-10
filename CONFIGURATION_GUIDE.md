# 📘 SchemeBridge Enterprise Microservices - Centralized Configuration Guide

This guide provides exhaustive documentation on the **Spring Cloud Config Server Architecture** integrated into the SchemeBridge Enterprise Microservices platform.

---

## 1. 🏗️ Architecture Overview

SchemeBridge utilizes a dedicated **Spring Cloud Config Server** running on **Port 8888** that acts as the single source of truth for externalized configurations across all microservices.

```
                   +------------------------------------+
                   |     Centralized Config Server      |
                   |            (Port 8888)             |
                   +-----------------+------------------+
                                     |
           +-------------------------+-------------------------+
           |                         |                         |
  +--------v-------+        +--------v-------+        +--------v-------+
  |  Native File   |        | Git Repository |        | HashiCorp      |
  |  System Repo   |        |  (GitHub/GitLab|        | Vault / AWS    |
  | (classpath: /  |        |  / Bitbucket)  |        | Parameter Store|
  |  config-repo)  |        +----------------+        +----------------+
  +----------------+
```

### Key Highlights:
- **Port**: `8888`
- **Profiles Supported**: `native` (default for local dev), `git` (for production/cloud staging), `development`, `testing`, `staging`, `production`
- **Fail-Fast & Retries**: Clients configured with `fail-fast: true` and `spring-retry` with exponential backoff.
- **Dynamic Property Import**: Uses Spring Boot 3 `spring.config.import: optional:configserver:http://localhost:8888`.
- **Runtime Refresh**: Implements `@RefreshScope` and `/actuator/refresh` readiness.

---

## 2. 📂 Centralized Config Repository Layout

The configuration repository resides at `config-server/src/main/resources/config-repo/` containing:

| Configuration File | Scope & Description |
| :--- | :--- |
| `application.yml` | **Global Ecosystem Defaults**: Eureka client defaults, JWT keys, Swagger paths, Actuator config, Feature Flags (`feature.*`), future Cloud/Middleware readiness blocks under `schemebridge.*`. |
| `application-development.yml` | **Development Profile**: Verbose debug logging, local database connections. |
| `application-production.yml` | **Production Profile**: WARN/ERROR logging, optimized connection pools, strict security. |
| `application-secrets.yml` | **Secret Management**: Sensitive keys (JWT secret, DB passwords, Brevo API key, Twilio tokens, FCM path) backed by Environment Variables. |
| `service-registry.yml` | **Eureka Registry**: Port `8761`, Eureka server self-preservation settings. |
| `api-gateway.yml` | **API Gateway**: Port `8080`, Dynamic locator, global CORS, dynamic routes to microservices. |
| `auth-service.yml` | **Auth Service**: Port `8081`, Oracle DB thin driver, HikariCP, JPA, JWT, Brevo. |
| `citizen-service.yml` | **Citizen Service**: Port `8082`, MongoDB URI (`schemebridge_citizen_db`), mongo indexing. |
| `scheme-service.yml` | **Scheme Service**: Port `8083`, Oracle DB thin driver, HikariCP, JPA. |
| `application-service.yml` | **Application Service**: Port `8084`, MongoDB URI (`schemebridge_application_db`), inter-service client URLs. |
| `document-service.yml` | **Document Service**: Port `8085`, MongoDB URI (`schemebridge_document_db`), file storage paths, MIME types, upload limits. |
| `notification-service.yml` | **Notification Service**: Port `8086`, MongoDB URI (`schemebridge_notification_db`), Brevo, Twilio, FCM, schedulers. |
| `admin-service.yml` | **Admin Service**: Port `8087`, Oracle DB thin driver, HikariCP, JPA, cleanup/analytics CRONs. |

---

## 3. 🔑 Environment Variable Resolution & Secret Management

All secrets in `application-secrets.yml` utilize safe local fallback defaults and resolve dynamically from OS environment variables:

```yaml
jwt:
  secret: ${JWT_SECRET:schemeBridgeEnterpriseJwtSecretKey2026SuperSecureKey...}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}

brevo:
  api-key: ${BREVO_API_KEY:xkeysib-placeholder-test-key-2026}
```

### Production Export Example:
```bash
export JWT_SECRET="MyProductionSuperSecure2026Key..."
export ORACLE_PASSWORD="ProdSecurePassword123"
export BREVO_API_KEY="xkeysib-prod-real-api-key"
```

---

## 4. 🔀 Native vs Git Backend Configuration

### Native Mode (Default for Local Development)
In `config-server/src/main/resources/application.yml`:
```yaml
spring:
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config-repo
```

### Switching to Git Repository Mode (Production Readiness)
To switch to Git without touching a single line of Java code, set the environment variable or property:
```yaml
spring:
  profiles:
    active: git
  cloud:
    config:
      server:
        git:
          uri: https://github.com/your-org/schemebridge-config-repo.git
          default-label: main
          force-pull: true
```

---

## 5. 🐳 Docker & Kubernetes Integration

### Docker Compose
```yaml
  config-server:
    image: schemebridge/config-server:latest
    ports:
      - "8888:8888"
    environment:
      - SPRING_PROFILES_ACTIVE=native
      - ENCRYPT_KEY=MyEnterpriseEncryptionKey
```

### Kubernetes ConfigMap & Secret
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: config-server-config
data:
  SPRING_PROFILES_ACTIVE: "native"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: config-server
spec:
  replicas: 2
  template:
    spec:
      containers:
        - name: config-server
          image: schemebridge/config-server:v1
          ports:
            - containerPort: 8888
```

---

## 6. 🚀 How to Add a New Microservice

1. **Add Dependency**: Include `spring-cloud-starter-config` and `spring-retry` in the service's `pom.xml`.
2. **Create Local `application.yml`**:
   ```yaml
   spring:
     application:
       name: my-new-service
     config:
       import: "optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}"
   ```
3. **Add Config File in `config-repo`**: Create `config-repo/my-new-service.yml` with port, database, and service-specific configurations.

---

## 7. ➕ How to Add New Properties

1. **Global Properties**: Add to `config-repo/application.yml`.
2. **Secrets**: Add to `config-repo/application-secrets.yml` backed by `${ENV_VAR:default}`.
3. **Service-Specific**: Add to `config-repo/<service-name>.yml`.
4. **Trigger Refresh**: POST to `http://localhost:<service-port>/actuator/refresh`.
