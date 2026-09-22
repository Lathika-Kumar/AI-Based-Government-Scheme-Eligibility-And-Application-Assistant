# SYSTEM REQUIREMENTS SPECIFICATION (SRS)
## Full Stack Java (AI-Integrated) Training Programme
### Karpagam College of Engineering, Coimbatore

---

| Field | Detail |
|---|---|
| **Project Title** | **SchemeBridge: AI-Based Government Scheme Eligibility & Application Assistant** |
| **SIH Problem Statement ID** | **SIH25120** (Smart India Hackathon 2025) |
| **Ministry / Organisation** | **Ministry of Electronics and Information Technology (MeitY) & Ministry of Social Justice and Empowerment, Government of India** |
| **Domain Category** | **Smart Governance / Citizen Services / Artificial Intelligence & Public Welfare** |
| **Team Name** | **Team SchemeBridge** |
| **Team Members** | 1. Lead Full-Stack Java & AI Developer (CSE)<br>2. Frontend Architect & UI/UX Specialist (CSE)<br>3. Cloud, DevOps & Quality Assurance Engineer (CSE/IT) |
| **Institution** | **Karpagam College of Engineering (Autonomous), Coimbatore - 641032** |
| **Project Type** | **[X] Software only** &nbsp;&nbsp;&nbsp;&nbsp; [ ] Software + Hardware (Hybrid) |
| **Version** | **v1.0 (Final Official Release)** |
| **Date** | **September 21, 2026** |
| **Faculty Mentors** | **Dr. Arul Antran Vijay S / Dr. Jothi Prakash V / Mr. Jegathesh P / Mr. Navaneetha Krishnan M / Dr. Castro S.** |

> *Note: This SRS document is prepared in accordance with IEEE Std 830-1998 for the Full Stack Java (AI-Integrated) Training Programme Internal Review (Day 22) and SIH Evaluation.*

---

## DOCUMENT REVISION HISTORY

| Version | Date | Author | Description of Changes |
|---|---|---|---|
| **v1.0** | 2026-09-05 | Team SchemeBridge | Initial draft — Complete SRS skeleton, architecture design, and database schema definition. |
| **v1.1** | 2026-09-14 | Team SchemeBridge | Integrated AI/ML Module Specifications (Random Forest 98.40% classifier + Sentence Transformer semantic search), API catalogs, and Docker configurations. |
| **v2.0** | 2026-09-21 | Team SchemeBridge | Final post-review edition: Complete use cases (FR-001 to FR-016), NFR verification metrics, full ER data dictionary, CI/CD pipeline, and SIH compliance updates. |

---

## 1. INTRODUCTION

### 1.1 Purpose
This System Requirements Specification (SRS) document describes the functional, non-functional, interface, and machine learning requirements for **SchemeBridge: AI-Based Government Scheme Eligibility & Application Assistant**, developed as part of the **Smart India Hackathon 2025** submission under Problem Statement **SIH25120**. It is structured in strict accordance with **IEEE Std 830-1998** and serves as the definitive technical contract and design baseline between the student engineering team, faculty evaluators, and industry reviewers.

### 1.2 Scope
- **System Name:** **SchemeBridge (Citizen Welfare Discovery & Automated Filing Engine)**
- **What the system does:**  
  SchemeBridge is an intelligent, bilingual digital governance platform designed to eliminate information asymmetry and bureaucratic barriers preventing vulnerable citizens from accessing Indian central and state government welfare schemes. The platform provides:
  1. Instant natural language and demographic profiling to match citizens with 4,734+ government welfare schemes.
  2. High-precision statutory eligibility checking combining deterministic Abstract Syntax Tree (AST) business rules with machine learning classification.
  3. Secure digital document vault with automated optical character recognition (OCR) and document readiness verification.
  4. End-to-end assisted application filing, grievance registration, and administrative verification workflows with real-time auditability.

### 1.3 Definitions, Acronyms & Abbreviations

| Term / Acronym | Definition |
|---|---|
| **SRS** | System Requirements Specification — this IEEE Std 830-1998 document |
| **SIH** | Smart India Hackathon 2025 |
| **API** | Application Programming Interface (RESTful HTTP/JSON) |
| **REST** | Representational State Transfer — stateless architectural style for HTTP APIs |
| **JWT** | JSON Web Token — cryptographically signed token for stateless authentication |
| **ML / AI** | Machine Learning / Artificial Intelligence |
| **AST** | Abstract Syntax Tree — syntactic tree representation of statutory eligibility logic |
| **OCR** | Optical Character Recognition — automated text extraction from scanned certificates |
| **CRUD** | Create, Read, Update, Delete — fundamental database storage operations |
| **CI/CD** | Continuous Integration / Continuous Deployment — automated build, test, and release pipeline |
| **FR** | Functional Requirement |
| **NFR** | Non-Functional Requirement |
| **UC** | Use Case |
| **ER** | Entity-Relationship (database architectural schema) |
| **SPA** | Single Page Application (built via React 18 and Vite) |
| **PWA** | Progressive Web Application — client web application with offline-first caching |
| **DPDPA** | Digital Personal Data Protection Act, 2023 (India) |
| **BPL / EWS** | Below Poverty Line / Economically Weaker Section |

