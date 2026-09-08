# PHASE 25: AI/ML CONTINUOUS LEARNING & RECOMMENDATION TRAINING AUDIT REPORT

**Audit Date**: 2026-09-04  
**Auditor**: SchemeBridge AI/ML Engineering & Safety Governance  
**Scope**: Full Codebase & Database Inspection of Existing AI/ML, Embedding, Recommendation, Event, and Feedback Pipelines  
**Invariant Status**: Statutory Eligibility Violation Target = 0.00% | Zero MongoDB Production Mutations  

---

## 1. Executive Summary

A comprehensive architectural and forensic audit of SchemeBridge was conducted to determine the feasibility and foundation for a feedback-driven AI/ML recommendation training pipeline. 

### Core Audit Finding
The current recommendation engine in SchemeBridge is a **Hybrid Deterministic Eligibility-Gated Semantic Retrieval and Multi-Attribute Heuristic Utility Ranker**. 
**There is currently NO supervised Learning-to-Rank (LTR) or gradient-trained machine learning model in production.**
Furthermore, **there is currently ZERO legitimate citizen recommendation feedback or interaction data stored in the database** (`recommendation_events: 0`, `portal_feedback: 0`, `applications: 0`).

In strict compliance with the Phase 25 Non-Fabrication Mandate:
> *"Do NOT fabricate datasets, metrics, model accuracy, user feedback, or production behavior. If there is NOT enough real feedback data, DO NOT fabricate it. Implement the infrastructure required to collect it and clearly report that actual model retraining must wait until sufficient real data exists."*

The training status for Phase 25 is formally designated as:
```
TRAINING_NOT_READY
```
This report establishes the complete factual baseline, details the existing components vs. missing components, documents the non-negotiable statutory eligibility safety architecture, and outlines the production-ready infrastructure to collect legitimate feedback and enable future safe offline model training and promotion.

---

## 2. Answers to Specific Audit Inquiries

### Q1: What is actually trained today?
- **Trained / Fitted Artifacts**:
  - An unsupervised vocabulary vectorizer (TF-IDF with n-grams 1-2, sublinear TF, 5,000 max features) and dimensionality reduction projector (TruncatedSVD, 384 components, randomized algorithm, seed=42) were fitted on the 4,734 master scheme texts in `scripts/generate_scheme_embeddings.py` (Phase 22B).
  - This produced the 384-dimensional dense scheme embedding matrix (`scheme_embeddings.index`) and the projection components (`query_vector_projector.json`).
- **NOT Trained**:
  - No supervised neural network, GBDT (XGBoost, LightGBM), or Learning-to-Rank model (RankNet, LambdaMART) has been trained on citizen behavioral labels (clicks, applications, approvals).
  - Explicit distinction: **SEMANTIC EMBEDDINGS ≠ SUPERVISED MODEL TRAINING ≠ ONLINE/CONTINUOUS LEARNING**. SchemeBridge currently has semantic embeddings and heuristic multi-criteria utility, but neither supervised model training nor online continuous learning.

### Q2: Which model/algorithm is currently responsible for semantic ranking?
- In Java runtime (`EligibleSchemeRecommendationService.java`):
  1. **Eligibility Filter**: `EligibilityEvaluationService` runs first. Only schemes with status `ELIGIBLE` pass the gate.
  2. **Candidate Feature Scoring**: Each candidate scheme is scored across 5 dimensions:
     - Occupational & Demographic Affinity ($f_{occ}$, weight = 0.25)
     - Economic Need & Welfare Priority ($f_{econ}$, weight = 0.25)
     - Geographic Specificity ($f_{geo}$, weight = 0.20)
     - Benefit Type & Impact ($f_{benefit}$, weight = 0.15)
     - Semantic Relevance / Embedding Similarity ($f_{semantic}$, weight = 0.15)
  3. **Semantic Similarity Implementation**: `SemanticEmbeddingIndexService.java` performs in-memory keyword affinity projection against the 384-dimensional pre-computed normalized vector representations from `data/ml_models/scheme_embeddings.index`.
  4. **Score Combination**: Weighted arithmetic sum normalized to $[0.0, 1.0]$.
  5. **Deterministic Ordering**: Sorted descending by `recommendationScore`, broken by `schemeCode` ascending.

