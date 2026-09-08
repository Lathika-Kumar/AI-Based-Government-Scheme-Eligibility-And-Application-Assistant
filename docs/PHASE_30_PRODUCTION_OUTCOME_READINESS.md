# PHASE 30 — PRODUCTION OUTCOME ACCUMULATION & READINESS ARCHITECTURE

## 1. Executive Summary

Phase 30 establishes the production outcome accumulation, readiness transition, and Learning-to-Rank (LTR) pre-training validation framework for SchemeBridge.

The primary objective of Phase 30 is to enable safe, deterministic accumulation of genuine citizen interaction telemetry until the system reaches the statutory threshold of **100 legitimate outcome sessions**. At this threshold, the platform transitions deterministically from `TRAINING_NOT_READY` to `TRAINING_READY`, while strictly maintaining model promotion barriers and preserving active production recommender configurations.

---

## 2. Architectural Invariants

| Invariant | Specification | Enforcement Mechanism |
|---|---|---|
| **Statutory Eligibility Authority** | Sole authority: `EligibilityEngine.evaluate()`. Violation rate = 0.00%. | First gate in `SessionAttributionService` and recommendation pipeline. Ineligible schemes never enter feature extraction or training pairs. |
| **Production Database Safety** | Strictly read-only for audit, evaluation, and dataset generation pipelines: `INSERT=0, UPDATE=0, DELETE=0, DROP=0`. | Verified by `scripts/audit_mongo_phase30.py` and regression tests. |
| **Zero Fabrication** | Zero synthetic clicks, views, applications, completions, sessions, or labels manufactured. | No artificial data generated. Metric reports withhold NDCG/MAP when data is insufficient. |
| **Synthetic Quarantine** | 29 historical synthetic `application_events` remain permanently quarantined. | Hardcoded quarantine filters in `SessionAttributionService`, `Phase29TelemetryQualityMonitor`, `ProductionOutcomeAccumulator`, and offline scripts. |
| **Active Recommender Preserved** | Active: `2.2.0-hybrid-semantic-384d`, Fallback: `1.0.0-deterministic`, Circuit Breaker: 200 ms. | Unchanged across all services and configurations. |
| **Model Promotion Disabled** | `modelPromotionAllowed = false` strictly enforced even when `TRAINING_READY`. | Enforced by `TrainingReadinessGate` and `ProductionOutcomeAccumulator`. |

---

## 3. End-to-End Outcome Flow

```
                      [Real Citizen Interaction]
                                  │
                                  ▼
                     [Recommendation Telemetry]
                                  │
                                  ▼
                  ┌───────────────────────────────┐
                  │ Phase29TelemetryQualityMonitor│
                  └───────────────┬───────────────┘
                                  │
                 ┌────────────────┼────────────────┐
                 ▼                ▼                ▼
           [SYNTHETIC]      [PII_VIOLATION]   [MALFORMED/DUP]
           Quarantined        Quarantined        Excluded
                                  │
                                  ▼
                         [VALID TELEMETRY]
                                  │
                                  ▼
                  ┌───────────────────────────────┐
                  │ ProductionOutcomeAccumulator  │
                  │ Group by: sessionId+schemeCode│
                  │ Chronological Sequence Check  │
                  └───────────────┬───────────────┘
                                  │
                                  ▼
                  ┌───────────────────────────────┐
                  │  SessionAttributionService    │
                  │  Terminal Grade: 0, 1, 2, 3   │
                  └───────────────┬───────────────┘
                                  │
                                  ▼
                  ┌───────────────────────────────┐
                  │  EligibilityEngine.evaluate() │
                  │  (Statutory Eligibility Gate) │
                  └───────────────┬───────────────┘
                                  │
                                  ▼
                  ┌───────────────────────────────┐
                  │    TargetLeakageDetector      │
                  │  (0 leakage keys permitted)   │
                  └───────────────┬───────────────┘
                                  │
                                  ▼
                  ┌───────────────────────────────┐
                  │     TrainingReadinessGate     │
                  │   Threshold = 100 sessions    │
                  └───────────────┬───────────────┘
                                  │
              ┌───────────────────┴───────────────────┐
              ▼                                       ▼
    [Outcomes < 100]                        [Outcomes >= 100]
    TRAINING_NOT_READY                      TRAINING_READY
    trainingReady = false                   trainingReady = true
    modelTrainingAllowed = false            modelTrainingAllowed = true
    modelPromotionAllowed = false           modelPromotionAllowed = false
                                                      │
                                                      ▼
                                            [PRE-TRAINING VALIDATION]
                                                      │
                                                      ▼
                                            [EXPLICIT GOVERNANCE STEP]
```

---

## 4. Legitimate Outcome Session Definition

A session is classified as a **Legitimate Outcome Session** if and only if:
1. It originates from genuine citizen telemetry (non-synthetic).
2. It contains valid `sessionId` and canonical `schemeCode`.
3. All telemetry passes recursive zero-PII inspection.
4. It is free from event duplication and sequence inversions.
5. It includes an initial recommendation anchor (`RECOMMENDATION_SHOWN`).
6. The scheme is confirmed statutory eligible by `EligibilityEngine.evaluate()`.
7. The session contains at least one legitimate conversion outcome:
   - `SCHEME_APPLIED`, or
   - `APPLICATION_COMPLETED`.

Events representing interest or exploration alone (`RECOMMENDATION_SHOWN`, `SCHEME_VIEWED`, `SCHEME_EXPANDED`, `SCHEME_SAVED`, `APPLICATION_STARTED`) receive terminal relevance grades 0–2, but **do not increment the legitimate outcome session counter**.

---

## 5. Automated Readiness State Machine

| Legitimate Outcomes | Status | `trainingReady` | `modelTrainingAllowed` | `modelPromotionAllowed` |
|---|---|---|---|---|
| **0 – 99** | `TRAINING_NOT_READY` | `false` | `false` | `false` |
| **Exactly 100** | `TRAINING_READY` | `true` | `true` | `false` |
| **> 100** | `TRAINING_READY` | `true` | `true` | `false` |

> [!IMPORTANT]
> `TRAINING_READY` authorizes offline model experimentation in a sandboxed environment only. Under no circumstances does it trigger automatic model training or model promotion. Model promotion remains a distinct, human-governed, explicitly authorized lifecycle stage.
