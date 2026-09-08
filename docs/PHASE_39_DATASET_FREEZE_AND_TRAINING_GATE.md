# PHASE 39 — DATASET FREEZE, LEAKAGE AUDIT & OFFLINE TRAINING READINESS

## 1. Executive Summary

Phase 39 establishes the complete, production-safe boundary for eventual behavioral Machine Learning (ML) training on SchemeBridge.

### Core Architectural Invariant
The platform maintains a strict separation between two independent lifecycles:
1. **Current-Citizen Recommendation (READY)**: Personalization operates directly on the current authenticated citizen profile, evaluated through statutory criteria by `EligibilityEngine.evaluate()`, and ranked by active model `2.2.0-hybrid-semantic-384d` with `1.0.0-deterministic` fallback. It requires **zero historical users** and **zero past telemetry**.
2. **Behavioral ML Training (NOT READY / NOT EXECUTED)**: Supervised offline training requires a qualified, frozen dataset containing at least 100 legitimate outcome sessions. Because SchemeBridge is a fresh production platform, historical users are 0 and legitimate outcomes are 0 / 100. Behavioral training is strictly **BLOCKED** at the gate.

> [!NOTE]
> Having 0 historical users and 0 / 100 legitimate outcome sessions is **not a system failure**. It is the expected, authentic baseline state for a new platform awaiting genuine citizen usage. Fabricating data to bypass this threshold is strictly forbidden.

---

## 2. Authoritative Baseline State

- **Historical users**: `0`
- **Historical behavioral records**: `0`
- **Legitimate outcome sessions**: `0 / 100`
- **Required outcome threshold**: `100`
- **Remaining sessions needed**: `100`
- **Synthetic events generated**: `0`
- **Historical synthetic application fixtures**: `29` (all quarantined)
- **Active recommender**: `2.2.0-hybrid-semantic-384d`
- **Fallback recommender**: `1.0.0-deterministic`
- **Circuit breaker**: `200 ms`
- **Current-user recommendation**: `READY`
- **Behavioral ML training**: `NOT READY / NOT EXECUTED`
- **Dataset freeze status**: `BLOCKED_BELOW_THRESHOLD`
- **modelTrainingAllowed**: `false`
- **modelPromotionAllowed**: `false`
- **Statutory eligibility violation rate**: `0.00%`
- **PII violations**: `0`
- **Target leakage violations**: `0`
- **Production MongoDB mutations**: `INSERT=0, UPDATE=0, DELETE=0, DROP=0`

---

## 3. Dataset Freeze Contract (`DatasetFreezeService.java`)

Before any offline behavioral training run can be initiated, the candidate dataset must undergo an immutable dataset freeze:
- **Read-only**: Guarantees zero mutations against production collections.
- **Pre-requisite Check**: If legitimate outcomes are below 100, the freeze is deterministically rejected (`BLOCKED_BELOW_THRESHOLD`, `datasetVersion = "NONE"`, `integritySha256Hash = "NONE"`).
- **Cryptographic Hashing**: When >= 100 qualified sessions exist, a canonical representation (including version, session count, timestamp, recommender version, eligibility engine version, and source event IDs) is hashed using SHA-256.
- **Immutability**: Frozen snapshots wrap all event and example lists using unmodifiable views (`Collections.unmodifiableList`).

---

## 4. Feature & Label Separation (`TrainingExample.java`)

To prevent target leakage:
- **Features (`TrainingFeatures`)**:
  - Demographics derived from profile (age, gender, state, district, occupation, income tier)
  - Scheme static attributes (ministry, category, benefits)
  - Recommendation context (rank position, total catalog evaluated)
  - Prior interaction sequence *strictly before* the target outcome (e.g., `RECOMMENDATION_SHOWN`, `SCHEME_VIEWED`)
- **Labels (`TrainingLabel`)**:
  - Target event type (`APPLICATION_COMPLETED`, `SCHEME_APPLIED`)
  - Conversion indicator (`converted = true/false`)
  - Relevance grade (`CONVERTED`, `INTENT_HIGH`, `VIEWED`, `IMPRESSED_UNENGAGED`)
  - Terminal event timestamp

Prohibited in features: `SCHEME_APPLIED`, `APPLICATION_COMPLETED`, future application status, future approval status, or terminal outcome grades. Any attempt to construct a `TrainingExample` with leaked signals triggers an immediate `IllegalStateException`.

---

## 5. Training Gate & Promotion Firewall (`Phase39TrainingGate.java`)

Training is permitted **ONLY** when all 10 conditions are simultaneously satisfied:
1. `legitimateOutcomeSessions >= 100`
2. `trainingReady = true`
3. `modelTrainingAllowed = true`
4. `syntheticContamination = 0`
5. `piiViolations = 0`
6. `targetLeakageViolations = 0`
7. Recommendation anchors valid (`RECOMMENDATION_SHOWN` opening event)
8. Chronological sequence valid
9. Dataset freeze successful (`FROZEN_SUCCESS`)
10. Integrity SHA-256 hash verified

If any condition fails, the gate blocks execution. Calling `enforceTrainingGate()` while blocked throws an `IllegalStateException`.

Even after an offline candidate model is trained in the future, **`modelPromotionAllowed = false` remains permanently enforced**. Model promotion is strictly reserved for human deployment authority and can never be automated.
