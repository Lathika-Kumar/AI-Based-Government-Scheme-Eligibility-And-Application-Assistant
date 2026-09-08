# PHASE 35 — CURRENT-CITIZEN PERSONALIZED RECOMMENDATION ARCHITECTURE

## 1. Executive Summary

Phase 35 hardens and validates the production recommendation pipeline of the SchemeBridge platform for a **CURRENT authenticated citizen** who has no historical interaction history (`historicalUsers = 0`, `historicalBehavioralRecords = 0`).

The platform's verified state establishes:
- **Historical citizens**: 0
- **Historical behavioral records**: 0
- **Legitimate outcome sessions**: 0 / 100
- **Synthetic events generated**: 0
- **Historical synthetic application fixtures**: 29 (all quarantined)
- **Active recommender**: `2.2.0-hybrid-semantic-384d`
- **Fallback recommender**: `1.0.0-deterministic`
- **Circuit breaker**: 200 ms
- **Model training**: NOT EXECUTED
- **Model promotion**: NOT EXECUTED
- **Statutory eligibility violation rate**: 0.00%
- **Production DB mutations**: `INSERT=0, UPDATE=0, DELETE=0, DROP=0`

### Distinction of Readiness States

> **CURRENT-USER RECOMMENDATION**: **`READY`**  
> Any new authenticated citizen can immediately receive personalized, statutorily verified recommendations on cold start.

> **BEHAVIORAL ML TRAINING**: **`NOT READY / NOT EXECUTED`**  
> Supervised model retraining remains strictly disabled until genuine citizen telemetry accumulates 100+ legitimate outcome sessions.

---

## 2. Recommendation Pipeline

The recommendation pipeline guarantees that statutory eligibility evaluation strictly precedes ranking:

```
    CURRENT AUTHENTICATED CITIZEN
              │
              ▼
    CURRENT USER PROFILE
              │
              ▼
    PROFILE NORMALIZATION
              │
              ▼
    EligibilityEngine.evaluate()
              │
              ├──── NOT_ELIGIBLE ──────► Discard (never returned)
              │
              ├──── INSUFFICIENT_DATA ──► Conservative handling
              │
              ▼
    ELIGIBLE SCHEMES ONLY
              │
              ▼
    EXISTING RECOMMENDER / RANKING (2.2.0-hybrid-semantic-384d)
              │
              ▼
    PERSONALIZED TOP-K RECOMMENDATIONS
```

---

## 3. Non-Negotiable Architectural Invariants

### 1. Current User Only
- Recommendations are computed strictly from the authenticated citizen's current profile, canonical scheme data, and statutory eligibility rules.
- No reliance on prior users, collaborative filtering, or imaginary "similar citizens".

### 2. EligibilityEngine is the Sole Statutory Authority
- `EligibilityEngine.evaluate()` is the statutory authority.
- Every scheme passing into the ranking stage is validated as `ELIGIBLE`. Schemes marked `NOT_ELIGIBLE` or `INSUFFICIENT_DATA` are discarded prior to ranking.
- Statutory eligibility rules are never duplicated in the frontend or ranking layers.

### 3. Missing Data Safety (`INSUFFICIENT_DATA`)
- Missing or unknown profile attributes are never guessed, inferred, or given arbitrary defaults.
- Incomplete criteria evaluations yield deterministic `INSUFFICIENT_DATA` and cannot produce false-positive eligibility.

### 4. Cold-Start Support
- A newly authenticated user with zero historical clicks, views, applications, or outcomes immediately receives recommendations matching their profile.
- If no schemes match, an honest empty result with explanation is returned.

### 5. Personalization Divergence
- Distinct citizen profiles (e.g. Maharashtra Software Engineer vs. Maharashtra Farmer) legitimately produce distinct eligible sets and recommendation rankings aligned with occupational, economic, and geographic criteria.

### 6. Active Recommender & Fallback Integrity
- Active model: `2.2.0-hybrid-semantic-384d`.
- Fallback model: `1.0.0-deterministic`.
- Latency circuit breaker: 200 ms.
- If semantic vector retrieval times out (>200 ms) or encounters an error, execution gracefully drops back to the deterministic MADM utility ranker.

### 7. Target Leakage Protection
- Behavioral outcomes (`SCHEME_APPLIED`, `APPLICATION_COMPLETED`, conversion labels) are strictly excluded from ranking features. Ranking utilizes only canonical profile attributes and scheme criteria.

### 8. Telemetry Compatibility & Zero PII
- Phase 31–34 telemetry events (`RECOMMENDATION_SHOWN`, `SCHEME_VIEWED`, `SCHEME_EXPANDED`, `SCHEME_SAVED`, `APPLICATION_STARTED`, `SCHEME_APPLIED`, `APPLICATION_COMPLETED`) continue to function as an observation layer.
- Telemetry is never a blocking prerequisite for generating recommendations.
- Output payloads and logs contain zero forbidden PII (Aadhaar, PAN, phone, email, address).

### 9. Quarantined Historical Synthetic Fixtures
- All 29 historical synthetic application fixtures remain permanently quarantined in database isolation.

### 10. Database Mutation Safety
- Recommendation generation and validation are strictly read-only (`INSERT=0, UPDATE=0, DELETE=0, DROP=0`).

---

## 4. Future Training Architecture (Documented, Not Executed)

When genuine citizen interaction volume matures, model training will follow this auditable lifecycle:

```
    Genuine Citizen Interactions
              │
              ▼
    11-Gate Telemetry Ingestion Boundary
              │
              ▼
    7-Way Quality Classification
              │
              ▼
    Legitimate Outcome Attribution
              │
              ▼
    100+ Qualified Outcome Sessions Threshold
              │
              ▼
    Data Freeze & Target Leakage Validation
              │
              ▼
    Train Candidate Model (Offline Sandbox)
              │
              ▼
    Offline Metric & Governance Evaluation
              │
              ▼
    Explicit Human Authorization & Promotion
```

At Phase 35, the system remains at `0 / 100` legitimate outcome sessions. No training script or model promotion has been executed.
