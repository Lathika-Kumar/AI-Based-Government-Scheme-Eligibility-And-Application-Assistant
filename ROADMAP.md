# SchemeBridge Master Development Roadmap

## 🎯 Executive Summary
SchemeBridge is an AI-based Government Scheme Eligibility and Application Assistant. The system connects Citizens with personalized government welfare schemes, automates eligibility checks, manages document vaults, and provides administrative oversight.

---

## 📊 Current Phase Progress

```
Phase 1: Authentication & OTP Security        [██████████] 100% (DONE)
Phase 2: Citizen Profile & Audit Engine       [██████████] 100% (DONE)
Phase 3: Scheme Catalog & Eligibility Engine  [░░░░░░░░░░]   0% (UP NEXT)
Phase 4: Document Vault & Storage Service     [░░░░░░░░░░]   0% (UPCOMING)
Phase 5: Application Processing & Tracking    [░░░░░░░░░░]   0% (UPCOMING)
Phase 6: Admin Consoles & Grievances          [░░░░░░░░░░]   0% (UPCOMING)
Phase 7: Live AI (Gemini) & DigiLocker OAuth  [████░░░░░░]  40% (IN PROGRESS)
Phase 8: Production Deployment & CI/CD        [░░░░░░░░░░]   0% (UPCOMING)
```

---

## 📦 Phase Details & Completed Endpoints

### ✅ Priority 1 — Authentication & Security (Phase 1 Completed)
- [x] `POST /api/v1/auth/login` (JWT token generation, BCrypt authentication)
- [x] `POST /api/v1/auth/register` (User registration with role CITIZEN)
- [x] `POST /api/v1/auth/send-email-otp` (Email OTP dispatch)
- [x] `POST /api/v1/auth/send-phone-otp` (Mobile OTP dispatch)
- [x] `POST /api/v1/auth/verify-otp` (OTP verification & status transition to `ACTIVE`)

---

### ✅ Priority 2 — Citizen Profile & Audit Engine (Phase 2 Completed)
- [x] `GET /api/v1/profile` (Retrieve authenticated citizen profile with masked PII)
- [x] `PUT /api/v1/profile` (Update citizen profile, increment `profileVersion`, log audit events to `profile_audit_logs`)
- [x] `GET /api/v1/profile/completion` (5-section weighted scoring breakdown & status classification)
- [x] `GET /api/v1/profile/summary` (Lightweight dashboard summary payload)

---

### ⏳ Priority 3 — Schemes & Dynamic Eligibility Engine (Phase 3 — UP NEXT)
- [ ] `GET /api/v1/schemes` (Paginated list of active schemes with filters)
- [ ] `GET /api/v1/schemes/:id` (Detailed scheme view & eligibility criteria)
- [ ] `POST /api/v1/schemes/recommendations` (Dynamic match engine pairing citizen profile with schemes)
- [ ] `GET /api/v1/schemes/categories` (Category taxomomy list)
- [ ] `GET /api/v1/schemes/search` (Full-text search across titles, tags, benefits)
- [ ] `POST /api/v1/schemes/:id/eligibility` (Individual scheme eligibility evaluation)

---

### ⏳ Priority 4 — Document Vault & Verification Service (Phase 4)
- [ ] `GET /api/v1/documents` (List citizen's uploaded documents)
- [ ] `POST /api/v1/documents` (Upload document with file storage)
- [ ] `DELETE /api/v1/documents/:id` (Delete stored document)
- [ ] `POST /api/v1/documents/:id/verify` (Verify document fields against profile)
- [ ] `POST /api/v1/documents/digilocker/sync` (DigiLocker sync integration)
- [ ] `GET /api/v1/documents/:id/download` (Secure file download link)
- [ ] `GET /api/v1/documents/vault-score` (Document readiness score)

---

### ⏳ Priority 5 — Application Engine & Tracking (Phase 5)
- [ ] `GET /api/v1/applications` (List citizen's applications)
- [ ] `POST /api/v1/applications` (Submit scheme application)
- [ ] `GET /api/v1/applications/:id` (Application details view)
- [ ] `POST /api/v1/applications/:id/withdraw` (Withdraw active application)
- [ ] `GET /api/v1/applications/saved` (List bookmarked schemes)
- [ ] `POST /api/v1/applications/saved/:schemeId` (Bookmark scheme)
- [ ] `DELETE /api/v1/applications/saved/:schemeId` (Remove scheme bookmark)
- [ ] `GET /api/v1/applications/:id/timeline` (Status transition history log)

---

### ⏳ Priority 6 — Admin Management & Grievance Workflows (Phase 6)
- [ ] `GET /api/v1/admin/stats` (Executive dashboard counters)
- [ ] `GET /api/v1/admin/users` & `PUT /api/v1/admin/users/:id` (User management)
- [ ] `GET /api/v1/admin/grievances` & `POST /api/v1/admin/grievances/:id/resolve` (Grievances center)
- [ ] `POST /api/v1/admin/schemes`, `PUT /api/v1/admin/schemes/:id`, `DELETE /api/v1/admin/schemes/:id`, `POST /api/v1/admin/schemes/:id/publish` (Scheme CRUD)
- [ ] `POST /api/v1/admin/applications/:id/review` (Application review & decision log)

---

### 🤖 Priority 7 — AI & External Integrations (Phase 7)
- [ ] Connect `SchemeAIChatWidget` to real Gemini API.
- [ ] DigiLocker OAuth 2.0 Flow integration.
- [ ] OCR Document Data Extraction pipeline.

---

### 🚀 Priority 8 — Cloud Deployment & CI/CD (Phase 8)
- [ ] GitHub Actions workflow.
- [ ] Cloud deployment (Render/AWS/GCP + Vercel).
- [ ] HTTPS & Custom Domain.
