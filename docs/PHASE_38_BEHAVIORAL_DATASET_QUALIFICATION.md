# PHASE 38 — GENUINE OUTCOME DATASET QUALIFICATION & BEHAVIORAL TRAINING UNLOCK GATE

## 1. Executive Summary

Phase 38 establishes a dedicated pre-training dataset qualification layer for SchemeBridge. Building on the operational current-citizen recommendation engine validated in Phase 37, this phase implements automated verification to determine when naturally accumulated citizen outcomes qualify for future offline behavioral ML training.

### Authoritative Baseline & Current State
- **Historical citizens**: 0
- **Historical behavioral records**: 0
- **Legitimate outcome sessions**: 0 / 100
- **Required outcome sessions threshold**: 100
- **Remaining outcomes required**: 100
- **Synthetic events generated**: 0
- **Historical synthetic application fixtures**: 29 (all quarantined)
- **Active recommender**: `2.2.0-hybrid-semantic-384d`
- **Fallback recommender**: `1.0.0-deterministic`
- **Circuit breaker**: 200 ms
- **Current-user recommendation**: `READY`
- **Behavioral ML training**: `NOT READY / NOT EXECUTED`
- **modelTrainingAllowed**: `false`
- **modelPromotionAllowed**: `false`
- **PII violations**: 0
- **Target leakage violations**: 0
- **Production MongoDB mutations**: `INSERT=0, UPDATE=0, DELETE=0, DROP=0`

---

## 2. Architectural Objective & Qualification Lifecycle

```
    REAL AUTHENTICATED CITIZEN
                 │
                 ▼
          CURRENT PROFILE
                 │
                 ▼
        ELIGIBILITY ENGINE
                 │
                 ▼
         RECOMMENDATIONS
                 │
                 ▼
         GENUINE TELEMETRY
                 │
                 ▼
       VALID OUTCOME SESSION
                 │
                 ▼
        OUTCOME ACCUMULATION
                 │
                 ▼
        DATASET QUALIFICATION (BehavioralTrainingDatasetQualificationService)
                 │
         ┌───────┴────────┐
         ▼                ▼
     < 100 SESSIONS     >= 100 SESSIONS
         │                │
         ▼                ▼
     NOT_READY      TRAINING_ELIGIBLE
   (Training locked)      │
                          ▼
              MANUAL OFFLINE TRAINING MAY BEGIN
              (Model promotion permanently manual)
```

---

## 3. Qualification Requirements

The pre-training dataset qualification layer enforces seven statutory and operational gates before declaring a candidate dataset eligible:

1. **Requirement A — Genuine Data**: Outcomes must strictly originate from genuine citizen interactions. Synthetic fixtures never qualify.
2. **Requirement B — Minimum Volume**: At least 100 legitimate outcome sessions are required. Current state is `0 / 100` (`trainingReady = false`).
3. **Requirement C — Valid Recommendation Anchor**: Every qualifying session must possess a valid `RECOMMENDATION_SHOWN` anchor. Orphan events lacking a recommendation context are discarded.
4. **Requirement D — Valid Chronological Sequence**: Events must adhere to forward progression (`RECOMMENDATION_SHOWN` -> `SCHEME_VIEWED` -> `APPLICATION_STARTED` -> `SCHEME_APPLIED` -> `APPLICATION_COMPLETED`). Inverted or impossible sequences are rejected.
5. **Requirement E — Zero Synthetic Contamination**: Historical fixtures (29 fixtures) remain isolated in permanent quarantine, contributing 0 to training volume.
6. **Requirement F — Zero PII**: Features, telemetry payloads, and metadata are audited to ensure complete absence of Aadhaar, PAN, phone, email, and address.
7. **Requirement G — Zero Target Leakage**: Input features used for ranking/scoring are strictly decoupled from target labels. Future terminal outcomes (`SCHEME_APPLIED`, `APPLICATION_COMPLETED`, etc.) are prohibited from input feature vectors.

---

## 4. Feature and Label Separation (`TrainingExample` Contract)

Phase 38 formalizes the training example contract (`TrainingExample.java`), dividing every training observation into:
- **Features (`TrainingFeatures`)**:
  - Profile-derived demographic attributes (age, state, district, occupation, social category, income tier)
  - Scheme static characteristics (ministry, category, benefits, tags)
  - Recommendation context (rank position, catalog evaluated, model version)
  - Prior interaction history occurring *strictly prior* to the target outcome
- **Label (`TrainingLabel`)**:
  - Target event type (`APPLICATION_COMPLETED`, `SCHEME_APPLIED`)
  - Conversion indicator (`converted = true/false`)
  - Relevance grade (`CONVERTED`, `INTENT_HIGH`, `VIEWED`, `IMPRESSED_UNENGAGED`)
  - Terminal event timestamp

Construction of any `TrainingExample` containing forbidden leakage keys throws an immediate `IllegalStateException`.

---

## 5. Distinction: Current-Citizen Engine vs. Future Behavioral ML

| Dimension | Current-Citizen Recommendation | Future Behavioral ML Model |
| :--- | :--- | :--- |
| **Status** | **READY (Active in Production)** | **NOT READY / NOT EXECUTED** |
| **Personalization Source**| Current authenticated citizen profile | Accumulated genuine telemetry |
| **Eligibility Authority**| Statutory `EligibilityEngine.evaluate()` | Statutory `EligibilityEngine.evaluate()` |
| **Model In Production** | `2.2.0-hybrid-semantic-384d` | None (Offline candidate only when qualified) |
| **Fallback Recommender** | `1.0.0-deterministic` (200 ms circuit breaker) | `1.0.0-deterministic` |
| **Promotion Authority** | Production verified | Explicit human governance only |
