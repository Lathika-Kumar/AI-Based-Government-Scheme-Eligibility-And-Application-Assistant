# PHASE 31 — PRODUCTION TELEMETRY ACTIVATION, OUTCOME INGESTION & CONTINUOUS READINESS MONITORING

## 1. Executive Summary

Phase 31 activates the controlled production telemetry ingestion, genuine citizen outcome attribution, and continuous training readiness monitoring infrastructure for SchemeBridge.

Building upon the foundations of Phases 26–30, Phase 31 ensures that incoming telemetry events pass through an authoritative **11-Gate Ingestion Boundary** and receive deterministic **7-Way Quality Classification** before participating in outcome attribution. Genuine citizen outcomes accumulate naturally toward the statutory threshold of **100 legitimate outcome sessions**.

Under no circumstances is any synthetic telemetry introduced, nor are ML models trained or promoted.

---

## 2. Architectural Invariants

| Invariant | Specification | Enforcement Mechanism |
|---|---|---|
| **Statutory Eligibility Authority** | `EligibilityEngine.evaluate()` is the sole statutory eligibility authority. Violation rate = 0.00%. | Mandatory Gate 11 in `ProductionTelemetryIngestionService` and `SessionAttributionService`. Ineligible schemes are strictly bypassed. |
| **Database Safety** | Strictly read-only for audit, evaluation, aggregation, and readiness pipelines: `INSERT=0, UPDATE=0, DELETE=0, DROP=0`. | Verified by `scripts/audit_mongo_phase31.py` and regression tests. |
| **Zero Fabrication** | Zero synthetic clicks, views, applications, completions, sessions, or labels created. | Telemetry counts derived exclusively from real production records. |
| **Synthetic Quarantine** | 29 historical synthetic `application_events` remain permanently quarantined. | Hardcoded quarantine filters in ingestion, accumulator, and offline audit scripts. |
| **Active Recommender Preserved** | Active: `2.2.0-hybrid-semantic-384d`, Fallback: `1.0.0-deterministic`, Circuit Breaker: 200 ms. | Frozen across all service configs. |
| **Model Promotion Disabled** | `modelPromotionAllowed = false` remains strictly enforced even when `TRAINING_READY`. | Enforced by `TrainingReadinessGate` and `Phase31ContinuousReadinessMonitor`. |

---

## 3. The 11-Gate Telemetry Ingestion Boundary

Every incoming citizen interaction event must pass all 11 gates before it is recognized as valid telemetry:

```
[Citizen Interaction Event]
         │
         ▼
┌────────────────────────────────────────────────────────┐
│ 1. Structural Validation (non-null payload)            │
│ 2. SessionId Validation (non-empty, non-synthetic)     │
│ 3. SchemeCode Validation (canonical scheme exists)     │
│ 4. EventType Validation (canonical enum value)         │
│ 5. Timestamp Validation (epoch > 0, not in future)     │
│ 6. Synthetic Fixture Check (quarantine test users)     │
│ 7. Recursive Zero-PII Audit (never log PII values)     │
│ 8. Scheme Master Verification (active scheme check)    │
│ 9. Recommendation Anchor Validation (RECOMMENDATION)   │
│ 10. Duplicate Detection (within session stream)        │
│ 11. Statutory Eligibility Validation (EligibilityEngine)│
└────────────────────────┬───────────────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         ▼                               ▼
 [Failed Any Gate]               [Passed All Gates]
  Categorized by                  Classified as:
  7-Way Engine:                       VALID
  - SYNTHETIC                          │
  - MALFORMED                          ▼
  - PII_VIOLATION               [Outcome Accumulator]
  - ORPHAN                      [Terminal Grade: 0-3]
  - DUPLICATE                          │
  - INVALID_SEQUENCE                   ▼
                                [Readiness Gate]
                                Threshold: 100
```

---

## 4. Telemetry Quality Classification Precedence

Classification follows a strict deterministic precedence:
1. `SYNTHETIC`: User/session matches known fixture patterns or quarantined historical records.
2. `MALFORMED`: Missing required attributes (`sessionId`, `schemeCode`, `eventType`), non-positive epoch, or future timestamp (> 1 hr).
3. `PII_VIOLATION`: Contains forbidden PII keys (`aadhaar`, `uid`, `pan`, `phone`, `mobile`, `email`, `name`, `address`, etc.) in root or nested structures. PII values are **never logged**.
4. `ORPHAN`: Scheme code missing in master repository OR non-anchor interaction lacking a prior `RECOMMENDATION_SHOWN` anchor in the session.
5. `DUPLICATE`: Identical event key `sessionId:schemeCode:eventType` already recorded in the session.
6. `INVALID_SEQUENCE`: Non-chronological timestamps or impossible event progressions.
7. `VALID`: Clean, verified genuine telemetry event.

---

## 5. Legitimate Outcome Session Definition

A session is classified as a **Legitimate Outcome Session** if and only if:
1. Originates from genuine citizen interaction telemetry.
2. Contains valid, non-synthetic `sessionId` and canonical `schemeCode`.
3. Passes recursive zero-PII inspection with zero violations.
4. Contains zero duplicate events and valid chronological ordering.
5. Has an initial `RECOMMENDATION_SHOWN` anchor for the scheme.
6. Statutory eligibility is confirmed by `EligibilityEngine.evaluate()`.
7. Contains at least one terminal conversion event:
   - `SCHEME_APPLIED`, or
   - `APPLICATION_COMPLETED`.

> [!NOTE]
> Browsing-only sessions (`RECOMMENDATION_SHOWN`, `SCHEME_VIEWED`, `SCHEME_EXPANDED`, `SCHEME_SAVED`, `APPLICATION_STARTED`) receive relevance grades 0–2, but **do not increment** the legitimate outcome session counter.

---

## 6. Continuous Readiness Monitoring State Machine

```
   [Current State: 0 legitimate outcomes]
                   │
                   ▼
         TRAINING_NOT_READY
         trainingReady = false
         modelTrainingAllowed = false
         modelPromotionAllowed = false
                   │
         [Natural Accumulation]
                   │
                   ▼
         [100 Legitimate Outcomes]
                   │
                   ▼
           TRAINING_READY
         trainingReady = true
         modelTrainingAllowed = true
         modelPromotionAllowed = false
                   │
                   ▼
       [PRE-TRAINING VALIDATION]
                   │
                   ▼
          [HUMAN GOVERNANCE]
```

Under no circumstances does `TRAINING_READY` automatically trigger model training or model promotion. Model promotion remains a distinct, separately authorized governance step.