### Q3: Which artifacts are static?
- `com/schemebridge/scheme/config/MlRecommenderProperties.java` (feature weights: occupation=0.25, economic=0.25, geographic=0.20, benefit=0.15, semantic=0.15; timeout=200ms).
- `com/schemebridge/scheme/service/EligibilityEngine.java` (statutory logic rules for age, gender, caste, income, state, land ownership, BPL).
- Authoritative document checklists in `scheme_verified_data` collection (e.g. `SO2YT5YLM` with 9 canonical documents).

### Q4: Which artifacts are generated from data?
- `data/ml_models/scheme_embeddings.index` (binary matrix: 4,734 schemes × 384 float32 dimensions = 7,271,432 bytes).
- `data/ml_models/query_vector_projector.json` (43,669,319 bytes: vocabulary terms, IDF weights, SVD component projection matrix).
- `data/ml_models/scheme_embeddings_metadata.json` (1,519,401 bytes: scheme-to-vector index mapping).
- `data/ml_models/model_registry.json` (model version `2.2.0-hybrid-semantic-384d`).
- `data/ml_models/split_manifest.json`, `train.jsonl`, `validation.jsonl`, `test.jsonl` (scheme-isolated splits from Phase 22B).
- `data/phase23_output/recommendation_evaluation.json` and `model_metadata.json`.

### Q5: Is there currently any real recommendation feedback dataset?
- **NO.**
- Forensic database query on `schemebridge_scheme_db`:
  - `recommendation_events` count = **0**
  - `portal_feedback` count = **0**
  - `applications` count = **0**
  - `application_events` count = **29** (all synthetic test fixtures from prior integration tests, all tied to dummy applicationId `6a952810c6037907f0030c06`)
  - `application_reviews` count = **1** (test fixture)
  - `citizen_profiles` count = **14**

### Q6: Are recommendation impressions/clicks/applications/outcomes stored anywhere?
- **Schema & Endpoints Exist**:
  - `RecommendationEvent` document entity exists in backend with fields: `id`, `userId`, `schemeCode`, `eventType`, `recommendationContext` (modelVersion, rank, score, eligibilityStatus), `timestamp`, `sessionId`, `metadata`.
  - `RecommendationEventType` enum exists: `SCHEME_VIEWED`, `SCHEME_EXPANDED`, `SCHEME_SAVED`, `SCHEME_APPLIED`, `APPLICATION_COMPLETED`.
  - `RecommendationEventService` and `RecommendationEventController` exist (`POST /api/recommendations/events`, `GET /api/recommendations/events/metrics`).
  - Frontend `schemeService.js` has `trackRecommendationEvent(eventData)`.
- **Actual Persistence in Production**:
  - The database table is empty. The frontend user interface components (`Recommendations.jsx`, scheme cards) were not dispatching calls to `trackRecommendationEvent` during citizen browsing.

### Q7: Can historical recommendation outcomes be safely converted into training examples?
- Not at this time, because there are 0 real user interaction records and 0 real application records.
- Any attempt to invent historical clicks, application conversions, or feedback ratings would violate the Non-Fabrication Mandate.

### Q8: Is there enough legitimate data to train a supervised ranking model?
- **NO.** Supervised learning-to-rank requires hundreds or thousands of query-document pairs with verified engagement or outcome labels across diverse citizen segments. With 0 interaction records, no legitimate supervised training is possible.

### Q9: Action Required for Insufficient Data
- As explicitly instructed:
  1. DO NOT fabricate datasets, synthetic interactions, or mock weights.
  2. Implement the full data generation and offline evaluation pipeline in code (`scripts/build_phase25_training_dataset.py`, `scripts/evaluate_phase25_recommendations.py`).
  3. Ensure the pipeline inspects live collections and, when insufficient data is detected, exits gracefully with `TRAINING_NOT_READY` and outputs detailed metadata regarding data deficiency.
  4. Ensure end-to-end event tracking is wired in the frontend and backend so future real citizen interactions automatically populate `recommendation_events`.

### Q10: System Taxonomy Classification
- The SchemeBridge recommendation system is:
  - **Type**: **Hybrid Eligibility-Gated Multi-Attribute Utility (MADM) + Unsupervised Semantic Vector Space Model**.
  - **Not**: Supervised Learning-to-Rank (LTR), Collaborative Filtering, or Online Reinforcement Learning.