### 1.4 References
- **IEEE Std 830-1998:** IEEE Recommended Practice for Software Requirements Specifications.
- **SIH 2025 Problem Statement SIH25120:** Ministry of Electronics and Information Technology (MeitY).
- **Spring Boot 3.3.2 Documentation:** [https://docs.spring.io/spring-boot/](https://docs.spring.io/spring-boot/)
- **Spring Security 6.x Architecture:** [https://docs.spring.io/spring-security/reference/](https://docs.spring.io/spring-security/reference/)
- **React 18 & Vite Documentation:** [https://react.dev/](https://react.dev/) and [https://vitejs.dev/](https://vitejs.dev/)
- **MongoDB 7.0 Manual:** [https://www.mongodb.com/docs/v7.0/](https://www.mongodb.com/docs/v7.0/)
- **Oracle Database 21c / XE Documentation:** [https://docs.oracle.com/en/database/](https://docs.oracle.com/en/database/)
- **Digital Personal Data Protection Act 2023:** Gazette of India, Ministry of Law and Justice.

### 1.5 Document Overview
- **Section 2** details product perspective, 12 core product functions, user classes, operational environments, constraints, and dependencies.
- **Section 3** establishes the multi-layer microservices architecture, technology stack, and end-to-end data flows.
- **Section 4** specifies external interfaces (UI screens, third-party APIs, communication protocols).
- **Section 5** formalizes functional requirements in standard use case tables across all services.
- **Section 6** delivers comprehensive AI/ML specifications for the Random Forest Classifier and Semantic Vector Search / Conversational AI.
- **Sections 7–10** define measurable non-functional requirements, database design (ER diagram + data dictionary), API specifications, and DevOps containerization plans.
- **Sections 11–14** establish the project milestone schedule, team ownership matrix, risk register, and technical appendices.

---

## 2. OVERALL DESCRIPTION

### 2.1 Product Perspective
SchemeBridge replaces the currently fragmented, manual, and error-prone process of discovering and applying for government schemes in India. Citizens currently have to navigate dozens of disconnected state and central portals (e.g., National Scholarship Portal, PM-KISAN, State e-Seva centres) or rely on predatory middlemen. SchemeBridge acts as an integrated intelligent bridge: it indexes 4,734+ government schemes from official gazettes and open datasets, cross-verifies citizen demographic profiles against statutory criteria, and routes validated applications directly into standardized administrative review queues.

```
┌─────────────────────────┐       ┌─────────────────────────┐       ┌─────────────────────────┐
│     Disorganized        │       │       SchemeBridge      │       │     Authorized State    │
│  Gazettes & Guidelines  │ ────► │  AI & Rules Engine Hub  │ ────► │    & Central Review     │
│  (Central / State Data) │       │   (Spring Boot + ML)    │       │     Officer Portal      │
└─────────────────────────┘       └─────────────────────────┘       └─────────────────────────┘
                                               ▲
                                               │
                                  ┌─────────────────────────┐
                                  │   Citizen / Beneficiary │
                                  │  Web & Mobile Interface │
                                  └─────────────────────────┘
```

### 2.2 Product Functions — Feature Summary

| # | Feature Name | Description |
|---|---|---|
| **1** | **Bilingual Citizen Authentication & Onboarding** | Secure OTP/password registration and progressive 6-step demographic profile creation (age, income, caste category, occupation, state/district domicile). |
| **2** | **Deterministic Statutory Eligibility Engine** | High-performance Boolean AST engine evaluating complex nested rules (age ranges, income caps, landholding limits, gender restrictions) with zero false-positives. |
| **3** | **Machine Learning Scheme Recommendation** | Standalone multi-class scikit-learn Random Forest model predicting citizen welfare scheme categories with 98.40% accuracy based on demographic features. |
| **4** | **Dense Semantic Vector Search (NLP)** | 384-dimensional dense semantic vector retrieval (`all-MiniLM-L6-v2`) enabling conversational, typo-tolerant natural language scheme search. |
| **5** | **Conversational AI Citizen Assistant** | Bilingual conversational chat assistant guiding users through scheme benefits, eligibility criteria, and required supporting documents. |
| **6** | **Encrypted Citizen Document Vault** | Tamper-proof personal repository for storing Aadhaar, Income, Caste, Land Record, and Disability certificates with version control. |
| **7** | **Intelligent Document OCR & Verification** | Automated extraction and field-matching of names, dates of birth, certificate numbers, and issue authorities from uploaded PDF/images. |
| **8** | **Guided 4-Step Scheme Application Wizard** | Step-by-step application submission workflow auto-populating verified citizen profile attributes to prevent data re-entry. |
| **9** | **Real-Time Application Status Tracker** | Transparent visual timeline tracking application progression from `SUBMITTED` to `UNDER_REVIEW`, `DOCUMENT_VERIFICATION`, `APPROVED`, or `REJECTED`. |
| **10** | **Citizen Grievance & Escalation System** | In-app dispute and grievance lodging mechanism with SLA timers and escalation workflows for delayed applications. |
| **11** | **Administrative Verification & Audit Dashboard** | Role-based portal for government verification officers to inspect applications, view OCR confidence scores, approve benefits, and log audit trails. |
| **12** | **Multi-Format Export & Analytical Reporting** | Dynamic generation of PDF application receipts, audit reports, and analytical aggregations on scheme disbursement metrics. |

### 2.3 User Classes and Characteristics

| User Role | Description | Technical Expertise | Primary Actions | Access Level |
|---|---|---|---|---|
| **Citizen (Beneficiary)** | General public seeking welfare benefits (farmers, students, women, senior citizens, BPL households). | Low to Medium | Profile onboarding, scheme discovery, eligibility assessment, document vault management, application submission, grievance filing. | Authenticated Citizen (`ROLE_USER`) |
| **Verification Officer (Field Staff)** | Authorized government department staff reviewing applications in a designated district or department. | Medium | Review submitted applications, cross-check OCR document extractions, request resubmission, approve or reject applications. | Verification Staff (`ROLE_VERIFICATION_OFFICER`) |
| **Scheme Manager** | Departmental official responsible for creating, editing, and publishing scheme guidelines and eligibility criteria. | High | Scheme catalog CRUD, rule criteria builder, document requirement management, circular uploads. | Scheme Admin (`ROLE_SCHEME_MANAGER`) |
| **System Administrator** | Technical administrator managing platform security, system configurations, and infrastructure health. | High | User management, role assignment, system health monitoring, audit log review, platform parameters. | Super Admin (`ROLE_ADMIN`) |
| **AI/ML Service Agent** | Internal microservice system actor executing automated background inferences. | Automated System | Vector index generation, multi-class category inference, OCR feature extraction, batch notification dispatch. | Internal Service-to-Service Token |

### 2.4 Operating Environment
- **Server / Cloud:** Docker containers deployed on Ubuntu 22.04 LTS / AWS EC2 / Railway / Local Kubernetes (minikube).
- **Backend Runtime:** Java 17 LTS, Spring Boot 3.3.2.
- **Frontend Runtime:** React 18.3, Vite 5.x, Node.js 18+ (runtime/build).
- **Databases:**
  - *Relational:* Oracle Database 21c XE (User authentication, credentials, roles, refresh tokens).
  - *Document / NoSQL:* MongoDB 7.0 (Master scheme catalog, applications, citizen vault documents, grievances, audit trails).
- **AI/ML Runtime:** Python 3.11, FastAPI, scikit-learn 1.4, Sentence-Transformers, NumPy, Pandas.
- **Client Browsers:** Chrome 100+, Mozilla Firefox 100+, Apple Safari 15+, Microsoft Edge (responsive across desktop and mobile down to 375px).
- **Network / Transport:** HTTPS (TLS 1.3), JSON REST endpoints, WebSockets (STOMP).

### 2.5 Design and Implementation Constraints
- **Multi-Service Architecture:** Backend logic partitioned cleanly between `schemebridge-auth-service` (Oracle DB) and `schemebridge-scheme-service` (MongoDB).
- **Stateless Authentication:** All protected APIs must enforce stateless Bearer JWT verification with cryptographic signature checks (HMAC-SHA256).
- **Deterministic Legal Eligibility:** Statutory welfare decisions must rely on deterministic Java rule validation (`EligibilityEngine.java`) to eliminate hallucination risks; ML recommendations are advisory.
- **Data Privacy Compliance:** Architecture must comply with the Indian Digital Personal Data Protection Act (DPDPA 2023); citizen documents and PII must be encrypted at rest (AES-256).
- **Zero-CORS Reverse Proxy:** Production container deployment routes traffic via unified Nginx ingress port 80/443 to eliminate cross-origin complexity.

### 2.6 Assumptions and Dependencies
- **Assumptions:**
  - Users possess basic digital literacy and access to a smartphone or desktop web browser with internet connectivity.
  - Government scheme guidelines and statutory eligibility criteria are published in machine-readable official circulars or standardized portals.
- **Dependencies:**
  - Python FastAPI inference runtime for high-dimensional semantic search and Random Forest predictions.
  - SMTP mail server for automated password reset and registration OTP dispatch.
  - Tesseract OCR / PDFBox libraries for parsing incoming application certificates.

---

## 3. SYSTEM ARCHITECTURE

### 3.1 High-Level Architecture Description
SchemeBridge employs a robust, loosely coupled microservices architecture structured into six distinct operational layers:

```
[ PRESENTATION LAYER ]
  React 18 SPA (Vite) + Tailwind CSS + Lucide Icons + Axios HTTP Client
                           │
                           ▼ (HTTP / Port 80, 443)
[ REVERSE PROXY / INGRESS GATEWAY LAYER ]
  Nginx Reverse Proxy / Spring Cloud Gateway
  - Routes /api/auth/* ───────────────► Auth Service
  - Routes /api/* (Schemes/Apps/Vault) ► Scheme Service
  - SSL/TLS Termination & Gzip Static Asset Delivery
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
[ BACKEND SERVICE LAYER ]
  ┌─────────────────────────┐   ┌─────────────────────────┐
  │  Auth Service (8080)    │   │  Scheme Service (8081)  │
  │  - Spring Boot 3.3.2    │   │  - Spring Boot 3.3.2    │
  │  - Spring Security JWT  │   │  - AST Eligibility     │
  │  - User & Profile Mgmt  │   │  - Application & Vault  │
  └───────────┬─────────────┘   └───────────┬─────────────┘
              │                             │
              ▼                             ├──────────────────────────┐
[ DATA PERSISTENCE LAYER ]                  ▼                          ▼
  ┌───────────────────────┐   ┌───────────────────────────┐  ┌─────────────────────────┐
  │   Oracle DB (1521)    │   │     MongoDB 7.0 (27017)   │  │  AI/ML FastAPI (8000)   │
  │  - Users & Roles      │   │  - 4,734+ Master Schemes  │  │  - Random Forest Model  │
  │  - Refresh Tokens     │   │  - Applications & Vault   │  │  - 384-D Vector Index   │
  │  - OTP State          │   │  - Grievances & Audits    │  │  - Document OCR Engine  │
  └───────────────────────┘   └───────────────────────────┘  └─────────────────────────┘
```

### 3.2 Microservices Breakdown

| # | Microservice Name | Port | Database | Primary Responsibility |
|---|---|---|---|---|
| **1** | **Frontend Ingress & Gateway** | 80 / 443 | None | Serves React SPA static assets; performs reverse proxy routing, request rate-limiting, and compression. |
| **2** | **Auth Service (`schemebridge-auth-service`)** | 8080 | Oracle XE (1521) | Citizen/Admin registration, BCrypt password hashing, JWT issue/refresh, and profile metadata. |
| **3** | **Scheme & Eligibility Service (`schemebridge-scheme-service`)** | 8081 | MongoDB (27017) | Core domain logic: 4,734+ scheme catalog, AST eligibility evaluation, application workflows, document vault, grievances, audit logs. |
| **4** | **AI/ML Inference Service** | 8000 | In-Memory / File Index | Python FastAPI microservice serving Random Forest category predictions and dense semantic vector similarity scoring. |

### 3.3 Technology Stack

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| **Frontend Framework** | React + Vite | 18.3 / 5.x | Single Page Application framework with fast HMR and optimized production bundling. |
| **State & HTTP** | React Context + Axios | 1.7.x | Centralized authentication, scheme, vault state management, and HTTP API client. |
| **Styling & Icons** | Tailwind CSS + Lucide React | 3.4 / 0.38 | Responsive modern design system with full dark/light theme support. |
| **Backend Framework** | Spring Boot | 3.3.2 | Enterprise Java microservice framework with dependency injection and embedded Tomcat. |
| **Security & Auth** | Spring Security + JJWT | 6.x / 0.12.6 | Stateless JWT authorization, role-based filters, and BCrypt password encryption. |
| **Relational Database** | Oracle Database XE | 21c | High-reliability ACID persistence for user authentication and authorization credentials. |
| **Document Database** | MongoDB | 7.0 | Scalable schema-less JSON storage for diverse scheme criteria and application payloads. |
| **Machine Learning** | Python / scikit-learn / FastAPI | 3.11 / 1.4 | Model training pipeline, Random Forest scheme classifier, and vector search API. |
| **Embeddings & NLP** | Sentence-Transformers | 2.5.x | 384-dimensional dense semantic text embeddings (`all-MiniLM-L6-v2`). |
| **Document OCR** | Apache PDFBox / Tesseract | 3.0.2 / 5.x | Optical character recognition and metadata parsing from uploaded citizen documents. |
| **Containerization** | Docker & Docker Compose | 24.x / 3.8 | Reproducible multi-container runtime across development, staging, and production. |
| **CI / CD Pipeline** | Jenkins | 2.440+ | Automated multi-stage build, test execution, container creation, and zero-downtime deployment. |

### 3.4 Hardware Component Specification (Hybrid Projects Only)
**Not Applicable.** SchemeBridge is a 100% Software-only digital governance solution. It requires no physical microcontrollers, sensors, or external hardware actuators.

### 3.5 Data Flow Description

```
[Citizen Action] ──► [React Frontend] ──► [Nginx Proxy / Gateway] ──► [Spring Boot Service]
                                                                            │
      ┌─────────────────────────┬───────────────────────────────────────────┤
      ▼                         ▼                                           ▼
[Oracle / Mongo DB]    [AST Rules Engine]                          [FastAPI AI Service]
(Persist Record)       (Deterministic Check)                       (ML Category / Vectors)
```

1. **User Action:** Citizen inputs query or submits application on the React SPA frontend.
2. **Gateway Ingress:** Axios sends HTTP request with `Authorization: Bearer <JWT>` header to Nginx reverse proxy on port 80.
3. **Route & Auth Filter:** Nginx forwards request to appropriate Spring Boot microservice; `JwtAuthenticationFilter` intercepts, validates token signature, and populates `SecurityContext`.
4. **Business Execution:** Spring Boot Service invokes business logic (e.g., AST rules engine, database query).
5. **AI Subsystem Delegation:** When semantic search or ML categorization is requested, the Spring Boot service calls the Python FastAPI endpoint via high-speed HTTP client.
6. **Persistence:** State updates (new application, document upload, status transition) are persisted to MongoDB or Oracle DB.
7. **Response Packaging:** Structured REST JSON response (`ApiResponse<T>`) is constructed and returned to the client.
8. **UI State Update:** React Context receives response, updates UI components reactively, and displays toast notifications or updated dashboard KPIs.

---

## 4. EXTERNAL INTERFACE REQUIREMENTS

### 4.1 User Interfaces

| # | Screen / Page | User Role | Description & Key Elements |
|---|---|---|---|
| **1** | **Landing / Home Portal** | Public / Unauthenticated | Hero banner with bilingual toggle, quick category browser, featured welfare schemes, search bar, and statistics counter. |
| **2** | **Citizen Login & Registration** | Public | Mobile/Email input, OTP verification modal, password input with strength indicator, and role-based redirect. |
| **3** | **Citizen Profile Onboarding** | Citizen (`ROLE_USER`) | Multi-step interactive wizard collecting age, gender, caste, income, domicile state/district, occupation, and family size. |
| **4** | **Personalized Dashboard** | Citizen (`ROLE_USER`) | Summary cards: Eligible Schemes count, Active Applications, Vault Document Status, and ML Recommendation carousel. |
| **5** | **Scheme Explorer & Search** | Citizen / Public | Faceted search with filters (Category, State, Gender, Income Cap), natural language search bar, and scheme comparison cards. |
| **6** | **Scheme Details & AST Checker** | Citizen (`ROLE_USER`) | Comprehensive scheme guidelines, benefit structure, required documents checklist, and interactive "Check My Eligibility" widget. |
| **7** | **4-Step Application Wizard** | Citizen (`ROLE_USER`) | Step 1: Pre-filled Profile Review; Step 2: Scheme Specific Data; Step 3: Vault Document Selection; Step 4: Final Declaration & Submit. |
| **8** | **Digital Document Vault** | Citizen (`ROLE_USER`) | Upload interface for PDF/JPG certificates, OCR verification status badge (`VERIFIED`, `PENDING`, `FLAGGED`), and document preview. |
| **9** | **Application Tracking & Grievance** | Citizen (`ROLE_USER`) | Visual progression timeline (`SUBMITTED` ➔ `REVIEW` ➔ `APPROVED`), download official acknowledgement receipt, and file grievance ticket. |
| **10** | **Admin Review & Verification Desk** | Officer / Admin | Split-pane view: Left pane shows application metadata & OCR extracted fields; right pane shows uploaded certificate preview with Approve/Reject/Request Info controls. |
| **11** | **Admin Analytics & Audit Dashboard**| Admin (`ROLE_ADMIN`) | Real-time charts (application volume by district, approval/rejection ratios, SLA compliance), scheme CRUD editor, and audit log inspector. |

### 4.2 Hardware Interfaces
**Not Applicable** (Software-only project).

### 4.3 Software Interfaces — Third-Party APIs

| API / Service | Provider | Auth Method | Purpose in This Project |
|---|---|---|---|
| **FastAPI ML Inference API** | Internal Microservice | Shared Secret Header | Serves Random Forest category predictions and dense semantic vector searches. |
| **Spring Mail (SMTP)** | Gmail / SendGrid SMTP | App Password / TLS | Dispatches email verification codes, application status alerts, and password reset tokens. |
| **Apache PDFBox & Tesseract** | Open Source Libraries | Local Binaries | Extracts text, dates, certificate IDs, and names from uploaded PDF/image documents. |
| **OpenStreetMap & Leaflet.js** | OpenStreetMap (Free) | None Required | Interactive map visualizer for district-wise scheme saturation and citizen kiosk location. |

### 4.4 Communication Interfaces
- **Stateless REST over HTTPS (TLS 1.3):** Primary protocol for all client-to-server and inter-service HTTP communications.
- **JWT Bearer Authentication:** Enforced in `Authorization: Bearer <token>` HTTP header for all protected API resources.
- **WebSocket (STOMP over SockJS):** Real-time administrative notifications and live citizen application status progression updates.

---

## 5. FUNCTIONAL REQUIREMENTS

### 5.1 Authentication & Profile Microservice

#### FR-AUTH-001: Citizen Registration and Account Creation
- **Actor(s):** New Citizen (`ROLE_USER`)
- **Description:** System shall register new users with email, mobile, password, full name, and demographic identifiers.
- **Pre-condition:** User is not already registered with the specified email or mobile number.
- **Main Flow:**
  1. User navigates to `/signup` and enters name, email, mobile number, and password.
  2. Spring Boot validates input format (RFC 5322 email, 10-digit Indian mobile, password ≥ 8 chars with mixed case/symbols).
  3. System checks Oracle DB for uniqueness.
  4. System hashes password with BCrypt (cost factor 12) and persists user in `PENDING_VERIFICATION` status.
  5. System generates 6-digit numeric OTP with 10-minute expiry and sends via Email/SMS.
  6. User inputs OTP on `/otp-verification`.
  7. System marks user `ACTIVE`, creates an empty `CitizenProfile` record, issues signed JWT, and redirects to onboarding.
- **Alternate Flow:**
  - *AF1 (Duplicate):* If email/mobile exists, system returns HTTP 409 Conflict with `"Account already registered. Please sign in."`
  - *AF2 (Invalid OTP):* If entered OTP is wrong, system decrements attempt counter; locks registration after 3 failed attempts for 15 minutes.
- **Post-condition:** User record exists in Oracle DB with status `ACTIVE`; valid JWT issued.

#### FR-AUTH-002: User Login and Stateless JWT Issuance
- **Actor(s):** Registered User (Citizen, Officer, Admin)
- **Description:** Authenticates credentials and issues cryptographically signed JWT with embedded roles and expiry.
- **Pre-condition:** User account exists and status is `ACTIVE`.
- **Main Flow:**
  1. User posts credentials to `POST /api/auth/login`.
  2. Spring Security `AuthenticationManager` invokes `CustomUserDetailsService`.
  3. System matches entered plaintext password against stored BCrypt hash.
  4. System generates access token (valid 2 hours) and refresh token (valid 7 days).
  5. System returns tokens and role permissions in response payload; client stores in secure storage.
- **Alternate Flow:**
  - *AF1 (Bad Credentials):* Invalid username/password returns HTTP 401 Unauthorized with `"Invalid email or password"`.
  - *AF2 (Account Locked):* Locked user returns HTTP 403 Forbidden with `"Account suspended. Contact administrator"`.
- **Post-condition:** User authenticated; subsequent requests authenticated via `Authorization: Bearer <token>`.

---

### 5.2 Scheme & Eligibility Microservice

#### FR-SCH-001: Scheme Catalog Exploration & Full-Text Search
- **Actor(s):** Any User / Citizen
- **Description:** System shall provide faceted search and filtering across 4,734+ government welfare schemes.
- **Pre-condition:** Scheme database is seeded and indexed.
- **Main Flow:**
  1. User navigates to `/schemes` and provides search keyword or selects filter facets (Category, Domicile State, Gender, Min/Max Age, Income Threshold).
  2. Client sends request to `GET /api/schemes?keyword=...&category=...&state=...`.
  3. Spring Boot builds dynamic MongoDB compound query using indexed criteria.
  4. Matching schemes returned with paginated metadata (total schemes, pages, eligibility summaries).
- **Alternate Flow:**
  - *AF1 (Zero matches):* If no exact match found, system triggers fallback semantic vector search to suggest conceptually similar welfare programs.
- **Post-condition:** Paginated list of schemes rendered on UI.

#### FR-SCH-002: Deterministic AST Statutory Eligibility Evaluation
- **Actor(s):** Citizen (`ROLE_USER`)
- **Description:** System evaluates citizen's verified profile against a scheme's Boolean logic tree and returns eligibility status with line-by-line statutory breakdown.
- **Pre-condition:** Citizen has completed onboarding profile; target scheme ID is valid.
- **Main Flow:**
  1. Citizen clicks "Check Eligibility" on scheme details page.
  2. Frontend sends request to `POST /api/eligibility/evaluate/{schemeId}`.
  3. `EligibilityEngine.java` retrieves citizen profile attributes from MongoDB.
  4. Engine parses scheme's nested AST rule groups (evaluating `AND`, `OR`, `NOT` operators against parameters: Age, Gender, Income, Landholding, Caste, Domicile, Employment).
  5. Engine calculates `eligibilityStatus` (`ELIGIBLE`, `INELIGIBLE`, `PARTIALLY_ELIGIBLE`) and constructs an array of condition verdicts with clear explanations.
  6. Result returned to client and rendered in interactive eligibility card with green checkmarks or red cross indicators.
- **Alternate Flow:**
  - *AF1 (Missing Profile Attribute):* If citizen profile lacks a required comparison field (e.g., land ownership size), status returns `INSUFFICIENT_DATA` with a direct link to update profile.
- **Post-condition:** Evaluation event logged to database; citizen informed of exact legal qualification status.

---

### 5.3 Digital Vault & Document Verification Microservice

#### FR-DOC-001: Citizen Document Upload and Vault Storage
- **Actor(s):** Citizen (`ROLE_USER`)
- **Description:** Allows citizens to securely upload, tag, and store certificates in personal encrypted vault.
- **Pre-condition:** Document format is PDF, PNG, or JPEG; file size ≤ 5 MB.
- **Main Flow:**
  1. Citizen opens `/documents` and clicks "Upload Document".
  2. Citizen selects document type (`AADHAAR`, `INCOME_CERTIFICATE`, `COMMUNITY_CERTIFICATE`, `DOMICILE`, `RATION_CARD`) and attaches file.
  3. Spring Boot validates MIME type and file size.
  4. System computes SHA-256 checksum, generates unique UUID storage path, and saves file.
  5. Metadata saved to MongoDB `citizenVaultDocuments` collection with initial status `PENDING_OCR`.
- **Alternate Flow:**
  - *AF1 (File too large):* Returns HTTP 413 Payload Too Large with `"Document exceeds 5MB size limit"`.
  - *AF2 (Unsupported format):* Returns HTTP 415 Unsupported Media Type.
- **Post-condition:** Document record stored in vault; asynchronous OCR extraction job dispatched.

#### FR-DOC-002: Automated OCR Text Extraction & Cross-Verification
- **Actor(s):** AI System Agent / Verification Engine
- **Description:** System extracts text from uploaded certificates using OCR and matches citizen name, DOB, and certificate number against profile.
- **Pre-condition:** Document successfully uploaded to vault.
- **Main Flow:**
  1. OCR Engine parses document image/PDF via Apache PDFBox / Tesseract.
  2. Regex pattern matchers locate Certificate Number, Issue Date, Authority Stamp, and Applicant Name.
  3. System compares extracted Name and DOB with citizen's profile using Levenshtein distance string similarity.
  4. If similarity ≥ 85%, document status is upgraded to `AUTO_VERIFIED` with high confidence score.
  5. If similarity < 85%, document is marked `FLAGGED_FOR_MANUAL_REVIEW`.
- **Alternate Flow:**
  - *AF1 (Unreadable Document):* If OCR confidence < 40% (blurred or corrupt scan), document status updated to `REJECTED_UNREADABLE`, and user notified to re-upload clear image.
- **Post-condition:** Extracted metadata saved to document entity; ready for scheme application auto-attachment.

---

### 5.4 Application & Grievance Microservice

#### FR-APP-001: End-to-End Scheme Application Submission
- **Actor(s):** Citizen (`ROLE_USER`)
- **Description:** Submits completed application for an eligible welfare scheme with attached vault documents.
- **Pre-condition:** Citizen evaluated as `ELIGIBLE` for scheme; required mandatory documents uploaded.
- **Main Flow:**
  1. Citizen launches Application Wizard from `/scheme/{id}/apply`.
  2. Step 1: Pre-populated demographic profile verified by user.
  3. Step 2: User completes scheme-specific questionnaires (e.g., bank account IFSC, crop type, scholarship grade).
  4. Step 3: User attaches required documents from their digital vault.
  5. Step 4: User reviews declaration and submits.
  6. Backend generates unique alphanumeric tracking number (e.g., `APP-2026-89421`), saves application in `SUBMITTED` state, and emits application submission event.
  7. Downloadable PDF acknowledgement receipt generated with QR code verification.
- **Alternate Flow:**
  - *AF1 (Missing Required Document):* Form prevents submission and highlights missing mandatory certificates.
- **Post-condition:** Application record persisted in MongoDB; visible in citizen tracker and admin review queues.

#### FR-APP-002: Citizen Grievance Filing and SLA Escalation
- **Actor(s):** Citizen (`ROLE_USER`)
- **Description:** Enables citizens to lodge formal grievances regarding application delays or unjust rejections.
- **Pre-condition:** Citizen has an application in `SUBMITTED`, `UNDER_REVIEW`, or `REJECTED` state.
- **Main Flow:**
  1. Citizen navigates to `/tracker`, selects application, and clicks "File Grievance".
  2. Citizen specifies grievance type (`DELAYED_REVIEW`, `DOCUMENT_DISPUTE`, `DISBURSEMENT_ISSUE`), enters detailed description, and submits.
  3. System assigns tracking ticket `GRV-XXXXXX`, logs timestamp, and sets statutory SLA deadline (e.g., 7 working days).
  4. Grievance escalated to Departmental Officer review queue.
- **Alternate Flow:**
  - *AF1 (SLA Breached):* If grievance remains unresolved after 7 days, automated background cron job triggers severity bump to `ESCALATED_TO_SUPERVISOR`.
- **Post-condition:** Grievance record created; citizen receives SMS/Email notification with tracking ticket.

---

### 5.5 Administrative Review & Audit Microservice

#### FR-ADM-001: Officer Application Review & Benefit Disbursement
- **Actor(s):** Verification Officer (`ROLE_VERIFICATION_OFFICER`) / Admin
- **Description:** Allows government officers to inspect applications, verify supporting documents, and issue approval or rejection.
- **Pre-condition:** Officer authenticated with administrative credentials; applications exist in `SUBMITTED` state.
- **Main Flow:**
  1. Officer accesses `/admin/review` and opens pending application.
  2. Screen renders applicant demographic breakdown, AST eligibility checklist, and embedded certificate preview.
  3. Officer reviews documents and clicks "Approve Application" or "Reject Application" (entering mandatory rejection justification remarks).
  4. Application status transitioned to `APPROVED` or `REJECTED`.
  5. Audit log entry recorded in `admin_audit_logs` collection capturing Officer ID, action, timestamp, and IP address.
  6. Real-time push notification and email sent to citizen.
- **Alternate Flow:**
  - *AF1 (Insufficient Information):* Officer clicks "Request Information"; application placed in `ACTION_REQUIRED` state; citizen notified to upload clarification.
- **Post-condition:** Application status updated; immutable audit record recorded.

---

## 6. AI / ML MODULE SPECIFICATION

### 6.1 AI Module 1 — Citizen Welfare Scheme Recommendation Model (Primary AI Feature)

| Attribute | Specification Details |
|---|---|
| **Module Name** | **Citizen Welfare Scheme Recommendation Model** |
| **AI Phase** | **Phase 1 (Plan) ➔ Phase 2 (Prototype) ➔ Phase 3 (Build) ➔ Phase 4 (Integrate) ➔ Phase 5 (Deploy) ➔ Phase 6 (Present)** |
| **Problem AI Solves** | Bridges discovery gap by instantly analyzing 9 multidimensional demographic/socioeconomic attributes of a citizen to accurately classify and recommend the most suitable welfare program category, eliminating manual portal searching. |
| **Input Data** | **Structured 9-feature JSON citizen profile:**<br>1. `age` (int: 18–75)<br>2. `gender` (string: `Female`, `Male`, `Transgender`)<br>3. `state` (string: 10 major Indian states)<br>4. `income` (float: annual household income in INR, ₹30,000–₹1,200,000)<br>5. `occupation` (string: `Student`, `Farmer`, `Artisan`, `Daily Wage Worker`, `Self-Employed`, `Salaried`, `Unemployed`)<br>6. `category` (string: `General`, `OBC`, `SC`, `ST`, `EWS`)<br>7. `rural` (string: `Yes`, `No`)<br>8. `student` (string: `Yes`, `No`)<br>9. `disability` (string: `Yes`, `No`) |
| **Output / Prediction** | **Target Scheme Category (One of 7 discrete classes) with model confidence probability (0.0 to 1.0):**<br>1. `Agriculture & Farmer Welfare`<br>2. `Education & Scholarships`<br>3. `Women & Child Development`<br>4. `Social Welfare & Disability Support`<br>5. `Senior Citizen Pension & Healthcare`<br>6. `Skill Development & Entrepreneurship`<br>7. `Rural & Urban Livelihood Support` |
| **Algorithm / Model** | **scikit-learn `RandomForestClassifier`** with 200 decision trees, `max_depth=16`, `min_samples_split=4`, `min_samples_leaf=2`, `class_weight="balanced"`. |
| **Training Dataset** | **Synthetic Citizen Welfare Dataset** (`synthetic_citizen_eligibility_dataset.csv`), 5,000 records modeled on Indian demographic distributions and statutory scheme criteria with 1.5% calibrated stochastic noise. |
| **Implementation Approach** | **[X] scikit-learn (local training)** &nbsp;&nbsp;&nbsp;&nbsp; [ ] TensorFlow/Keras &nbsp;&nbsp;&nbsp;&nbsp; [ ] Hosted API |
| **Integration Point** | Python FastAPI endpoint: `POST /predict/recommend-category`<br>Called by Spring Boot `schemebridge-scheme-service` using `RestClient` / `WebClient`. |
| **Expected Accuracy / Metric** | **Accuracy: 98.40%** (0.9840) \| **Weighted Precision: 98.42%** \| **Weighted Recall: 98.40%** \| **Weighted F1 Score: 98.40%** \| Latency: 21.6 µs/sample. |
| **Fallback if AI Fails** | If model confidence < 60% or API unreachable, system gracefully falls back to deterministic AST rule matching on citizen's primary occupation & age. |
| **AI Ethical Considerations** | **Human-in-the-Loop & Advisory Only:** Model output is advisory for discovery; all statutory qualifications are verified by deterministic code and human officers. Balanced class weighting applied during training to prevent demographic discrimination. |
| **Sprint Target** | **Week 3:** Standalone prototype \| **Week 4:** FastAPI serving \| **Week 5:** Spring Boot integration \| **Week 6:** React UI integration \| **Week 7:** End-to-end load testing. |

---

### 6.2 AI Module 2 — Dense Semantic Vector Search & Conversational Assistant (Secondary AI Feature)

| Attribute | Specification Details |
|---|---|
| **Module Name** | **Dense Semantic Vector Scheme Discovery & Conversational AI Assistant** |
| **AI Phase** | **Phase 1 (Plan) ➔ Phase 2 (Prototype) ➔ Phase 3 (Build) ➔ Phase 4 (Integrate) ➔ Phase 5 (Deploy) ➔ Phase 6 (Present)** |
| **Problem AI Solves** | Traditional keyword search fails when citizens search using colloquial, dialectical, or informal terms (e.g. "subsidy for buying dairy buffaloes" vs official title "Pashu Kisan Credit Card"). This module bridges semantic intent. |
| **Input Data** | Unstructured natural language query text (English / Romanized vernacular) entered by citizen in search bar or conversational chat assistant. |
| **Output / Prediction** | Ranked list of scheme IDs ordered by Cosine Similarity score (0.000 to 1.000) + Conversational natural language synthesis of scheme requirements. |
| **Algorithm / Model** | **Sentence-Transformers `all-MiniLM-L6-v2`** producing 384-dimensional dense semantic vector embeddings + TruncatedSVD projection + Cosine Dot-Product ranking. |
| **Training / Corpus Dataset** | Full authoritative corpus of **4,734 Master Government Schemes** (`master_schemes_4734.json`) indexed across scheme names, benefits, eligibility summaries, and target beneficiaries. |
| **Implementation Approach** | **[X] Python FastAPI + sentence-transformers (local embedding index)** &nbsp;&nbsp;&nbsp;&nbsp; [X] OpenAI GPT-4o / Local RAG for conversational synthesis. |
| **Integration Point** | Python FastAPI endpoint: `POST /api/v1/ai/semantic-search` & `POST /api/ai/citizen/chat`<br>Called by Spring Boot `AiChatController` & `SchemeController`. |
| **Expected Accuracy / Metric** | **Mean Reciprocal Rank (MRR@10) > 0.85** on standard citizen queries; semantic query inference latency < 45 ms. |
| **Fallback if AI Fails** | If vector index service is unavailable, Spring Boot automatically degrades to MongoDB text index regex matching (`$text` search on `schemeName` and `description`). |
| **AI Ethical Considerations** | Complete PII protection: No citizen personal data or uploaded documents are ever sent to external LLMs; queries are sanitized; answers provide direct citations to official gazette notices. |
| **Sprint Target** | **Week 4:** Vector index compilation \| **Week 5:** FastAPI microservice \| **Week 6:** React chat assistant UI \| **Week 7:** Full integration testing. |

---

## 7. NON-FUNCTIONAL REQUIREMENTS

| NFR ID | Category | NFR Name | Measurable Requirement |
|---|---|---|---|
| **NFR-PERF-001** | Performance | API Response Time | All standard REST API endpoints must respond in **< 500 ms** under nominal load; 99th percentile response time must be **< 1.5 seconds** at 100 concurrent requests. |
| **NFR-PERF-002** | Performance | AI Inference Latency | Standalone Random Forest model prediction must complete in **< 50 ms**; Dense semantic vector search over 4,734 schemes must complete in **< 100 ms**. |
| **NFR-PERF-003** | Performance | UI Page Load Time | React frontend dashboard must achieve First Contentful Paint (FCP) **< 1.2s** and fully interactive state **< 2.5s** on standard 4G mobile connections. |
| **NFR-SEC-001** | Security | Stateless Authentication | All non-public API endpoints must enforce cryptographically signed JWTs (HMAC-SHA256) with 2-hour token expiration and refresh token rotation. |
| **NFR-SEC-002** | Security | Password Encryption | User passwords must be salted and hashed using **BCrypt with cost factor 12**; plaintext passwords must never be logged or stored. |
| **NFR-SEC-003** | Security | Input Validation & Sanitization | All client payloads must be validated against XSS and SQL/NoSQL injection using Spring Boot `jakarta.validation` annotations and parameterized queries. |
| **NFR-SEC-004** | Security | Data Encryption at Rest | Uploaded citizen identity certificates and sensitive PII must be encrypted at rest using **AES-256** encryption standards. |
| **NFR-SCAL-001** | Scalability | Concurrency | System must support a minimum of **250 concurrent authenticated users** without connection pool exhaustion or memory leaks. |
| **NFR-REL-001** | Reliability | System Availability | System shall maintain **99.5% uptime** during operational hours, supported by automated Docker container restarts (`restart: unless-stopped`). |
| **NFR-REL-002** | Reliability | Graceful Fault Recovery | In the event of AI microservice downtime, the core portal must continue functioning normally using deterministic rule fallbacks without 500 errors. |
| **NFR-USE-001** | Usability | Responsive Mobile Design | All screens must render responsively across devices down to 375px viewport width (iPhone SE standard) with single-thumb interactive accessibility. |
| **NFR-USE-002** | Usability | Accessibility Compliance | User interface components must adhere to **WCAG 2.1 Level AA** standards, including color contrast ratio ≥ 4.5:1, screen reader tags, and keyboard tab navigation. |
| **NFR-MAINT-001**| Maintainability| Layered Clean Architecture| Codebase must strictly preserve the layered design pattern (`Controller ➔ Service ➔ Repository`) with zero business logic implemented in Controllers. |
| **NFR-MAINT-002**| Maintainability| API Documentation | All REST endpoints must be annotated and accessible via interactive Swagger / OpenAPI 3.0 documentation at `/swagger-ui.html`. |
| **NFR-COMP-001** | Compliance | Digital Personal Data Protection | Platform must comply with the Indian **Digital Personal Data Protection Act (DPDPA 2023)**; citizens must have clear consent mechanisms and right-to-forget controls. |

---

## 8. DATABASE DESIGN

### 8.1 Entity-Relationship Overview

SchemeBridge employs a polyglot persistence architecture:
1. **Oracle Database XE:** Houses security-critical identity data (`USERS`, `ROLES`, `REFRESH_TOKENS`, `OTP_VERIFICATIONS`).
2. **MongoDB 7.0:** Houses high-volume, dynamic welfare schema entities (`CITIZEN_PROFILES`, `SCHEMES`, `SCHEME_CATEGORIES`, `APPLICATIONS`, `CITIZEN_VAULT_DOCUMENTS`, `GRIEVANCES`, `ADMIN_AUDIT_LOGS`).

![SchemeBridge Entity Relationship Diagram](ER_DIAGRAM.png)

> **PlantUML Source Specification:** Available at [`ER_DIAGRAM.puml`](file:///e:/SCHEMEBRIDGE/ER_DIAGRAM.puml).

```mermaid
erDiagram
    USER ||--o{ REFRESH_TOKEN : owns
    USER ||--o{ OTP_VERIFICATION : verifies
    USER ||--|| CITIZEN_PROFILE : defines
    CITIZEN_PROFILE ||--o{ CITIZEN_VAULT_DOCUMENT : uploads
    CITIZEN_PROFILE ||--o{ APPLICATION : submits
    SCHEME ||--o{ APPLICATION : receives
    APPLICATION ||--o{ GRIEVANCE : escalates
    APPLICATION ||--o{ APPLICATION_EVENT : logs
    USER ||--o{ ADMIN_AUDIT_LOG : audits

    USER {
        bigint id PK
        string email UK
        string mobile UK
        string password_hash
        string role
        string status
        datetime created_at
    }

    CITIZEN_PROFILE {
        string id PK
        bigint user_id FK
        int age
        string gender
        string state
        string district
        double annual_income
        string caste_category
        string occupation
        boolean is_rural
        boolean is_student
        boolean is_disabled
    }

    SCHEME {
        string id PK
        string scheme_code UK
        string scheme_name
        string ministry
        string category
        string level
        object eligibility_rules_ast
        array required_documents
        string benefit_summary
    }

    APPLICATION {
        string id PK
        string application_number UK
        string citizen_profile_id FK
        string scheme_id FK
        string status
        datetime submission_date
        array attached_vault_documents
    }

    CITIZEN_VAULT_DOCUMENT {
        string id PK
        string citizen_profile_id FK
        string document_type
        string file_storage_path
        string ocr_status
        string extracted_certificate_number
        double ocr_confidence
    }

    GRIEVANCE {
        string id PK
        string application_id FK
        string ticket_number UK
        string grievance_type
        string status
        string priority
        datetime sla_deadline
    }
```

### 8.2 Entity Descriptions

#### ENTITY 1: User
- **Service:** `schemebridge-auth-service`
- **Database:** Oracle Database 21c XE
- **Table Name:** `USERS`

| Column Name | Data Type | Constraints | Description |
|---|---|---|---|
| `id` | `NUMBER(19)` | PRIMARY KEY, AUTO_INCREMENT | Unique surrogate identifier for the user account. |
| `email` | `VARCHAR2(150)` | NOT NULL, UNIQUE | Citizen/Admin primary email address used for login. |
| `mobile` | `VARCHAR2(15)` | NOT NULL, UNIQUE | 10-digit Indian mobile number for OTP authentication. |
| `password_hash` | `VARCHAR2(255)` | NOT NULL | BCrypt cryptographic hash of the user password. |
| `role` | `VARCHAR2(30)` | NOT NULL | User authorization role (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_VERIFICATION_OFFICER`). |
| `status` | `VARCHAR2(30)` | NOT NULL, DEFAULT 'ACTIVE' | Account state (`ACTIVE`, `PENDING_VERIFICATION`, `SUSPENDED`). |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Account creation timestamp. |
| `last_login` | `TIMESTAMP` | NULLABLE | Timestamp of the most recent successful login session. |

---

#### ENTITY 2: CitizenProfile
- **Service:** `schemebridge-scheme-service`
- **Database:** MongoDB 7.0
- **Collection Name:** `citizen_profiles`

| Field Name | Data Type | Constraints | Description |
|---|---|---|---|
| `_id` | `ObjectId / String` | PRIMARY KEY | Unique identifier for citizen demographic profile. |
| `userId` | `Long / Number` | INDEXED, NOT NULL | Foreign reference to Oracle `USERS.id`. |
| `fullName` | `String` | NOT NULL | Full name of citizen as recorded on official identity card. |
| `age` | `Integer` | NOT NULL (Min: 0, Max: 120) | Citizen age in years (used for AST gatekeeping). |
| `gender` | `String` | NOT NULL | Legal gender (`MALE`, `FEMALE`, `TRANSGENDER`). |
| `state` | `String` | NOT NULL, INDEXED | State of permanent domicile (e.g., "Tamil Nadu"). |
| `district` | `String` | NOT NULL | District of domicile (e.g., "Coimbatore"). |
| `annualIncome` | `Double` | NOT NULL | Annual household income in Indian Rupees (INR). |
| `casteCategory` | `String` | NOT NULL | Reservation category (`GENERAL`, `OBC`, `SC`, `ST`, `EWS`). |
| `occupation` | `String` | NOT NULL | Primary livelihood (`FARMER`, `STUDENT`, `ARTISAN`, etc.). |
| `isRural` | `Boolean` | NOT NULL | True if domicile is classified under rural administration. |
| `isStudent` | `Boolean` | NOT NULL | True if currently enrolled in recognized educational institution. |
| `isDisabled` | `Boolean` | NOT NULL | True if certified Person with Benchmark Disability (PwD). |

---

#### ENTITY 3: Scheme
- **Service:** `schemebridge-scheme-service`
- **Database:** MongoDB 7.0
- **Collection Name:** `schemes`

| Field Name | Data Type | Constraints | Description |
|---|---|---|---|
| `_id` | `ObjectId / String` | PRIMARY KEY | MongoDB internal object identifier. |
| `schemeCode` | `String` | UNIQUE, INDEXED, NOT NULL | Standardized government code (e.g., "PM-KISAN-001"). |
| `schemeName` | `String` | NOT NULL, TEXT INDEXED | Official scheme title. |
| `ministry` | `String` | NOT NULL, INDEXED | Administering Ministry or State Department. |
| `category` | `String` | NOT NULL, INDEXED | Scheme welfare category (e.g., "Agriculture & Farmer Welfare"). |
| `level` | `String` | NOT NULL | Scope of scheme (`CENTRAL`, `STATE`). |
| `eligibilityRulesAst` | `Object / Document` | NOT NULL | JSON Abstract Syntax Tree defining Boolean eligibility rules. |
| `requiredDocuments` | `Array<String>` | NOT NULL | List of required supporting document types. |
| `benefits` | `Object / Document` | NOT NULL | Detailed financial or non-financial welfare benefits. |
| `isActive` | `Boolean` | NOT NULL, DEFAULT true | Current publication status of scheme. |

---

#### ENTITY 4: Application
- **Service:** `schemebridge-scheme-service`
- **Database:** MongoDB 7.0
- **Collection Name:** `applications`

| Field Name | Data Type | Constraints | Description |
|---|---|---|---|
| `_id` | `ObjectId / String` | PRIMARY KEY | Unique database record identifier. |
| `applicationNumber` | `String` | UNIQUE, INDEXED, NOT NULL | Citizen-facing tracking ID (e.g., "APP-2026-91823"). |
| `citizenProfileId` | `String` | NOT NULL, INDEXED | Reference to applicant's `citizen_profiles._id`. |
| `schemeId` | `String` | NOT NULL, INDEXED | Reference to target `schemes._id`. |
| `status` | `String` | NOT NULL, INDEXED | Progression status (`SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`). |
| `submissionDate` | `Date` | NOT NULL | Timestamp when application was filed. |
| `attachedDocuments` | `Array<Document>` | NOT NULL | References to vault documents with verified hashes. |
| `reviewRemarks` | `String` | NULLABLE | Official review comments or rejection justifications. |

---

#### ENTITY 5: CitizenVaultDocument
- **Service:** `schemebridge-scheme-service`
- **Database:** MongoDB 7.0
- **Collection Name:** `citizen_vault_documents`

| Field Name | Data Type | Constraints | Description |
|---|---|---|---|
| `_id` | `ObjectId / String` | PRIMARY KEY | Unique document identifier. |
| `citizenProfileId` | `String` | NOT NULL, INDEXED | Reference to owning citizen. |
| `documentType` | `String` | NOT NULL | Type (`AADHAAR`, `INCOME_CERTIFICATE`, `COMMUNITY`, etc.). |
| `storagePath` | `String` | NOT NULL | Internal secure file system or object store path. |
| `fileChecksum` | `String` | NOT NULL | SHA-256 integrity hash of document content. |
| `ocrStatus` | `String` | NOT NULL | OCR state (`PENDING`, `AUTO_VERIFIED`, `FLAGGED`, `FAILED`). |
| `ocrExtractedData` | `Object` | NULLABLE | Parsed JSON attributes (certificate number, holder name, DOB). |
| `confidenceScore` | `Double` | NULLABLE | Machine learning extraction confidence (0.0 to 1.0). |

---

#### ENTITY 6: Grievance
- **Service:** `schemebridge-scheme-service`
- **Database:** MongoDB 7.0
- **Collection Name:** `grievances`

| Field Name | Data Type | Constraints | Description |
|---|---|---|---|
| `_id` | `ObjectId / String` | PRIMARY KEY | Unique grievance record ID. |
| `ticketNumber` | `String` | UNIQUE, INDEXED, NOT NULL | Citizen tracking ticket (e.g., "GRV-2026-4412"). |
| `applicationId` | `String` | NOT NULL, INDEXED | Reference to contested `applications._id`. |
| `citizenProfileId` | `String` | NOT NULL | Reference to citizen filing dispute. |
| `grievanceType` | `String` | NOT NULL | Category (`DELAYED_REVIEW`, `WRONGFUL_REJECTION`, `DISBURSEMENT`). |
| `status` | `String` | NOT NULL | Status (`OPEN`, `INVESTIGATING`, `RESOLVED`, `ESCALATED`). |
| `slaDeadline` | `Date` | NOT NULL | Statutory deadline for administrative resolution. |

---

## 9. API DESIGN OVERVIEW

### 9.1 Authentication Service APIs (Port 8080)

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | No | Register new citizen/officer; sends 6-digit OTP. |
| `POST` | `/api/auth/verify-otp` | No | Verifies registration OTP and activates account. |
| `POST` | `/api/auth/login` | No | Authenticates credentials; returns signed JWT and refresh token. |
| `POST` | `/api/auth/refresh-token` | Yes (Refresh Token) | Exchanges valid refresh token for fresh access JWT. |
| `GET` | `/api/auth/me` | Yes (`ROLE_USER`/`ADMIN`) | Fetches session profile of currently authenticated user. |
| `POST` | `/api/auth/forgot-password` | No | Dispatches password reset link with secure single-use token. |
| `POST` | `/api/auth/reset-password` | No | Resets password upon presentation of valid reset token. |

### 9.2 Core Scheme & Eligibility Service APIs (Port 8081)

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `GET` | `/api/schemes` | No | Retrieves paginated list of schemes with keyword and facet filters. |
| `GET` | `/api/schemes/{id}` | No | Fetches complete scheme guidelines, AST rules, and benefits. |
| `POST` | `/api/eligibility/evaluate/{schemeId}` | Yes (`ROLE_USER`) | Executes deterministic AST evaluation against citizen's profile. |
| `GET` | `/api/recommendations` | Yes (`ROLE_USER`) | Returns personalized eligible schemes for logged-in citizen. |
| `GET` | `/api/citizen/profile` | Yes (`ROLE_USER`) | Retrieves citizen's 6-step demographic profile. |
| `PUT` | `/api/citizen/profile` | Yes (`ROLE_USER`) | Updates citizen demographic attributes. |
| `POST` | `/api/vault/upload` | Yes (`ROLE_USER`) | Uploads certificate file to digital vault (multipart form data). |
| `GET` | `/api/vault/documents` | Yes (`ROLE_USER`) | Lists all vault certificates with OCR verification badges. |
| `POST` | `/api/applications` | Yes (`ROLE_USER`) | Submits formal application for a scheme. |
| `GET` | `/api/applications/my` | Yes (`ROLE_USER`) | Returns citizen's submitted applications with progress status. |
| `GET` | `/api/applications/{id}` | Yes (`ROLE_USER`) | Retrieves detailed application tracking timeline and receipt. |
| `POST` | `/api/grievances` | Yes (`ROLE_USER`) | Submits a grievance against an application. |

### 9.3 Admin & Verification Desk APIs (Port 8081)

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `GET` | `/api/admin/applications` | Yes (`ROLE_VERIFICATION_OFFICER`) | Fetches pending applications for officer review queue. |
| `PUT` | `/api/admin/applications/{id}/review` | Yes (`ROLE_VERIFICATION_OFFICER`) | Updates application state to `APPROVED` or `REJECTED` with remarks. |
| `POST` | `/api/admin/schemes` | Yes (`ROLE_SCHEME_MANAGER`/`ADMIN`) | Creates and publishes a new scheme into the master catalog. |
| `PUT` | `/api/admin/schemes/{id}` | Yes (`ROLE_SCHEME_MANAGER`/`ADMIN`) | Modifies existing scheme criteria and guidelines. |
| `GET` | `/api/admin/analytics/overview` | Yes (`ROLE_ADMIN`) | Returns platform KPIs, demographic saturation, and approval stats. |
| `GET` | `/api/admin/audit-logs` | Yes (`ROLE_ADMIN`) | Fetches immutable system audit trails. |

### 9.4 AI / ML Inference Service APIs (Port 8000)

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/predict/recommend-category` | Service Token | Evaluates 9 citizen demographic features via Random Forest (Returns scheme category & confidence). |
| `POST` | `/api/v1/ai/semantic-search` | Service Token | Dense semantic vector search over 4,734 schemes using sentence-transformers. |
| `POST` | `/api/ai/citizen/chat` | Service Token | Generative AI conversational assistant processing citizen questions. |
| `GET` | `/health` | No | Microservice health check returning `{"status":"ok"}`. |

---

## 10. DEVOPS & DEPLOYMENT PLAN

### 10.1 CI/CD Pipeline — Jenkins

```mermaid
flowchart LR
    A[Git Push Main] --> B[Jenkins Checkout]
    B --> C[Maven & NPM Build]
    C --> D[JUnit 5 & Vitest]
    D --> E[SonarQube Quality Gate]
    E --> F[Docker Multi-Build]
    F --> G[Docker Compose Deploy]
    G --> H[Actuator Health Ping]
    H --> I[Slack/Email Alert]
```

| Stage | Stage Name | Actions & Commands |
|---|---|---|
| **1** | **Source Checkout** | Webhook triggered on commit to `main`; Jenkins clones repository and verifies Git commit hash. |
| **2** | **Compilation & Build** | `mvn clean compile -DskipTests` for both Spring Boot microservices; `npm ci && npm run build` for React SPA. |
| **3** | **Automated Testing** | `mvn test` running JUnit 5 unit and integration tests; executes Vitest unit tests for frontend components. |
| **4** | **Code Quality Gate** | SonarQube static code analysis evaluating code coverage (> 75%), security hotspots, and cyclomatic complexity. |
| **5** | **Container Packaging** | `docker build -t schemebridge-auth:${GIT_COMMIT} ./schemebridge-auth-service`<br>`docker build -t schemebridge-scheme:${GIT_COMMIT} ./schemebridge-scheme-service`<br>`docker build -t schemebridge-frontend:${GIT_COMMIT} ./schemebridge-frontend` |
| **6** | **Container Push** | Push tagged images to private container registry / Docker Hub repository. |
| **7** | **Orchestrated Deploy** | `docker compose -f docker-compose.yml up -d --remove-orphans` on target host. |
| **8** | **Health Verification** | Curl verification scripts query `/actuator/health` on services; pipeline fails if status is not `UP`. |
| **9** | **Notification** | Automated build status alert dispatched to team Slack channel and Discord webhook. |

### 10.2 Docker Container Configuration

| Container | Base Image | Port Mapping | Depends On | Environment Variables |
|---|---|---|---|---|
| **frontend** | `nginx:alpine` | `80:80`, `443:443` | `scheme-service`, `auth-service` | `PORT=80`, `PROXY_PASS_AUTH=auth-service:8080`, `PROXY_PASS_SCHEME=scheme-service:8081` |
| **auth-service** | `eclipse-temurin:17-jre-jammy` | `8080:8080` | `oracle-db` | `SERVER_PORT=8080`, `ORACLE_URL=jdbc:oracle:thin:@oracle-db:1521/XEPDB1`, `JWT_SECRET=...` |
| **scheme-service** | `eclipse-temurin:17-jre-jammy` | `8081:8081` | `mongodb`, `ai-service` | `SERVER_PORT=8081`, `MONGODB_URI=mongodb://user:pass@mongodb:27017/schemebridge_db`, `AI_SERVICE_URL=http://ai-service:8000` |
| **ai-service** | `python:3.11-slim` | `8000:8000` | None | `MODEL_PATH=/app/models/random_forest_scheme_model.pkl`, `INDEX_PATH=/app/data/ml_models` |
| **mongodb** | `mongo:7.0` | `27017:27017` | None | `MONGO_INITDB_ROOT_USERNAME=schemebridge_user`, `MONGO_INITDB_DATABASE=schemebridge_scheme_db` |
| **oracle-db** | `gvenzl/oracle-xe:21-slim` | `1521:1521` | None | `ORACLE_PASSWORD=system_secure_pass`, `APP_USER=schemebridge_auth` |

### 10.3 Deployment Environments

| Environment | Infrastructure | Purpose & Access URL |
|---|---|---|
| **Development** | Local Workstations (Windows 11 / macOS / Ubuntu) | Feature development with live hot-reloading via Vite HMR and Spring DevTools (`http://localhost:5173`, `http://localhost:8080`). |
| **Testing / QA** | Staging Linux Virtual Machine (2 vCPU, 4GB RAM) | Automated nightly Jenkins integration test environment and mentor review (`http://staging.schemebridge.internal`). |
| **Demo / Evaluation** | Cloud VPS / AWS EC2 / Railway Cloud Container | Smart India Hackathon live demonstration and evaluation instance (`https://schemebridge.demo.gov.in`). |

---

## 11. PROJECT TIMELINE & MILESTONES

| Week | Phase | Deliverables & Tasks (All 3 Team Members) |
|---|---|---|
| **Week 1** | **Foundation & Architecture** | Complete IEEE Std 830-1998 SRS document; initialize Git repositories with branch protection; design Oracle & MongoDB schemas; containerize base services with Docker Compose; seed master scheme catalog with 4,734 records. |
| **Week 2** | **Core Backend & Auth** | Implement `schemebridge-auth-service` (BCrypt, JWT filters, OTP verification); configure Oracle database connection; create standalone Python Random Forest prototype; build initial AST rule evaluation logic in Java. |
| **Week 3** | **Full Backend Implementation** | Implement `schemebridge-scheme-service` CRUD APIs; establish MongoDB repositories for applications, vault, and grievances; finalize AST rule evaluation engine; deploy Python FastAPI Random Forest inference endpoint; execute Postman test suite. |
| **Week 4** | **Frontend Foundation** | Initialize React 18 SPA with Vite, Tailwind CSS, and Lucide icons; build authentication context and route guards (`CitizenGuard`, `AdminGuard`); implement Onboarding wizard and Scheme Explorer with dynamic filtering. |
| **Week 5** | **Frontend Completion & AI Hookup** | Implement 4-step Application Wizard, Digital Vault upload interface, and real-time status tracker; wire conversational AI assistant widget; integrate responsive mobile layouts (tested on 375px viewport). |
| **Week 6** | **Integration & System Testing** | End-to-end user journey verification (Registration ➔ Profile ➔ AST Eligibility ➔ Vault Upload ➔ Application Submission ➔ Admin Approval); execute 100+ JUnit 5 tests; configure Jenkins CI/CD pipeline. |
| **Week 7** | **Polish, Security Audit & Docs** | Comprehensive security audit (SQLi/XSS checks, rate-limiting); Swagger OpenAPI documentation finalization; record 5-minute product walkthrough demo video; conduct Day 22 Internal Review with faculty mentors. |
| **Week 8** | **Final Submission & Viva** | Incorporate faculty and industry mentor feedback; freeze release tag `v1.0`; prepare SIH slide deck, project poster, and live demonstration script; final viva rehearsal. |

---

## 12. TEAM COMPOSITION & ROLE ASSIGNMENT

### 12.1 Member Profile & Responsibilities

| Member Name | Degree / Branch | Academic Year | Primary Project Responsibilities |
|---|---|---|---|
| **Student 1 (Team Lead)** | B.E. Computer Science and Engineering | Final Year (2025–26) | Full Stack Java Architecture, Spring Boot Microservices (`Auth` & `Scheme`), AST Eligibility Rules Engine, and Jenkins CI/CD pipeline. |
| **Student 2** | B.E. Computer Science and Engineering | Final Year (2025–26) | Machine Learning & NLP Engineering, scikit-learn Random Forest Model, Dense Vector Embedding Generation, FastAPI inference service. |
| **Student 3** | B.Tech Information Technology / CSE | Final Year (2025–26) | Frontend Architecture (React 18, Vite, Tailwind CSS), UI/UX Design, Digital Document Vault, Quality Assurance & API testing. |

### 12.2 Ownership Matrix

| Project Component | Member 1 (Primary) | Member 2 (Primary) | Member 3 (Primary) |
|---|---|---|---|
| **Spring Boot Auth Microservice** | **Owner** | Support | Support |
| **Spring Boot Scheme Microservice** | **Owner** | Support | Support |
| **AST Statutory Rules Engine** | **Owner** | Support | — |
| **Random Forest ML Module** | Support | **Owner** | — |
| **Semantic Vector Search & Chat** | Support | **Owner** | — |
| **React 18 Single Page Application** | Support | — | **Owner** |
| **Digital Document Vault & OCR** | Support | Support | **Owner** |
| **Docker & Jenkins CI/CD** | **Owner** | Support | Support |
| **Database Schema (Oracle + Mongo)**| **Owner** | Support | Support |
| **SRS Documentation (IEEE Std 830)** | **Owner** | **Owner** | **Owner** |
| **Automated Testing (JUnit + Vitest)**| **Owner** | Support | **Owner** |

### 12.3 Interdisciplinary Contribution Map

| Academic Stream | Expected Competency Area | Specific Contribution in SchemeBridge |
|---|---|---|
| **Computer Science (CSE)** | Full-Stack Java (Spring Boot), Spring Security JWT, Distributed Systems, CI/CD pipelines, Machine Learning algorithms. | Developed the core microservice cluster, statutory AST evaluation engine, Random Forest classification pipeline, and automated Jenkins pipelines. |
| **Information Technology (IT)** | Web Technologies, Responsive UX Engineering, Information Security, Cloud Operations. | Built the responsive React 18 SPA, encrypted digital document vault, role-based route guards, and accessibility WCAG compliance. |
| **Domain & Policy Context** | Public Administration, Welfare Policy, Digital Governance workflows. | Researched and structured eligibility criteria for 4,734+ central and state schemes from official gazette notifications; designed realistic citizen personas. |

---

## 13. RISK REGISTER

| ID | Risk Description | Prob. | Impact | Concrete Mitigation Strategy |
|---|---|:---:|:---:|---|
| **R01** | **AI Model Accuracy Degradation**<br>Model fails to generalize across diverse, rare citizen demographic edge cases. | **M** | **H** | Implemented balanced class weighting in Random Forest; calibrated 1.5% stochastic noise; established strict deterministic AST rules engine as primary authority for statutory qualification. |
| **R02** | **Third-Party API Outage**<br>External email gateway or external NLP inference service experiences high latency or downtime. | **M** | **H** | Designed asynchronous resilience patterns using fallback local templates, mock email services in development, and local Sentence-Transformer embeddings. |
| **R03** | **OCR Extraction Inaccuracies**<br>Low-resolution, skewed, or blurred certificates uploaded by rural citizens. | **H** | **M** | Implemented image preprocessing (grayscale normalization, binarization); set strict confidence threshold (>85% for auto-verification); route low-confidence docs to Officer Review Queue. |
| **R04** | **Data Privacy Exposure (PII Breach)**<br>Accidental disclosure of citizen personal data or certificates in logs or Git. | **L** | **H** | Enforced strict `.gitignore` on environment configs (`.env`); zero storage of plaintext passwords; encrypted certificates at rest (AES-256); automated Git secret scanners. |
| **R05** | **Team Member Unavailability**<br>Illness or academic constraints preventing a member from delivering their module. | **L** | **H** | Maintained clean architectural separation with standardized REST contracts; conducted weekly cross-training on Java and React components; maintained 1-week timeline buffer. |
| **R06** | **Container Deployment Failure during Live Demo**<br>Host environment networking or memory constraints breaking container startup. | **M** | **M** | Pre-tested deployment on clean Linux virtual machines; verified `docker-compose.yml` health checks; prepared recorded 5-minute video demonstration as contingency. |
| **R07** | **Scope Creep & Feature Bloat**<br>Attempting too many secondary features, compromising core stability before evaluation. | **M** | **M** | Frozen core scope in Week 1 SRS; tracked features strictly via GitHub Milestones; designated advanced features (e.g. voice input) as optional stretch goals. |
| **R08** | **Database Lockups or Connection Exhaustion**<br>Surge in concurrent queries causing thread pool exhaustion in Oracle/MongoDB. | **L** | **H** | Configured HikariCP connection pooling in Spring Boot (`maximum-pool-size=20`); indexed all foreign query keys in MongoDB (`userId`, `schemeId`, `status`). |
| **R09** | **Bilingual Localization Drift**<br>Translations out of sync with official scheme terminology causing citizen confusion. | **M** | **L** | Structured multilingual scheme entities using canonical JSON dictionaries (`MultilingualText.java`) with English baseline fallback. |
| **R10** | **Stateless Session Synchronization Bugs**<br>Expired JWT tokens causing jarring page redirects or data loss during form submission. | **M** | **M** | Implemented Axios interceptor automatically refreshing access tokens via refresh token rotation; cached form progress in `sessionStorage`. |

---

## 14. APPENDIX

### 14.1 Glossary of Domain-Specific Terms

| Term | Definition |
|---|---|
| **Direct Benefit Transfer (DBT)** | Government mechanism of transferring subsidy benefits directly to citizen verified bank accounts. |
| **Economically Weaker Section (EWS)**| Official reservation sub-category in India for citizens with annual household income below ₹8,00,000. |
| **Below Poverty Line (BPL)** | Economic benchmark used by government agencies to identify households eligible for maximum welfare subsidies. |
| **Kisan Credit Card (KCC)** | Specialized credit welfare scheme providing Indian farmers with timely credit for agricultural inputs. |
| **Public Distribution System (PDS)** | Indian food security system distributing subsidized food grains to vulnerable citizens via Ration Cards. |
| **AST Business Rule** | Tree structure representing statutory conditions: e.g., `(Age >= 18 AND Age <= 35) AND (Income < 250000) AND (State == "Tamil Nadu")`. |

### 14.2 AI Model Details — Training Logs & Hyperparameters

#### Training Log Summary (Random Forest Classifier)
- **Dataset File:** `ml/random_forest/data/synthetic_citizen_eligibility_dataset.csv`
- **Number of Records:** 5,000 rows (4,000 training, 1,000 test; stratified 80:20 split)
- **Training Environment:** Python 3.11.7, scikit-learn 1.4.1, CPU execution (4 physical cores)
- **Training Duration:** 1.42 seconds (200 estimators parallelized via `n_jobs=-1`)
- **Model Size on Disk:** 4.1 MB (`ml/random_forest/models/random_forest_scheme_model.pkl`)

#### Hyperparameters
```python
RandomForestClassifier(
    n_estimators=200,
    criterion="gini",
    max_depth=16,
    min_samples_split=4,
    min_samples_leaf=2,
    max_features="sqrt",
    class_weight="balanced",
    random_state=42,
    n_jobs=-1
)
```

#### Classification Report (Held-Out Test Set)
```
                                        precision    recall  f1-score   support

         Agriculture & Farmer Welfare       0.99      0.98      0.98       143
             Education & Scholarships       0.98      0.99      0.98       143
      Rural & Urban Livelihood Support      0.97      0.97      0.97       143
 Senior Citizen Pension & Healthcare        0.99      0.99      0.99       143
  Skill Development & Entrepreneurship      0.98      0.98      0.98       143
Social Welfare & Disability Support         0.99      0.99      0.99       143
          Women & Child Development         0.99      0.99      0.99       142

                             accuracy                           0.9840      1000
                            macro avg       0.98      0.98      0.98      1000
                         weighted avg       0.98      0.98      0.98      1000
```

---

### 14.3 API Request-Response Samples

#### 1. User Registration (`POST /api/auth/register`)
**Request:**
```json
{
  "name": "Kavitha Raman",
  "email": "kavitha.raman@example.com",
  "mobile": "9876543210",
  "password": "SecurePassword@2026",
  "role": "ROLE_USER"
}
```
**Response (HTTP 201 Created):**
```json
{
  "success": true,
  "message": "User registered successfully. Verification OTP dispatched.",
  "data": {
    "userId": 1042,
    "email": "kavitha.raman@example.com",
    "status": "PENDING_VERIFICATION"
  }
}
```

#### 2. Deterministic AST Eligibility Evaluation (`POST /api/eligibility/evaluate/SCH-PM-KISAN-01`)
**Request:**
```json
{
  "citizenProfileId": "6648dfa2e109ab43810"
}
```
**Response (HTTP 200 OK):**
```json
{
  "schemeCode": "PM-KISAN",
  "schemeName": "Pradhan Mantri Kisan Samman Nidhi",
  "eligibilityStatus": "ELIGIBLE",
  "overallVerdict": true,
  "ruleEvaluations": [
    {
      "criterion": "Occupation",
      "required": "FARMER",
      "actual": "FARMER",
      "satisfied": true,
      "message": "Citizen occupation matches agricultural criteria."
    },
    {
      "criterion": "Landholding Cap",
      "required": "<= 2.0 Hectares",
      "actual": "1.2 Hectares",
      "satisfied": true,
      "message": "Cultivable land within small/marginal farmer ceiling."
    },
    {
      "criterion": "Institutional Landowner",
      "required": "false",
      "actual": "false",
      "satisfied": true,
      "message": "Applicant is not an institutional landholder."
    }
  ],
  "recommendedAction": "Proceed to Application Wizard"
}
```

#### 3. AI Category Recommendation (`POST /predict/recommend-category`)
**Request:**
```json
{
  "age": 22,
  "gender": "Female",
  "state": "Tamil Nadu",
  "income": 120000,
  "occupation": "Student",
  "category": "OBC",
  "rural": "Yes",
  "student": "Yes",
  "disability": "No"
}
```
**Response (HTTP 200 OK):**
```json
{
  "predictedCategory": "Education & Scholarships",
  "confidenceScore": 0.9702,
  "inferenceLatencyMs": 0.021,
  "alternativePredictions": [
    { "category": "Skill Development & Entrepreneurship", "confidence": 0.018 },
    { "category": "Women & Child Development", "confidence": 0.0118 }
  ]
}
```

---

### 14.4 References & Citations
1. **Government of India Scheme Portals:** myScheme National Platform ([https://www.myscheme.gov.in/](https://www.myscheme.gov.in/)). Accessed: September 2026.
2. **National Scholarship Portal (NSP):** Ministry of Electronics and Information Technology ([https://scholarships.gov.in/](https://scholarships.gov.in/)). Accessed: September 2026.
3. **PM-KISAN Portal:** Department of Agriculture and Farmers Welfare ([https://pmkisan.gov.in/](https://pmkisan.gov.in/)). Accessed: September 2026.
4. **IEEE Computer Society:** IEEE Std 830-1998 — *IEEE Recommended Practice for Software Requirements Specifications*, IEEE Standards Coordinating Committee, 1998.
5. **Breiman, Leo:** *Random Forests*, Machine Learning 45, 5–32 (2001). [https://doi.org/10.1023/A:1010933404324](https://doi.org/10.1023/A:1010933404324).
6. **Reimers, Nils & Gurevych, Iryna:** *Sentence-BERT: Sentence Embeddings using Siamese BERT-Networks*, Proceedings of EMNLP-IJCNLP 2019.

---

### END OF SRS DOCUMENT — Version 1.0
**Full Stack Java (AI-Integrated) Training Programme**  
**Department of Computer Science and Engineering**  
**Karpagam College of Engineering, Coimbatore**  
*Mentors: Dr. Arul Antran Vijay S & Dr. Jothi Prakash V*
