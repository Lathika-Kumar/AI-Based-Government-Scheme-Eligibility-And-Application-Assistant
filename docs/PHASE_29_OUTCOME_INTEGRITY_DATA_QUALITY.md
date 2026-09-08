# PHASE 29 — PRODUCTION OUTCOME INTEGRITY, DATA QUALITY MONITORING & AUTOMATED ML TRAINING GATE

## Executive Summary

Phase 29 of SchemeBridge implements a production-grade telemetry quality monitoring service, a deterministic 7-way telemetry classification engine, recursive zero-PII validation, session sequence integrity checking, feature/label target leakage detection, and a centralized automated ML training readiness gate (`TrainingReadinessGate`).

All implementations strictly adhere to the project's non-negotiable architectural invariants:
1. `EligibilityEngine.evaluate()` remains the sole and mandatory statutory eligibility authority.
2. `STATUTORY_ELIGIBILITY_VIOLATION_RATE = 0.00%`.
3. Production MongoDB remains strictly read-only for audit and offline analysis (`INSERT=0, UPDATE=0, DELETE=0, DROP=0`).
4. All 29 historical synthetic `application_events` remain permanently quarantined.
5. Zero synthetic telemetry, clicks, applications, or labels were manufactured.
6. The active recommendation model remains `2.2.0-hybrid-semantic-384d`, fallback remains `1.0.0-deterministic`, and circuit breaker remains `200 ms`.
7. No ML model was trained, fine-tuned, or promoted.
8. Current training readiness status strictly evaluates to `TRAINING_NOT_READY` because genuine outcome sessions = 0 (< 100 required threshold).

---

## 1. Architecture Overview

```
                      [Citizen Interaction Telemetry]
                                    │
                                    ▼
                 ┌──────────────────────────────────────┐
                 │  Phase29TelemetryQualityMonitor      │
                 │  - Deterministic 7-Way Classifier    │
                 │  - Recursive Zero-PII Validator      │
                 │  - Chronological Sequence Validator  │
                 │  - Synthetic Fixture Quarantine      │
                 └──────────────────┬───────────────────┘
                                    │
                  ┌─────────────────┴─────────────────┐
                  ▼                                   ▼
        [Ineligible / Quarantined]           [Valid Telemetry]
        - 29 Synthetic Fixtures                       │
        - PII Violations                              ▼
        - Malformed / Orphans            ┌──────────────────────────┐
        - Invalid Sequences              │ Session Event Aggregator │
                                         │ Terminal Grade: 0,1,2,3  │
                                         └────────────┬─────────────┘
                                                      │
                                                      ▼
                                         ┌──────────────────────────┐
                                         │  TargetLeakageDetector   │
                                         │  (Feature / Target Split)│
                                         └────────────┬─────────────┘
                                                      │
                                                      ▼
                                         ┌──────────────────────────┐
                                         │  TrainingReadinessGate   │
                                         │  Threshold: 100 Sessions │
                                         └────────────┬─────────────┘
                                                      │
                            ┌─────────────────────────┴─────────────────────────┐
                            ▼                                                   ▼
                [Outcome Sessions < 100]                            [Outcome Sessions >= 100]
                Status: TRAINING_NOT_READY                          Status: TRAINING_READY
                modelTrainingAllowed: false                         modelTrainingAllowed: true
                modelPromotionAllowed: false                        modelPromotionAllowed: false
                                                                    (Separately Authorized Phase)
```

---

## 2. Telemetry Quality Classification

Every incoming interaction telemetry event is classified into one of seven disjoint categories:

