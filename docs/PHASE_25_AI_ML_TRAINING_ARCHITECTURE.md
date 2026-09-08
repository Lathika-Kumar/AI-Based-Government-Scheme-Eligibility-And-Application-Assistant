# PHASE 25: AI/ML CONTINUOUS LEARNING & TRAINING ARCHITECTURE

**System**: SchemeBridge Production Recommendation & Intelligence Platform  
**Phase**: Phase 25 — AI/ML Continuous Learning, Feedback-Driven Training & Safe Model Promotion  
**Safety Mandate**: Statutory Eligibility Gate Strictly Precedes ML | Zero Database Mutations | Zero Fabrication  

---

## 1. Architectural Overview

SchemeBridge requires a reliable, reproducible, privacy-preserving, and non-regressive continuous learning pipeline to adapt scheme ranking over time as citizens interact with recommendations and submit applications.

### Continuous Feedback Loop Lifecycle
```
+-----------------------------------------------------------------------------------+
| 1. Citizen Interaction: Browses recommendations, views details, saves, or applies  |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼
+-----------------------------------------------------------------------------------+
| 2. Client Telemetry: Deduplicated event dispatch with rank, score & session ID    |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼
+-----------------------------------------------------------------------------------+
| 3. Storage Layer: Persisted in MongoDB recommendation_events (0 PII retained)     |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼
+-----------------------------------------------------------------------------------+
| 4. Offline Dataset Builder: scripts/build_phase25_training_dataset.py             |
|    - Excludes synthetic test fixtures                                             |
|    - Validates minimum volume threshold (>= 100 legitimate outcome sessions)       |
|    - Generates deterministic scheme/citizen isolated train/val/test splits        |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼
+-----------------------------------------------------------------------------------+
| 5. Offline Model Training: Only runs when legitimate data supports it            |
|    (Currently: TRAINING_NOT_READY — Zero fabricated models or synthetic weights)   |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼
+-----------------------------------------------------------------------------------+
| 6. Offline Evaluation: scripts/evaluate_phase25_recommendations.py                |
|    - Precision@K, Recall@K, NDCG@K, MRR, Latency, Eligibility Violations         |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼
+-----------------------------------------------------------------------------------+
| 7. Model Registry & Promotion Gates: ModelRegistryService.java                    |
|    - Gate 1: Legitimate training data exists                                      |
|    - Gate 2: Offline evaluation metrics present                                   |
|    - Gate 3: Statutory Eligibility Violation Rate == 0.00%                        |
|    - Gate 4: Inference Latency <= 200ms                                           |
|    - Gate 5: Zero critical NDCG metric regression vs Active Baseline             |
|    - Gate 6: Deterministic fallback remains available                             |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼
+-----------------------------------------------------------------------------------+
| 8. Shadow Mode: Candidate evaluated side-by-side without affecting citizens        |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼
+-----------------------------------------------------------------------------------+
| 9. Production Activation / Rollback: Active model serves live queries             |
|    - Instant rollback to previous approved model if anomaly detected              |
+-----------------------------------------------------------------------------------+
```

---

## 2. Non-Negotiable Statutory Eligibility Safety Architecture

ML models must **NEVER** determine statutory eligibility, override legal criteria, or resurrect an ineligible scheme.

```
Citizen Profile Attributes
         │
         ▼
EligibilityEngine.evaluate(profile, scheme)
         │
         ▼
REMOVE INELIGIBLE SCHEMES (Score = 0.0)
         │
         ▼
Only ELIGIBLE Candidate Schemes
         │
         ▼
ML / Semantic Ranking Model
         │
         ▼
Top-K Ranked Recommendations
         │
         ▼
Explainable Recommendation (Zero ML Jargon)
```

### Safety Invariants
1. **Statutory Eligibility Violation Rate = 0.00%**: Ineligible schemes are filtered before ranking and are never surfaced.
2. **Circuit Breaker Budget**: Maximum 200 ms inference budget. If ML ranking times out or encounters memory faults, `DETERMINISTIC_FALLBACK` activates instantly.
3. **Document Checklist Invariance**: Canonical document checklists from `scheme_verified_data` supersede any heuristic or prediction. Hallucination rate = 0.00%.

---

## 3. Model Registry & Lifecycle State Machine

`ModelRegistryService.java` enforces strict state transitions:

```
[NEW CANDIDATE]
       │
       ▼
   CANDIDATE ──(fails gates)──> REJECTED
       │
  (passes criteria)
       ▼
    SHADOW ────(fails test)───> REJECTED
       │
  (verified superior)
       ▼
   APPROVED
       │
   (promoted)
       ▼
    ACTIVE ────(anomaly)──────> ROLLED_BACK (Restores previous ACTIVE)
```

### Registered Model Inventory
| Version | Model Type | Status | Role | NDCG@5 | Latency | Violation Rate |
|---|---|---|---|---|---|---|
| `2.2.0-hybrid-semantic-384d` | HYBRID_SEMANTIC_MADM | **ACTIVE** | Current Production Model | 0.6384 | 8.23 ms | 0.00% |
| `1.0.0-deterministic` | DETERMINISTIC_MADM | **APPROVED** | Authoritative Fallback | 0.5883 | 1.09 ms | 0.00% |
| `phase25-candidate-v1` | LEARNING_TO_RANK | **REJECTED** | Awaiting legitimate data | N/A | N/A | 0.00% |

---

## 4. Telemetry Event Specification

To ensure future supervised training is grounded in real citizen behavior, the telemetry layer captures genuine behavioral progression:

| Event Type | User Trigger | Weight in Future Training |
|---|---|---|
| `RECOMMENDATION_SHOWN` | Visible recommendation card rendered | 0.0 (Impression baseline) |
| `SCHEME_VIEWED` | Citizen clicked card to open overview | 0.2 (Initial curiosity) |
| `SCHEME_EXPANDED` | Citizen expanded detailed eligibility criteria | 0.4 (High interest) |
| `SCHEME_SAVED` | Citizen bookmarked scheme to personal profile | 0.6 (Intent signal) |
| `APPLICATION_STARTED` | Citizen launched Application Wizard | 0.8 (Strong intent) |
| `SCHEME_APPLIED` | Citizen submitted completed application | 1.0 (Conversion) |
| `APPLICATION_COMPLETED`| Verification officer approved application | 1.5 (Positive outcome) |

### Anti-Flooding & Deduplication
- React components use `trackedImpressionsRef` and session storage to record impression events once per session.
- Synthetic and automated test fixtures (`userId` containing "test", "citizen_user", or known test fixture IDs) are strictly segregated.

---

## 5. Privacy & Zero-PII Policy

In compliance with national data protection standards:
- **No PII in ML datasets**: Names, emails, phone numbers, Aadhaar numbers, and street addresses are excluded.
- Feature vectors utilize only categorical indices and normalized attributes (e.g., occupation category, income tier, state code, demographic category).
- Offline training scripts run in read-only mode against MongoDB with zero data mutation.
