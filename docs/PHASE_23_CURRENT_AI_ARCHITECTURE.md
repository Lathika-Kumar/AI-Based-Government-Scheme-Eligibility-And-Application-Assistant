# PHASE 23 — CURRENT AI/ML ARCHITECTURE & AUDIT ASSESSMENT

**Document Reference:** `docs/PHASE_23_CURRENT_AI_ARCHITECTURE.md`  
**Date:** September 4, 2026  
**System:** SchemeBridge National Social Welfare Platform  
**Assessment Target:** Phase 22B Baseline & Phase 23 Production Promotion  

---

## 1. Executive Assessment & Baseline Architecture

SchemeBridge has completed Phase 22B, creating a production-grade machine learning foundation while preserving absolute statutory correctness through a **Deterministic Hard Eligibility Gate**.

```
                           ┌─────────────────────────────────┐
                           │      Citizen Profile Input      │
                           │ (Demographics & Socio-Economic) │
                           └────────────────┬────────────────┘
                                            │
                                            ▼
                           ┌─────────────────────────────────┐
                           │   HARD ELIGIBILITY GATE (AST)   │
                           │  - Deterministic AST Evaluator  │
                           │  - Statutory Criterion Check    │
                           │  - Ineligible -> Score = 0.0    │
                           └────────────────┬────────────────┘
                                            │
                             Eligible Candidates ONLY
                                            │
                                            ▼
                  ┌──────────────────────────────────────────────────┐
                  │       HYBRID RECOMMENDATION ENGINE (MADM)        │
                  │  - Demographic Match Utility (0.20)              │
                  │  - Geographic Match Utility (0.20)               │
                  │  - Occupation Match Utility (0.15)               │
                  │  - Benefit Match Utility (0.15)                  │
                  │  - Economic Match Utility (0.10)                 │
                  │  - Dense Semantic Vector Space Matching (0.20)   │
                  └─────────────────────────┬────────────────────────┘
                                            │
                                            ▼
                  ┌──────────────────────────────────────────────────┐
                  │      CIRCUIT BREAKER & SAFETY EVALUATION         │
                  │  - Shadow-Mode Dual Evaluation Logging           │
                  │  - Latency Guard (< 200 ms timeout)              │
                  │  - Deterministic Fallback if Failure Occurs      │
                  └─────────────────────────┬────────────────────────┘
                                            │
                                            ▼
                  ┌──────────────────────────────────────────────────┐
                  │          EXPLAINABLE RECOMMENDATIONS             │
                  │  - Human-Readable Matched Reasons                │
                  │  - Additive Audit Fields (rankingMethod, model)  │
                  └──────────────────────────────────────────────────┘
```

---

## 2. Existing Components Inventory

### 2.1 Existing Machine Learning & Embedding Components
1. **Semantic Scheme Vector Embeddings (`data/ml_models/scheme_embeddings.index`)**:
   - 4,734 schemes embedded as 384-dimensional dense single-precision IEEE 754 float32 vectors.
   - Vector representation based on `sentence-transformers/all-MiniLM-L6-v2`.
   - File size: 7,271,432 bytes.
   - Accompanied by checksums and code mappings in `scheme_embeddings_metadata.json` and `model_registry.json`.
2. **In-Memory Zero-Latency Vector Index (`SemanticEmbeddingIndexService.java`)**:
   - Loads binary matrix into memory once at Spring Boot startup (~105 ms).
   - Fast cosine similarity computation in pure Java: `< 0.05 ms` per candidate scheme.
   - Status check and availability guard (`isAvailable()`).
3. **Dataset Splits & Offline Evaluation Harness (`scripts/split_and_evaluate.py`)**:
   - 70% Train (13,108 records, 3,277 schemes), 15% Validation (2,808 records, 702 schemes), 15% Test (2,812 records, 703 schemes).
   - Scheme-level partition guaranteeing zero scheme leakage across partitions.
4. **Document Intelligence Extraction Pipeline (`scripts/document_intelligence_pipeline.py`)**:
   - Extracted 6,389 verified documents across 4,554 schemes from official gazettes and raw evidence datasets.
   - Preserved 1,584 disjunctive `ONE_OF` requirement groups with explicit group identifiers and 100% provenance citations.

### 2.2 Existing Deterministic Components
1. **EligibilityEngine (`com.schemebridge.scheme.service.EligibilityEngine`)**:
   - Recursive Abstract Syntax Tree (AST) evaluator parsing nested rule groups with boolean logic (`AND`, `OR`, `NOT`, `NONE`).
   - Evaluates leaf-level conditions across age, gender, occupation, annual income, caste, state, land ownership, and student status.
   - 100% deterministic, immutable, and statutory.
2. **EligibilityEvaluationService (`com.schemebridge.scheme.service.EligibilityEvaluationService`)**:
   - Orchestrates full-catalog statutory evaluation for a given citizen profile.
   - Segregates schemes into `ELIGIBLE`, `NOT_ELIGIBLE`, and `INDETERMINATE`.
3. **SchemeDocumentRequirementResolver (`com.schemebridge.scheme.service.SchemeDocumentRequirementResolver`)**:
   - Four-tier hierarchical resolution:
     1. Priority 1: Explicit `VERIFIED_OFFICIAL` canonical requirements (`scheme_verified_data`).
     2. Priority 2: Catalog required documents (`Scheme.requiredDocuments`).
     3. Priority 3: System-configured requirements derived from AST criteria.
     4. Priority 4: `DOCUMENT_REQUIREMENTS_NOT_MAPPED` fallback.
   - Strict `ONE_OF` alternative parsing preserving disjunctive options (e.g. Aadhaar OR Voter ID OR Passport).