---

## 3. Forensic Database Baseline Audit

Verified via direct read-only query against local production MongoDB (`schemebridge_scheme_db`):

| Metric | Target Value | Baseline Value | Status |
|---|---|---|---|
| `schemes` count | 4734 | 4734 | MATCH |
| `scheme_verified_data` count | 4682 | 4682 | MATCH |
| Difference (Seed-Only Schemes) | 52 | 52 | MATCH |
| Duplicate `schemeCode` | 0 | 0 | MATCH |
| Duplicate `slug` | 0 | 0 | MATCH |
| Embedded `benefits` | 35612 | 35612 | MATCH |
| Embedded `tags` | 22756 | 22756 | MATCH |
| `SO2YT5YLM` canonical documents | 9 | 9 | MATCH |
| `recommendation_events` count | 0 | 0 | MATCH |
| `portal_feedback` count | 0 | 0 | MATCH |
| `applications` count | 0 | 0 | MATCH |
| Production Mutations (Insert/Update/Delete/Drop) | 0 | 0 | ZERO MUTATIONS |

---

## 4. Non-Negotiable Safety Architecture Audit

### Statutory Eligibility Gate
```
Citizen Profile Snapshot
        ↓
EligibilityEngine.evaluate(profile, scheme)
        ↓
Filter: status == ELIGIBLE
        ↓
Candidate Schemes Entering Ranking
        ↓
ML / Semantic Ranking Engine
        ↓
Top-K Ranked Recommendations
```
- **Safety Invariant**: `STATUTORY_ELIGIBILITY_VIOLATION_RATE = 0.00%`.
- **Code Audit**: In `EligibleSchemeRecommendationService.java`, line 83-87:
  ```java
  CitizenEligibilityEvaluationResponse evalResponse =
      eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes(userId);
  List<EligibilityEvaluationResult> eligibleCandidates = evalResponse.getEligibleSchemes();
  ```
  Only `eligibleCandidates` are passed to `computeRecommendationScore`. Ineligible schemes are strictly discarded prior to ranking.
- **Circuit Breaker**: 200 ms timeout budget enforced. If exceeded or if ML fails, the service falls back gracefully to `DETERMINISTIC_FALLBACK` without dropping the eligibility gate.

### Document Intelligence Safety
- Canonical verified documents from `scheme_verified_data` strictly supersede any heuristic prediction.
- Hallucination rate remains 0.00%.

---

## 5. Architectural Recommendations for Phase 25

1. **Wire Real-Time Interaction Telemetry**:
   - Update frontend scheme views/clicks to call `trackRecommendationEvent` with genuine session and rank context.
   - Expand `RecommendationEventType` if needed (e.g. `RECOMMENDATION_SHOWN`, `SCHEME_CLICKED`, `APPLICATION_STARTED`).

2. **Build Dataset Pipeline (`scripts/build_phase25_training_dataset.py`)**:
   - Query legitimate collections: `recommendation_events`, `applications`, `application_events`, `portal_feedback`.
   - Implement minimum data threshold validation (e.g. minimum 100 interaction sessions with diverse outcomes).
   - If below threshold, cleanly output `TRAINING_NOT_READY` with a detailed schema requirement report.

3. **Build Offline Evaluation Engine (`scripts/evaluate_phase25_recommendations.py`)**:
   - Compare Model A (Deterministic Baseline) vs. Model B (Current Hybrid Semantic) vs. Model C (Candidate Learned Model, when available).
   - Validate Precision@5, Precision@10, Recall@5, NDCG@5, NDCG@10, MRR, and Statutory Eligibility Violation Rate (must be 0.00%).

4. **Model Registry & Promotion Safety**:
   - Implement version states: `CANDIDATE`, `SHADOW`, `APPROVED`, `ACTIVE`, `REJECTED`, `ROLLED_BACK`.
   - Enforce mandatory promotion gate: 0.00% eligibility violations, NDCG improvement, cold-start acceptability, latency <= 200ms.

5. **Explanations & Cold-Start**:
   - Preserve human-readable plain language explanations ("Matches your occupation", "Available in your state").
   - Maintain seamless cold-start handling for citizens with zero prior history.
