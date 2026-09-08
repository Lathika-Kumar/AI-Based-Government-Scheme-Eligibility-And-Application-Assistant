# PHASE 36 — PRE-TRAINING DATA READINESS & BEHAVIORAL ML TRAINING GATE

## 1. Executive Summary

Phase 36 establishes a strict pre-training gate and validates the handoff boundary between the **current-citizen recommendation engine** and the **eventual behavioral ML training pipeline**.

### Verified System Baselines

- **Historical citizens**: 0
- **Historical behavioral records**: 0
- **Legitimate outcome sessions**: 0 / 100
- **Required outcome sessions threshold**: 100
- **Synthetic events generated**: 0
- **Historical synthetic application fixtures**: 29 (all quarantined)
- **Active recommender**: `2.2.0-hybrid-semantic-384d`
- **Fallback recommender**: `1.0.0-deterministic`
- **Circuit breaker**: 200 ms
- **Model training**: NOT EXECUTED (`modelTrainingAllowed = false`)
- **Model promotion**: NOT EXECUTED (`modelPromotionAllowed = false`)
- **Statutory eligibility violation rate**: 0.00%
- **Production MongoDB mutations**: `INSERT=0, UPDATE=0, DELETE=0, DROP=0`

---

## 2. Core Operational Distinctions

```
┌────────────────────────────────────────────────────────┐
│              CURRENT-CITIZEN RECOMMENDATION            │
│                       STATUS: READY                    │
├────────────────────────────────────────────────────────┤
│ • Driven strictly by authenticated user's profile.     │
│ • Evaluated against canonical schemes & criteria.      │
│ • EligibilityEngine.evaluate() is the statutory authority.│
│ • Ineligible / Insufficient data schemes discarded.    │
│ • Operates with 100% precision on cold start.          │
│ • Zero dependence on historical user interactions.    │
└────────────────────────────────────────────────────────┘

                           vs.

┌────────────────────────────────────────────────────────┐
│                 BEHAVIORAL ML TRAINING                 │
│              STATUS: NOT READY / NOT EXECUTED          │
├────────────────────────────────────────────────────────┤
│ • Historical behavioral data: 0 records.               │
│ • Legitimate outcome sessions: 0 / 100.                │
│ • Model training: Strictly disabled (Gate: BLOCKED).   │
│ • Model promotion: Strictly disabled (Firewall: ACTIVE)│
│ • Zero synthetic data permitted to inflate counts.     │
└────────────────────────────────────────────────────────┘
```

---

## 3. Two Architectural Milestones

As established in the architectural roadmap:

### Milestone 1 (NOW — Phase 36): Current-User Recommendation READY
The production platform is fully operational for current authenticated citizens:
```
    CURRENT CITIZEN PROFILE
               │
               ▼
    PROFILE NORMALIZATION
               │
               ▼
    EligibilityEngine.evaluate()
               │
         ┌─────┴─────┐
         ▼           ▼
   NOT_ELIGIBLE   ELIGIBLE
         ▼           ▼
      DISCARD      RANKING (2.2.0-hybrid-semantic-384d)
                     ▼
             TOP-K RECOMMENDATIONS
```
No historical interactions are needed. Citizens receive personalized, statutorily verified recommendations immediately upon login.

### Milestone 2 (FUTURE — Upon Reaching 100 Genuine Outcomes): Behavioral ML Lifecycle
Only when legitimate citizen interactions naturally accumulate 100+ verified outcome sessions:
```
    GENUINE CITIZEN TELEMETRY ACCUMULATION
                     │
                     ▼
    100+ LEGITIMATE OUTCOME SESSIONS REACHED
                     │
                     ▼
    PRE-TRAINING VALIDATION & LEAKAGE AUDIT
                     │
                     ▼
    OFFLINE MODEL TRAINING (SANDBOX)
                     │
                     ▼
    OFFLINE METRIC EVALUATION
                     │
                     ▼
    HUMAN GOVERNANCE & EXPLICIT PROMOTION
```
Reaching 100 outcomes permits offline sandbox training; it **NEVER automatically promotes** a model to production.

---

## 4. Invariants Enforced in Phase 36

1. **EligibilityEngine is Sole Authority**: Statutory evaluation strictly precedes ranking. No ML score can override an ineligible statutory determination.
2. **Missing Data Safety**: Incomplete criteria produce `INSUFFICIENT_DATA`. Attributes are never guessed or defaulted.
3. **No Target Leakage**: Future outcomes (`SCHEME_APPLIED`, `APPLICATION_COMPLETED`) are never ranking features.
4. **Browsing Sessions Decoupled**: Clicks, views, and expansions do not count as legitimate conversion outcomes.
5. **PII Isolation**: Zero forbidden PII (Aadhaar, PAN, phone, email, address) in logs, recommendations, or telemetry.
6. **Synthetic Quarantine**: All 29 historical application fixtures remain permanently isolated.
7. **Read-Only DB Safety**: Zero production database mutations (`INSERT=0, UPDATE=0, DELETE=0, DROP=0`).
