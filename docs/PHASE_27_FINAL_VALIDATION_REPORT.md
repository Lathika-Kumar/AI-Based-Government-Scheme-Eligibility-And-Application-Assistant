# PHASE 27 FINAL VALIDATION REPORT

**Phase**: Phase 27 — Offline Interaction-to-Outcome Attribution, Multi-level Relevance Labeling & LTR Dataset Pipeline  
**Execution Timestamp**: 2026-09-05T15:06:33+05:30  
**Overall Validation Status**: **PASSED**  
**Active Production Model**: `2.2.0-hybrid-semantic-384d`  
**Fallback Model**: `1.0.0-deterministic` (Circuit Breaker: 200 ms)  
**Training Readiness Status**: `TRAINING_NOT_READY` (Legitimate Outcome Sessions: 0 < 100 threshold)  

---

## 1. Executive Summary

Phase 27 successfully implements the offline interaction-to-outcome telemetry attribution, multi-level relevance labeling, and Learning-to-Rank (LTR) dataset preparation pipeline for SchemeBridge without prematurely training or promoting any model.

All non-negotiable operational constraints were satisfied with zero violations:
1. **Statutory Eligibility First Gate**: `EligibilityEngine.evaluate() == ELIGIBLE` is verified as the mandatory first gate. No feature vector, relevance label, or training pair can be created for any scheme failing statutory eligibility (`STATUTORY_ELIGIBILITY_VIOLATION_RATE = 0.00%`).
2. **Strict Production Read-Only DB**: MongoDB experienced exactly zero mutations (`INSERT=0, UPDATE=0, DELETE=0, DROP=0`).
3. **Zero Data Fabrication**: With 0 legitimate citizen outcome sessions in production telemetry, the status strictly reports `TRAINING_NOT_READY` and offline evaluation reports `INSUFFICIENT_DATA`. No synthetic clicks, applications, or outcomes were fabricated.
4. **Permanent Quarantine of Historical Test Fixtures**: All 29 historical `application_events` in MongoDB are synthetic automated test fixtures and remain permanently excluded from all training data and evaluation.
5. **Zero-PII Telemetry Protection**: Explicit fields and arbitrary metadata keys were checked and sanitized against forbidden PII tokens.
6. **Deterministic Terminal-State Relevance Attribution**: Monotonically resolves session events to the highest legitimately observed grade (0..3) without fabricating unobserved outcomes.
7. **Session-Level Isolation (0.00% Leakage)**: Sessions are split into TRAIN (70%), VALIDATION (15%), and TEST (15%) strictly at the `sessionId` level using SHA-256 hashing.
8. **No Model Training or Promotion**: The active model remains `2.2.0-hybrid-semantic-384d`.

---

## 2. Test Suite Execution Results

### 2.1 Backend Regression Suite (Maven / JUnit 5)
- **Total Tests Executed**: 444
- **Passed**: 444
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0
- **Execution Time**: 34.38 s
- **Key Phase 27 Tests (`Phase27SessionAttributionTest`)**:
  - `EndToEndPipelineTests`: Complete pipeline chain (`Eligibility -> Feature Vector -> Event Attribution -> Relevance Grade -> Training Pair -> Readiness Gate -> Split`) — **PASSED**
  - `EligibilityGateTests`: Ineligible and insufficient data schemes yield `Optional.empty()` (0.00% violation rate) — **PASSED**
  - `TrainingPair Invariant Tests`: Constructor rejects ineligible schemes with `IllegalStateException` — **PASSED**
  - `TerminalStateAttributionTests`: Monotonic terminal-state resolution across grades 0, 1, 2, 3 without regression — **PASSED**
  - `SyntheticFixtureTests`: Automated fixture detection and exclusion — **PASSED**
  - `ZeroPiiProtectionTests`: Sanitization of forbidden PII keys in telemetry metadata — **PASSED**
  - `SessionLeakageTests`: Session-level split isolation across 200 sessions with 0 overlap — **PASSED**
  - `DatasetReadinessGateTests`: Readiness gate evaluates strictly to `TRAINING_NOT_READY` when outcomes < 100 — **PASSED**

### 2.2 Frontend Test Suite (Vitest)
- **Total Test Files**: 16 passed (16)
- **Total Tests Executed**: 146
- **Passed**: 146
- **Failures**: 0
- **Execution Time**: 4.79 s
- **Phase 27 Test File**: `src/services/phase27DatasetValidation.test.js` (6 tests passed)
- **Frontend Production Build**: `npm run build` completed in 2.27s with zero errors.

---

## 3. MongoDB Before/After Forensic Audit

Audited via `python scripts/audit_mongo_phase27.py`:

| Collection / Metric | Before Phase 27 | After Phase 27 | Difference | Status |
| :--- | :---: | :---: | :---: | :---: |
| `schemes` | 4,734 | 4,734 | 0 | **Unmodified** |
| `scheme_verified_data` | 4,682 | 4,682 | 0 | **Unmodified** |
| Difference (Seed Only) | 52 | 52 | 0 | **Unmodified** |
| Duplicate `schemeCodes` | 0 | 0 | 0 | **Clean** |
| Duplicate `slugs` | 0 | 0 | 0 | **Clean** |
| Embedded Benefits | 35,612 | 35,612 | 0 | **Unmodified** |
| Embedded Tags | 22,756 | 22,756 | 0 | **Unmodified** |
| `SO2YT5YLM` Canonical Docs | 9 | 9 | 0 | **Unmodified** |
| `recommendation_events` | 0 | 0 | 0 | **Unmodified** |
| `portal_feedback` | 0 | 0 | 0 | **Unmodified** |
| `application_events` (Historical Fixtures) | 29 | 29 | 0 | **Quarantined** |
| `applications` | 0 | 0 | 0 | **Unmodified** |
| **Database Mutations** | **0** | **0** | **0** | **INSERT=0, UPDATE=0, DELETE=0, DROP=0** |

