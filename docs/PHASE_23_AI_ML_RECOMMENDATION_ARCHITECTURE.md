# Phase 23: Production AI/ML Scheme Recommendation Architecture

## 1. Executive Summary & Production Promotion

In Phase 23, SchemeBridge promoted its AI/ML-assisted recommendation engine from **Shadow Mode (`shadow-mode: false`)** to the **Primary Production Ranking Engine**. This promotion was executed only after strict offline evaluation on the held-out test split (`data/ml_models/test.jsonl`), confirming:
1. **0.00% Statutory Eligibility Violation Rate**: Ineligible schemes are strictly filtered prior to semantic scoring and assigned a score of `0.00`.
2. **Ranking Superiority**: Hybrid semantic ML achieved an **NDCG@10 of 0.5252** (a +4.87% improvement over the deterministic baseline of 0.5008) and **Precision@10 of 0.5625** (vs. 0.5375).
3. **Resilient Circuit-Breakers**: 100% automated fallback to deterministic scoring during timeouts (>200ms) or index unavailability.

---

## 2. Strict Architectural Invariant: Hard Statutory Eligibility Gate

The recommendation pipeline enforces a non-negotiable legal and technical hierarchy:
```
                                Citizen Profile
                                       │
                                       ▼
                     ┌───────────────────────────────────┐
                     │     Statutory Eligibility Gate     │
                     │      (EligibilityEngine.java)     │
                     └───────────────────────────────────┘
                                       │
               ┌───────────────────────┴───────────────────────┐
               │                                               │
               ▼                                               ▼
      [Ineligible Schemes]                             [Eligible Schemes]
               │                                               │
       Score = 0.00                                            ▼
   Strictly Discarded                          ┌───────────────────────────────────┐
   Never Recommended                           │   Hybrid Semantic ML Ranking      │
                                               │   (Cosine Similarity 384d Dense)  │
                                               └───────────────────────────────────┘
                                                               │
                                                               ▼
                                               ┌───────────────────────────────────┐
                                               │      Weighted Utility Fusion      │
                                               │  (Multi-Attribute Criteria Match) │
                                               └───────────────────────────────────┘
                                                               │
                                                               ▼
                                               ┌───────────────────────────────────┐
                                               │   Explainable Citizen Response    │
                                               │   (Zero-Jargon Transparency)      │
                                               └───────────────────────────────────┘
```

### Safety Rules Enforced in Production:
- The ML model is **strictly prohibited** from determining statutory eligibility.
- Every scheme must pass all mandatory AST conditions (`state`, `age`, `income`, `caste`, `occupation`, `disability`, `landholding`).
- Schemes failing any condition receive an eligibility score of `0.00` and are never processed by the semantic embedding layer.
- Evaluated Violation Rate: **0.00% across all 5,624 pairwise test evaluations**.

---

## 3. Embedding Vector Index & Ranking Pipeline

### 3.1 Model & Embedding Artifacts
- **Base Embedding Model**: `sentence-transformers/all-MiniLM-L6-v2` (384-dimensional dense vectors, normalized).
- **Index Store**: `data/ml_models/scheme_embeddings_384d.bin` (Raw Float32 little-endian binary vectors, 7,271,424 bytes for 4,734 schemes).
- **Metadata Index**: `data/ml_models/scheme_embeddings_metadata.json` mapping scheme codes, slugs, and row offsets.
- **Query Vectorizer**: `SemanticEmbeddingIndexService.java` generates query representations from citizen profiles (occupation, state, category, demographic factors) and computes exact cosine similarity against indexed scheme vectors in memory.

### 3.2 Scoring Formula
For an eligible scheme $s$ and citizen profile $c$:
$$\text{UtilityScore}(s, c) = w_{\text{state}} \cdot M_{\text{state}} + w_{\text{cat}} \cdot M_{\text{cat}} + w_{\text{occ}} \cdot M_{\text{occ}} + w_{\text{inc}} \cdot M_{\text{inc}} + w_{\text{prior}} \cdot P(s)$$
$$\text{RecommendationScore}(s, c) = \alpha \cdot \text{UtilityScore}(s, c) + (1 - \alpha) \cdot \text{CosineSimilarity}(\vec{e}_s, \vec{q}_c)$$
Where:
- $\alpha = 0.65$ (Utility weighting ensuring policy correctness)
- $(1 - \alpha) = 0.35$ (Semantic affinity weighting ensuring contextual relevance)
- $\text{Threshold} = 0.40$

---

## 4. Resilience & Circuit-Breaker Architecture

| Trigger Condition | Failure Mode | System Response |
| :--- | :--- | :--- |
| Semantic Index Missing | Missing `.bin` or `.json` file | Falls back immediately to deterministic rule utility scoring (`rankingMethod: DETERMINISTIC_FALLBACK`) |
| Vector Computation Timeout | Latency $> 200\text{ms}$ | Timeout intercepted via `CompletableFuture.orTimeout()`; returns deterministic ranking without failing request |
| Cold-Start Profile | New user with sparse attributes | State and demographic broad utility prioritization; zero crash, graceful degraded ranking |
| Ineligible Scheme Surfacing Attempt | Boundary logic error | Hard assertion filter strips non-eligible items before building API response |

---

## 5. API Unification & Explainability

### Unified Endpoints:
- `/api/schemes/recommendations`: Core personalized endpoint utilized by citizen dashboard and recommendation feed.
- Unified response model: `PersonalizedRecommendationResponse` and `RecommendationResponseItem`.
- **Explainability**: No technical ML jargon (e.g., "cosine similarity", "loss", "dense vector", "embedding") is exposed to citizens. Instead, human-readable explanations are provided:
  - *Matches your state (e.g., Gujarat)*
  - *Matches your age group (30 years)*
  - *You meet the income requirement*
  - *You belong to the eligible occupation (Farmer)*
  - *You satisfy the required statutory eligibility conditions*