| Status | Description | Action |
|---|---|---|
| `VALID` | Passes all structural, schemeCode, sequence, and zero-PII checks. Non-synthetic. | Included in session aggregation |
| `DUPLICATE` | Identical event key, timestamp, schemeCode, and sessionId within session stream. | Excluded from attribution |
| `SYNTHETIC` | Matches known synthetic fixture identifiers, test user prefixes, or quarantine IDs. | Excluded permanently |
| `MALFORMED` | Missing `sessionId`, missing `schemeCode`, invalid event type, or future timestamp. | Excluded |
| `PII_VIOLATION` | Contains prohibited PII fields in root, metadata, payloads, or nested structures. | Quarantined; PII never logged |
| `ORPHAN` | Interaction event without an initial `RECOMMENDATION_SHOWN` anchor. | Excluded from session grading |
| `INVALID_SEQUENCE`| Event ordering violates natural citizen interaction workflow. | Excluded from terminal grading |

---

## 3. Legitimate Outcome Session Specification

A session is considered a **Legitimate Outcome Session** if and only if all of the following criteria are satisfied:
1. **Valid Session ID**: Non-empty, non-synthetic, not matching test fixture patterns (`test_`, `fixture_`, `mock_`).
2. **Canonical Scheme Code**: Must match an active, statutory canonical scheme code.
3. **Zero PII**: Recursively audited across all event payloads and metadata; zero forbidden keys.
4. **Non-Synthetic**: Not matching quarantined historical records or known fixture patterns.
5. **No Duplication**: Event hashes are unique within the session context.
6. **Chronological Validity**: Timestamps are non-future, non-negative, monotonically non-decreasing.
7. **Outcome Event Present**: Must contain at least one terminal outcome event:
   - `SCHEME_APPLIED`, or
   - `APPLICATION_COMPLETED`.
8. **Statutory Eligibility Authority**: Schemes within the session must have been verified as eligible by `EligibilityEngine.evaluate()`. Ineligible schemes are strictly forbidden from entering training pairs.

---

## 4. Relevance Attribution & Monotonic Terminal Grading

Relevance grades strictly follow the Phase 27 definition:
- `0` = `IMPRESSED_UNENGAGED` (Scheme recommended, no further interaction)
- `1` = `VIEWED` (Scheme details viewed)
- `2` = `INTENT_HIGH` (Application initiated / eligibility checked)
- `3` = `CONVERTED` (Scheme application submitted / completed)

Terminal grade is computed deterministically:
$$\text{terminalGrade} = \max_{e \in \text{SessionEvents}} (\text{grade}(e))$$

---

## 5. Automated Training Readiness Gate

The `TrainingReadinessGate` centralizes readiness evaluation:
- **Threshold**: Minimum **100 legitimate outcome sessions**.
- **Current Production Evaluation**:
  - Legitimate outcome sessions: `0`
  - Synthetic fixtures quarantined: `29`
  - Status: `TRAINING_NOT_READY`
  - `modelTrainingAllowed`: `false`
  - `modelPromotionAllowed`: `false`
- **Future Ready State (When >= 100)**:
  - Status: `TRAINING_READY`
  - `modelTrainingAllowed`: `true`
  - `modelPromotionAllowed`: `false` (Model promotion requires a distinct, explicit governance step).

---

## 6. Target Leakage Detector

`TargetLeakageDetector` strictly checks `UserSchemeFeatureVector` and its `rankingContext` map:
- Target labels (`relevanceGrade`, `label`, `target`) must **never** appear inside feature vectors.
- Post-ranking behavioral outcomes (`applicationOutcome`, `clickOutcome`, `eventType`, `completionState`) are strictly barred from features.
- If any forbidden key is detected, an `IllegalStateException` is thrown and dataset generation is halted safely.

---

## 7. Zero-PII Recursive Enforcement

Forbidden keys audited across all maps, lists, and primitives:
`aadhaar`, `uid`, `pan`, `phone`, `mobile`, `email`, `name`, `firstname`, `lastname`, `address`, `street`, `ip`, `location`, `pincode`, `udid`, `lat`, `lon`.

If a forbidden key is encountered:
1. Event is flagged as `PII_VIOLATION`.
2. The key name is recorded in audit metrics.
3. The value is **never written to logs or storage**.
4. The event is quarantined and prevented from entering training datasets.