---

## 4. Synthetic Fixture Quarantine & Telemetry Audit

- **Historical `application_events`**: 29
- **Identified as Synthetic Test Fixtures**: 29 (100%)
  - Application ID: `6a952810c6037907f0030c06`
  - User IDs: `citizen_user`, `citizen1`, `citizen_sharma_65`, `test_citizen_*`
- **Quarantine Enforcement**: Quarantined permanently by `SessionAttributionService.isSyntheticFixture()` and `scripts/build_phase27_ltr_dataset.py`.
- **Legitimate Outcome Sessions Extracted**: 0
- **Fabricated Telemetry Events**: 0

---

## 5. Telemetry Zero-PII Audit

- **Audit Target**: All raw interaction telemetry collections, DTOs, and event models.
- **Forbidden PII Attributes Checked**: `aadhaar`, `uid`, `pan`, `phone`, `mobile`, `email`, `name`, `firstname`, `lastname`, `address`, `street`, `ip`, `location`, `pincode`, `udid`.
- **Violations Detected**: **0**
- **Sanitization Invariant**: `SessionInteractionEvent.sanitizeMetadata()` automatically purges any forbidden key from arbitrary telemetry metadata maps.

---

## 6. Session-Level Split & Leakage Audit

Deterministic SHA-256 hash assignment on `sessionId` mod 100:

| Split Partition | Ratio Target | Simulated 500-Session Distribution | Cross-Split Leakage |
| :--- | :---: | :---: | :---: |
| **TRAIN** | 70% | 352 sessions (70.4%) | 0 sessions (0.00%) |
| **VALIDATION** | 15% | 71 sessions (14.2%) | 0 sessions (0.00%) |
| **TEST** | 15% | 77 sessions (15.4%) | 0 sessions (0.00%) |

**Leakage Audit Result**:
- $\text{TRAIN} \cap \text{VALIDATION} = \emptyset$
- $\text{TRAIN} \cap \text{TEST} = \emptyset$
- $\text{VALIDATION} \cap \text{TEST} = \emptyset$
- **Total Cross-Split Leakage**: **0.00%**

---

## 7. Deterministic Terminal-State Attribution Results

Relevance labeling tested across interaction progressions:

| Sequence of Observed Events | Resolved Terminal Grade | Canonical Level | Invariant Check |
| :--- | :---: | :--- | :---: |
| *(No events)* | **0** | `IMPRESSED_UNENGAGED` | No outcome manufactured |
| `RECOMMENDATION_SHOWN` | **0** | `IMPRESSED_UNENGAGED` | Baseline impression |
| `RECOMMENDATION_SHOWN` $\rightarrow$ `SCHEME_VIEWED` | **1** | `VIEWED` | Interest captured |
| `RECOMMENDATION_SHOWN` $\rightarrow$ `SCHEME_EXPANDED` | **1** | `VIEWED` | Reading details |
| `SCHEME_VIEWED` $\rightarrow$ `APPLICATION_STARTED` | **2** | `INTENT_HIGH` | Deep intent |
| `APPLICATION_STARTED` $\rightarrow$ `APPLICATION_COMPLETED` | **3** | `CONVERTED` | Statutory application submitted |
| `APPLICATION_COMPLETED` $\rightarrow$ `RECOMMENDATION_SHOWN` | **3** | `CONVERTED` | **Monotonicity preserved (no regression)** |

---

## 8. Final Training Readiness & Offline Evaluation Status

- **Legitimate Outcome Sessions Required**: $\ge 100$
- **Legitimate Outcome Sessions Observed**: 0
- **Final Training Readiness Status**: `TRAINING_NOT_READY`
- **Offline Evaluation Benchmark Status**: `INSUFFICIENT_DATA`
- **Model Promotion**: **NONE** (Active model remains `2.2.0-hybrid-semantic-384d`, fallback `1.0.0-deterministic`).
- **All Phase 27 Deliverables**:
  - `schemebridge-scheme-service/src/main/java/com/schemebridge/scheme/ml/dataset/RelevanceGrade.java`
  - `schemebridge-scheme-service/src/main/java/com/schemebridge/scheme/ml/dataset/SessionInteractionEvent.java`
  - `schemebridge-scheme-service/src/main/java/com/schemebridge/scheme/ml/dataset/AttributedSession.java`
  - `schemebridge-scheme-service/src/main/java/com/schemebridge/scheme/ml/dataset/TrainingPair.java`
  - `schemebridge-scheme-service/src/main/java/com/schemebridge/scheme/ml/dataset/TrainingReadinessStatus.java`
  - `schemebridge-scheme-service/src/main/java/com/schemebridge/scheme/ml/dataset/SessionAttributionService.java`
  - `schemebridge-scheme-service/src/test/java/com/schemebridge/scheme/service/Phase27SessionAttributionTest.java`
  - `schemebridge-frontend/schemeBridge-frontend/src/services/phase27DatasetValidation.test.js`
  - `scripts/audit_mongo_phase27.py`
  - `scripts/build_phase27_ltr_dataset.py`
  - `scripts/evaluate_offline_recommender.py`
  - `data/phase27_output/training_readiness.json`
  - `data/phase27_output/dataset_attribution_metadata.json`
  - `data/phase27_output/phase27_validation_report.json`
  - `data/phase27_output/offline_evaluation_report.json`
  - `docs/PHASE_27_DATASET_ATTRIBUTION_ARCHITECTURE.md`
  - `docs/PHASE_27_OFFLINE_EVALUATION_SPECIFICATION.md`
  - `docs/PHASE_27_FINAL_VALIDATION_REPORT.md`