4. **Multi-Attribute Decision Making (MADM) Utility Scorer**:
   - Multi-factor deterministic utility calculation weighing demographic, geographic, economic, benefit, and occupation relevance.

---

## 3. Data Flow and API Pipelines

### 3.1 Recommendation Flow
1. Citizen triggers recommendation request via `GET /api/recommendations` or `POST /api/schemes/recommendations`.
2. Citizen demographic and socio-economic attributes are mapped to `CitizenEligibilityProfile`.
3. **Hard Gate Execution**: `EligibilityEvaluationService` runs AST evaluation over the active scheme catalog.
4. If candidate list is empty, returns clean zero-result response without invoking ML.
5. For eligible candidates, `EligibleSchemeRecommendationService` calculates:
   - Deterministic feature scores (demographic, geographic, occupation, benefit, economic).
   - Semantic vector similarity score via `SemanticEmbeddingIndexService`.
   - Hybrid score: $Score = \sum (w_i \times s_i)$.
6. Circuit breaker monitors execution time. If execution exceeds 200 ms or the index is unavailable, it gracefully defaults to `DETERMINISTIC_FALLBACK`.
7. Explainable reasons are synthesized from verified matched features.
8. Recommendations are sorted descending by score and paginated.

### 3.2 Document Checklist Flow
1. Client requests document checklist for scheme (`/api/schemes/{id}/documents` or wizard step).
2. `SchemeDocumentRequirementResolver` inspects canonical `scheme_verified_data`.
3. If canonical record exists, canonical documents take absolute precedence (`VERIFIED_OFFICIAL`).
4. If missing, resolver inspects master catalog `requiredDocuments` or derives from AST rules.
5. `ONE_OF` alternative groups are packaged with group ID and options list so the frontend can render selection radio/dropdown rather than mandatory multi-upload.
6. Provenance metadata (source URL, authority, verified timestamp) is attached.

---

## 4. Current Limitations in Phase 22B

1. **Shadow Mode Operation**: In Phase 22B, the backend was configured with `recommendation.ml.shadow-mode: true`, running dual calculation and logging divergence, but primarily returning conservative ranking.
2. **Dual Endpoint Discrepancy**: 
   - `/api/recommendations` uses `EligibleSchemeRecommendationService` (with ML hybrid scoring and shadow mode).
   - `/api/schemes/recommendations` in `SchemeController` uses legacy `SchemeRecommendationService` (calculating leaf condition percentages without semantic embeddings).
   - Frontend `Recommendations.jsx` calls `/api/schemes/recommendations` via `schemeService.js`, missing the hybrid ML ranking and explainability fields.
3. **Admin AI Operational Query Gaps**:
   - While `AiChatService` handles general application workload and grievance stats, it lacks direct handlers for ML-specific admin queries (e.g. "Which schemes have incomplete document requirements?", "Show schemes where ML ranking differs from deterministic ranking", "Show recommendation model evaluation metrics").
4. **Citizen AI Scheme Deep Linking & Explanations**:
   - `AiChatService` citizen handler currently searches schemes by regex text matching. It does not integrate semantic similarity or explainable recommendation reasons for student/state/occupation queries.

---

## 5. Components to Reuse vs. Components Requiring Modification

### 5.1 Components to Reuse As-Is (Untouched)
- `EligibilityEngine.java`: Statutory AST evaluator is rock solid and must NEVER be modified.
- `EligibilityEvaluationService.java`: Full-catalog candidate filtering.
- `SchemeRepository.java`, `SchemeVerifiedDataRepository.java`: Database read interfaces.
- `scheme_embeddings.index` & `scheme_embeddings_metadata.json`: Precomputed 384-d binary vectors.
- `scripts/audit_ml_datasets.py`, `scripts/generate_scheme_embeddings.py`: Validated pipelines.
- Payment, Grievance, Feedback, and User Management services.

### 5.2 Components to Extend & Modify in Phase 23
1. **`EligibleSchemeRecommendationService.java`**:
   - Promote ML ranking from shadow mode to production primary mode once offline evaluation passes.
   - Enhance explainability generation with rich, specific bullet points.
2. **`SchemeRecommendationService.java` & `SchemeController.java`**:
   - Unify recommendation endpoints: ensure `/api/schemes/recommendations` utilizes the production hybrid ML engine so frontend `Recommendations.jsx` seamlessly receives the full power of Phase 23.
3. **`SchemeDocumentRequirementResolver.java`**:
   - Integrate with `data/ml_predictions/document_checklist_predictions.json` as an authoritative evidence cache for seed-only schemes while guaranteeing that canonical verified data always wins in conflicts.
   - Introduce explicit confidence states: `VERIFIED_OFFICIAL`, `VERIFIED_SOURCE`, `REQUIRES_REVIEW`, `UNRESOLVED`.
4. **`AiChatService.java`**:
   - Extend citizen AI to answer questions like "Why was this scheme recommended?", "What documents do I need?", "Can I apply with an income certificate instead?", "Show schemes suitable for students/my state".
   - Extend admin AI to answer operational ML queries: incomplete document requirements, ranking divergence, evaluation metrics, and human verification queue.
5. **Frontend `Recommendations.jsx`**:
   - Display "Why this scheme is recommended" badges and explainability bullet points, deadline badges, and document checklist readiness unobtrusively without exposing raw ML jargon.
6. **Evaluation & Reporting Artifacts**:
   - Implement `scripts/phase23_recommendation_evaluator.py` to generate Precision@K, Recall@K, NDCG@K, MRR, and verify 0% eligibility violations.
   - Generate all Phase 23 JSON and Markdown deliverables.
