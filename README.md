# SchemeBridge: AI-Based Government Scheme Eligibility & Application Assistant

[![Smart India Hackathon 2025](https://img.shields.io/badge/SIH-2025_SIH25120-orange.svg)](https://sih.gov.in)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17_LTS-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![React](https://img.shields.io/badge/React-18%2F19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5%2F8-646CFF?logo=vite&logoColor=white)](https://vitejs.dev/)
[![MongoDB](https://img.shields.io/badge/MongoDB-7.0-47A248?logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Oracle DB](https://img.shields.io/badge/Oracle_DB-21c_XE-F80000?logo=oracle&logoColor=white)](https://www.oracle.com/database/)
[![Python](https://img.shields.io/badge/Python-3.11-3776AB?logo=python&logoColor=white)](https://www.python.org/)
[![scikit--learn](https://img.shields.io/badge/scikit--learn-98.40%25_Accuracy-F7931E?logo=scikit-learn&logoColor=white)](https://scikit-learn.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 📌 Project Overview

**SchemeBridge** is an enterprise-grade, bilingual digital governance platform developed for **Smart India Hackathon 2025 (Problem Statement ID: SIH25120)** under the **Ministry of Electronics and Information Technology (MeitY)** and **Ministry of Social Justice and Empowerment, Government of India**.

In India, hundreds of central and state welfare schemes remain underutilized due to fragmented portals, complex statutory eligibility rules, language barriers, and predatory middlemen. **SchemeBridge** bridges this divide by delivering:

1. **4,734+ Indexed Welfare Schemes** covering Central and State welfare programs across India.
2. **Deterministic AST Statutory Rules Engine** (`EligibilityEngine.java`) that computes 100% mathematically verifiable qualification results with zero AI hallucination.
3. **Dual-Tier AI/ML Intelligence**:
   - **Random Forest Classifier (98.40% accuracy)** trained on demographic profiles for instant scheme category recommendations.
   - **Dense Semantic Vector Search (384-dimensional `all-MiniLM-L6-v2`)** and Conversational Assistant to interpret informal citizen queries.
4. **Universal Digital Document Vault** with automated OCR (`Apache PDFBox` / `Tesseract`) and profile cross-verification (Levenshtein distance matching).
5. **4-Step Assisted Application Wizard** with profile auto-population, QR-coded PDF acknowledgement receipts, and complete tracking.
6. **Administrative Verification Desk** with a split-pane document review workstation, immutable audit logs, and automated SLA-based grievance escalation.

---

## 🏗️ System Architecture

SchemeBridge implements a distributed, decoupled multi-service architecture with a unified **Nginx reverse proxy** acting as a single entry point (Zero-CORS configuration):

```
                                  Client Browser / Mobile
                                            │
                                            │ Port 80 (or 443 with SSL)
                                            ▼
                             ┌─────────────────────────────┐
                             │     Frontend Ingress        │
                             │       (Nginx:alpine)        │
                             │  • Serves React / Vite SPA  │
                             │  • Zero-CORS Reverse Proxy  │
                             │  • Gzip & Cache Controller  │
                             └──────────────┬──────────────┘
                                            │
                        ┌───────────────────┴───────────────────┐
                        │ (Isolated Docker Network: schemebridge)
                        ▼                                       ▼
          /api/auth/* & /api/admin/users/*               All other /api/*
         ┌─────────────────────────────────┐   ┌─────────────────────────────────┐
         │      Auth Service Container     │   │     Scheme Service Container    │
         │      (Spring Boot, Java 17)     │   │      (Spring Boot, Java 17)     │
         │      Port: 8080 (Internal)      │   │      Port: 8081 (Internal)      │
         └────────────────┬────────────────┘   └────────┬────────────────────────┘
                          │                             │
                          │                             ├──────────────────────────┐
                          ▼                             ▼                          ▼
            ┌──────────────────────────┐   ┌───────────────────────────┐  ┌─────────────────────────┐
            │   Oracle Database 21c    │   │     MongoDB 7.0 Container │  │  FastAPI / ML Engine    │
            │   (Users, Auth, Roles,   │   │  (4,734+ Master Schemes,  │  │  (Random Forest Model,  │
            │    Refresh Tokens, PII)  │   │   Vault Docs, Grievances, │  │   384-D Vector Index,   │
            │        Port: 1521        │   │    Applications, Audits)  │  │    Sentence-Transformers│
            └──────────────────────────┘   │        Port: 27017        │  │        Port: 8000       │
                                           └───────────────────────────┘  └─────────────────────────┘
```

### Microservice Division

| Service | Port | Persistence | Primary Responsibility |
|---|---|---|---|
| **Frontend Web App** | `80` / `443` | N/A | React 18/19 SPA, Tailwind CSS UI, interactive dashboards, application wizards, and bilingual support. |
| **Auth Service** (`schemebridge-auth-service`) | `8080` | Oracle 21c XE | Citizen & Admin onboarding, BCrypt hashing, stateless JWT issuance/validation, and role-based security. |
| **Scheme Service** (`schemebridge-scheme-service`) | `8081` | MongoDB 7.0 | Master scheme catalog (4,734+ records), AST eligibility engine, vault document storage, application pipelines, audit trails, and grievances. |
| **AI / ML Subsystem** | `8000` / Local | In-Memory / Vector File | Random Forest demographic category prediction and 384-dimensional cosine vector similarity search. |

---

## 💻 Complete Technology Stack

### Frontend Architecture
- **Framework & Build**: React (18.3 / 19), Vite (5.x / 8.x) for sub-second Hot Module Replacement (HMR) and optimized rollup production bundles.
- **Styling & Design System**: Tailwind CSS (v3.4), PostCSS, Autoprefixer, modern glassmorphic UI tokens, dark/light theme switching.
- **Icons & Visuals**: Lucide React (`lucide-react`) for consistent SVG icons.
- **Data Visualization**: Recharts (`recharts`) for admin metric distribution charts and application status graphs.
- **Routing & Forms**: React Router DOM (v7), Zod runtime schema validation.
- **Testing**: Vitest, React Testing Library.

### Backend Microservices
- **Core Framework**: Java 17 LTS, Spring Boot 3.3.2.
- **Security & Authorization**: Spring Security 6, JJWT (`io.jsonwebtoken` 0.12.6) for stateless HMAC-SHA256 JWT tokens.
- **Relational Persistence**: Spring Data JPA, Hibernate, Oracle JDBC Driver (`ojdbc11`), Flyway for database schema versioning.
- **Document & File Persistence**: Spring Data MongoDB, MongoDB GridFS for citizen document binary storage.
- **Document Intelligence**: Apache PDFBox (3.0.2) for native text/stream extraction and Tesseract OCR integration.
- **API Documentation**: SpringDoc OpenAPI / Swagger UI (`springdoc-openapi-starter-webmvc-ui` 2.6.0).
- **Communication & Mail**: Spring Boot Starter Mail (SMTP) for OTP and notification emails.

### AI / Machine Learning Stack
- **Languages & Frameworks**: Python 3.11, FastAPI, Uvicorn, scikit-learn (1.4+), NumPy, Pandas.
- **Classification Model**: scikit-learn `RandomForestClassifier` (200 estimators, balanced class weights, achieving **98.40% accuracy**).
- **Semantic Embeddings**: Sentence-Transformers (`all-MiniLM-L6-v2`) generating 384-dimensional dense semantic vectors.
- **Search & Ranking**: TruncatedSVD projection + Cosine Dot-Product distance ranking over 4,734 indexed government schemes.
- **Generative AI / LLM**: Google Gemini / RAG integration for citizen query summarization and vernacular dialogue.

### Databases & DevOps
- **Relational DB**: Oracle Database 21c Express Edition (Users, credentials, roles, security logs).
- **Document DB**: MongoDB 7.0 (Scheme records, citizen applications, digital vault files, grievance tickets).
- **Containerization**: Docker, Docker Compose (v3.8 specification), Alpine Linux base images.
- **CI/CD**: Jenkins / GitHub Actions multi-stage build pipelines.

---

## 🔄 End-to-End System Workflows

### 1. Citizen Onboarding & Profile Verification Workflow

```
[Citizen] ────────► Enters Mobile, Email, Password
                         │
                         ▼
             [Auth Service (Port 8080)]
                         │
        ┌────────────────┴────────────────┐
        ▼                                 ▼
   Verify Uniqueness              Hash Password (BCrypt)
   in Oracle DB                    Generate 6-Digit OTP
        │                                 │
        ▼                                 ▼
   Persist User                Dispatch via Email / SMS
   (Status: PENDING)                      │
        │                                 ▼
        └─────────────────────── Citizen Submits OTP
                                          │
                                          ▼
                               Mark User Status: ACTIVE
                               Issue Signed JWT Bearer Token
                                          │
                                          ▼
                               Create Citizen Profile
                               (Age, Gender, Income, Caste, State, Occupation)
```

1. **Registration**: Citizen registers with email, mobile, and password.
2. **Security & Hashing**: Backend hashes credentials with BCrypt (cost factor 12) and persists the record to Oracle DB in `PENDING_VERIFICATION` status.
3. **OTP Dispatch**: A 6-digit cryptographic OTP is generated and dispatched via SMTP.
4. **Token Generation**: Upon successful OTP entry, the account is activated and signed JWT access/refresh tokens are returned.
5. **Profile Completion**: The citizen completes a multi-step onboarding wizard storing socioeconomic parameters (income, caste, district, landholding, occupation) required for welfare checks.

---

### 2. Dual-Engine Scheme Discovery & Recommendation Workflow

```
[Citizen Query / Search]
       │
       ├─────────────────────────────────┬─────────────────────────────────┐
       ▼                                 ▼                                 ▼
[Keyword / Faceted Search]      [Random Forest Classifier]     [Dense Semantic Search]
- Category, State, Gender       - Inputs: 9-D Citizen Vector   - Model: all-MiniLM-L6-v2
- Income & Age Range            - Output: Welfare Category     - 384-D Vector Space
- Fast Compound Index Query     - Accuracy: 98.40%             - Cosine Similarity Score
       │                                 │                                 │
       └─────────────────────────────────┼─────────────────────────────────┘
                                         ▼
                     [Aggregated & Ranked Recommendations]
                     Displays matching schemes with confidence badge
```

- **Faceted Catalog Filtering**: Dynamic MongoDB queries filter 4,734+ schemes by state, age, category, and income.
- **Machine Learning Categorization**: The 9-attribute citizen demographic vector (`age`, `gender`, `income`, `occupation`, `caste`, `state`, `rural`, `student`, `disability`) is processed by the Random Forest model to instantly recommend suitable welfare categories.
- **Semantic Vector Match**: Informal or vernacular citizen queries (e.g. *"money for pregnant mothers"* vs official title *"Pradhan Mantri Matru Vandana Yojana"*) are converted into 384-dimensional embeddings and matched via cosine similarity.

---

### 3. Deterministic AST Statutory Eligibility Workflow

To guarantee zero hallucination in legal welfare decisions, eligibility is evaluated deterministically using an **Abstract Syntax Tree (AST)**:

```
                      [Citizen Clicks "Check Eligibility"]
                                       │
                                       ▼
                        [EligibilityEngine.java]
                                       │
               Fetches Scheme AST Rule Tree from MongoDB
                                       │
                ┌──────────────────────┴──────────────────────┐
                ▼                                             ▼
       [Rule Group: AND]                             [Rule Group: OR]
       • Age >= 18 && Age <= 60                      • Category == "SC" OR "ST"
       • Income <= 250000                            • Landholding < 2.5 Acres
                │                                             │
                └──────────────────────┬──────────────────────┘
                                       ▼
                         Evaluates Each Condition Node
                                       │
        ┌──────────────────────────────┼──────────────────────────────┐
        ▼                              ▼                              ▼
  [ELIGIBLE]                     [INELIGIBLE]               [INSUFFICIENT_DATA]
  All mandatory criteria met     Disqualified with reason   Prompts citizen to update
                                 explanation                missing profile fields
```

- **AST Rule Parser**: Schemes store nested criteria with Boolean operators (`AND`, `OR`, `NOT`).
- **Explainable Verdicts**: The engine generates a line-by-line justification explaining precisely which criteria passed and which failed.

---

### 4. Digital Vault & OCR Cross-Verification Workflow

```
[Citizen Uploads Document] (Aadhaar, Income, Caste, Land Record)
            │
            ▼
[GridFS Document Storage] (Encrypted binary + SHA-256 Checksum)
            │
            ▼
[OCR & Document Intelligence Engine] (PDFBox / Tesseract)
            │
            ▼
Extracts Text: Certificate Number, Applicant Name, Date of Birth, Issue Date
            │
            ▼
[Levenshtein Distance Cross-Matcher]
Compares extracted text with registered Citizen Profile
            │
      ┌─────┴─────────────────────────┐
      ▼                               ▼
Similarity >= 85%              Similarity < 85%
Marked: [AUTO_VERIFIED]        Marked: [FLAGGED_FOR_MANUAL_REVIEW]
```

- **Universal Reuse**: Once a document is verified in the citizen's vault, it can be attached to multiple scheme applications without re-uploading.
- **Tamper Resistance**: Checksums prevent duplicate or altered file uploads.

---

### 5. 4-Step Application Filing & Tracking Workflow

```
[Step 1: Profile Review] ──► Pre-populates verified citizen profile data
           │
[Step 2: Scheme Form]    ──► Captures scheme-specific questions (bank account, crop details, etc.)
           │
[Step 3: Document Attach]──► One-click attachment from Citizen Document Vault
           │
[Step 4: Sign & Submit]  ──► Self-declaration confirmation & submission
           │
           ▼
[Backend Processing]
- Generates Tracking Number: APP-2026-XXXXX
- Sets Status: SUBMITTED
- Creates QR-coded PDF Acknowledgement Receipt
- Dispatches Email Confirmation
```

- **Real-Time Tracking**: Citizens track live progress (`SUBMITTED` ➔ `UNDER_REVIEW` ➔ `ACTION_REQUIRED` ➔ `APPROVED` / `REJECTED`).
- **Grievance Redressal**: If an application exceeds SLA limits, citizens can log an escalation ticket with automated officer notification.

---

### 6. Administrative Review & Verification Desk Workflow

```
[Verification Officer Login] ──► Authenticated via Spring Security (ROLE_ADMIN / OFFICER)
             │
             ▼
[Split-Pane Review Workspace]
 ┌───────────────────────────────┬───────────────────────────────┐
 │ Left Pane: Applicant Data     │ Right Pane: Document Viewer   │
 │ • Extracted OCR Fields        │ • Live Certificate Rendering  │
 │ • AST Rule Verdicts           │ • Zoom, Rotate, Inspect       │
 └───────────────────────────────┴───────────────────────────────┘
             │
             ├───────────────────┬───────────────────┐
             ▼                   ▼                   ▼
        [Approve]            [Reject]       [Request Clarification]
   Status: APPROVED     Status: REJECTED    Status: ACTION_REQUIRED
             │                   │                   │
             └───────────────────┼───────────────────┘
                                 ▼
                     [Immutable Audit Log]
       Captures Officer ID, Action, Timestamp, IP Address
                                 │
                                 ▼
                    [Push & Email Alert to Citizen]
```

---

## 📂 Project Directory Structure

```
SCHEMEBRIDGE/
├── .env.docker                          # Production Docker environment configuration
├── .env.example                         # Environment template reference
├── DEPLOYMENT.md                        # Production deployment guide
├── ER_DIAGRAM.png / .puml               # Complete Entity-Relationship architectural model
├── SRS_DOCUMENT.md                      # IEEE Std 830-1998 System Requirements Specification
├── docker-compose.yml                   # Multi-service Docker Compose orchestration
├── deploy.sh / deploy.ps1               # Automated deployment scripts (Linux / Windows)
│
├── data/                                # Machine learning assets & datasets
│   ├── ml_dataset/                      # Synthetic citizen datasets & scheme checklists
│   ├── ml_models/                       # Trained Random Forest model & vector indices
│   ├── ml_predictions/                  # Precomputed classification predictions
│   └── mongodb/                         # MongoDB seed scripts & scheme dumps
│
├── ml/                                  # Machine learning source & training pipelines
│   └── random_forest/
│       ├── train_random_forest.py       # scikit-learn training script (98.40% accuracy)
│       ├── evaluate_model.py            # Confusion matrix & performance evaluation
│       └── requirements.txt             # Python dependencies
│
├── scripts/                             # Utility & pipeline automation scripts
│   ├── document_intelligence_pipeline.py# Document checklist extraction pipeline
│   ├── generate_scheme_embeddings.py    # Sentence-transformer 384-D vector generator
│   └── extract_image_ocr.py             # OCR extraction helper
│
├── schemebridge-auth-service/           # Auth Microservice (Spring Boot + Oracle XE)
│   ├── pom.xml                          # Maven build configuration
│   └── src/main/java/com/schemebridge/auth/
│       ├── config/                      # Security & JWT configuration
│       ├── controller/                  # REST controllers (/api/auth/*, /api/admin/users/*)
│       ├── entity/                      # JPA entities (User, Role, RefreshToken)
│       ├── repository/                  # Spring Data JPA repositories
│       └── service/                     # Authentication & email dispatch services
│
├── schemebridge-scheme-service/         # Core Scheme Microservice (Spring Boot + MongoDB)
│   ├── pom.xml                          # Maven build configuration
│   └── src/main/java/com/schemebridge/scheme/
│       ├── controller/                  # Scheme, Eligibility, Application & Vault APIs
│       ├── document/                    # MongoDB document models (Scheme, Application, Vault)
│       ├── ocr/                         # Native PDFBox & regex document parsing
│       ├── repository/                  # Spring Data MongoDB repositories
│       └── service/                     # EligibilityEngine, ApplicationService, VaultService
│
└── schemebridge-frontend/               # Frontend Application (React + Vite)
    └── schemeBridge-frontend/
        ├── package.json                 # Node dependencies & npm run scripts
        ├── vite.config.js               # Vite build configuration
        └── src/
            ├── admin/                   # Admin Review Desk, Analytics & Audit screens
            ├── user/                    # Citizen portal, onboarding, search & vault
            ├── components/              # Reusable UI components & navigation headers
            ├── context/                 # React Context providers (Auth, Language, Theme)
            └── services/                # Axios API client integrations
```

---

## 🚀 Quick Start & Installation

### Option 1: One-Click Docker Compose (Recommended)

#### Prerequisites
- [Docker](https://docs.docker.com/get-docker/) (v24+)
- [Docker Compose](https://docs.docker.com/compose/) (v2.0+)

#### Steps
1. **Clone the repository**:
   ```bash
   git clone https://github.com/Lathika-Kumar/AI-Based-Government-Scheme-Eligibility-And-Application-Assistant.git schemebridge
   cd schemebridge
   ```

2. **Configure Environment Variables**:
   ```bash
   cp .env.docker .env
   ```
   *(Update credentials such as `JWT_SECRET`, `ORACLE_URL`, `ORACLE_PASSWORD` in `.env` if using external instances).*

3. **Launch All Services**:
   - **Linux / macOS**:
     ```bash
     chmod +x deploy.sh
     ./deploy.sh
     ```
   - **Windows (PowerShell)**:
     ```powershell
     .\deploy.ps1
     ```
   - **Or via Docker Compose directly**:
     ```bash
     docker compose up --build -d
     ```

4. **Access the Application**:
   - **Web Application**: [http://localhost](http://localhost) (Port 80)
   - **Auth Service APIs**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - **Scheme Service APIs**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
   - **MongoDB Database**: `mongodb://localhost:27017`

---

### Option 2: Local Development Setup (Without Docker)

#### Prerequisites
- **Java 17 JDK**
- **Node.js 18+ & npm**
- **Python 3.11**
- **MongoDB 7.0 Community Server**
- **Oracle Database 21c XE**

#### 1. Start MongoDB
```bash
mongod --dbpath <path-to-data-dir> --port 27017
```

#### 2. Start Auth Service (Port 8080)
```bash
cd schemebridge-auth-service
mvn clean spring-boot:run
```

#### 3. Start Scheme Service (Port 8081)
```bash
cd schemebridge-scheme-service
mvn clean spring-boot:run
```

#### 4. Start React Frontend (Port 5173 / 80)
```bash
cd schemebridge-frontend/schemeBridge-frontend
npm install
npm run dev
```

Visit [http://localhost:5173](http://localhost:5173) in your browser.

---

## 📡 REST API Summary

### Authentication Service (`/api/auth/*`)
| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register new citizen account and dispatch OTP | Public |
| `POST` | `/api/auth/verify-otp` | Verify 6-digit registration OTP & activate user | Public |
| `POST` | `/api/auth/login` | Authenticate user and return signed JWT tokens | Public |
| `POST` | `/api/auth/refresh-token` | Exchange refresh token for fresh access token | Public |
| `GET` | `/api/auth/me` | Fetch authenticated user profile & roles | Bearer JWT |

### Scheme & Eligibility Service (`/api/schemes/*`, `/api/eligibility/*`)
| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/schemes` | Paginated search across 4,734+ welfare schemes | Public |
| `GET` | `/api/schemes/{id}` | Detailed scheme guidelines, benefits & criteria | Public |
| `POST` | `/api/eligibility/evaluate/{schemeId}` | Run deterministic AST statutory rules check | Citizen |
| `GET` | `/api/recommendations/personalized` | AI Random Forest & vector recommendations | Citizen |

### Document Vault & Application Service (`/api/documents/*`, `/api/applications/*`)
| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/documents/vault/upload` | Upload document to vault with automated OCR | Citizen |
| `GET` | `/api/documents/vault` | Retrieve citizen's uploaded certificates | Citizen |
| `POST` | `/api/applications` | Submit application for eligible scheme | Citizen |
| `GET` | `/api/applications/my` | Retrieve citizen's submitted applications | Citizen |
| `GET` | `/api/applications/{id}/receipt` | Download PDF acknowledgement with QR code | Citizen |

### Admin & Audit Service (`/api/admin/*`)
| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/admin/applications` | Officer queue for pending applications | Officer / Admin |
| `PUT` | `/api/admin/applications/{id}/review` | Approve, reject, or request information | Officer / Admin |
| `GET` | `/api/admin/analytics` | Real-time application volume & SLA metrics | Admin |
| `GET` | `/api/admin/audit-logs` | Immutable security and review audit trail | Admin |

---

## 🧪 Testing & Model Evaluation

### Unit & Integration Tests
```bash
# Frontend tests (Vitest)
cd schemebridge-frontend/schemeBridge-frontend
npm run test

# Backend Auth Service tests
cd schemebridge-auth-service
mvn test

# Backend Scheme Service tests
cd schemebridge-scheme-service
mvn test
```

### AI / ML Model Benchmark
The citizen welfare recommendation Random Forest model was trained on 5,000 synthesized demographic records across 10 Indian states:

```
============================================================
              RANDOM FOREST CLASSIFICATION REPORT
============================================================
Target Categories: 7 Classes
Accuracy           : 98.40% (0.9840)
Weighted Precision : 98.42%
Weighted Recall    : 98.40%
Weighted F1-Score  : 98.40%
Inference Latency  : 21.6 µs / record
============================================================
```

---

## 👥 Project Team & Mentorship

- **Academic Institution**: [Karpagam College of Engineering (Autonomous)](https://kce.ac.in), Coimbatore - 641032
- **Training Programme**: Full Stack Java (AI-Integrated) Training Programme
- **Project Initiative**: Smart India Hackathon (SIH 2025) — Problem Statement **SIH25120**
- **Faculty Mentors**:
  - Dr. Arul Antran Vijay S
  - Dr. Jothi Prakash V
  - Mr. Jegathesh P
  - Mr. Navaneetha Krishnan M
  - Dr. Castro S

---

## 📜 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
