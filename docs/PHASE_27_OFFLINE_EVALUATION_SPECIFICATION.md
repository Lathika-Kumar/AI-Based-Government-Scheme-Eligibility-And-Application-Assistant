# PHASE 27 — OFFLINE EVALUATION SPECIFICATION & BENCHMARK HARNESS

## 1. Overview & Policy

This specification outlines the evaluation protocol for the SchemeBridge recommendation ranking pipeline.

### Cold-Start Policy (Zero Legitimate Outcome Telemetry)
In strict accordance with the project integrity constraints:
> **"Offline evaluation must not claim model performance when there are zero legitimate outcome sessions. The benchmark should explicitly report INSUFFICIENT_DATA rather than generating artificial metrics."**

Because production MongoDB currently contains 0 legitimate outcome sessions (the 29 historical `application_events` are synthetic automated test fixtures and quarantined), the benchmark harness outputs:
```json
{
  "evaluationStatus": "INSUFFICIENT_DATA",
  "reason": "Zero legitimate citizen outcome sessions exist in production telemetry. Offline benchmark metrics (NDCG@K, MAP, MRR) are withheld rather than artificially manufactured.",
  "legitimateOutcomeSessions": 0,
  "metrics": {
    "ndcgAt5": null,
    "mapAt10": null,
    "mrr": null
  },
  "modelEnvironment": {
    "activeModel": "2.2.0-hybrid-semantic-384d",
    "fallbackModel": "1.0.0-deterministic",
    "circuitBreakerMs": 200,
    "modelPromotionOccurred": false
  }
}
```

---

## 2. Future Evaluation Protocol (Once >= 100 Legitimate Outcomes Exist)

When real citizen usage yields $\ge 100$ legitimate outcome sessions, the offline evaluation protocol will compute the following standard Learning-to-Rank metrics on the held-out `TEST` split (15%):

### 2.1 Normalized Discounted Cumulative Gain (NDCG@K)
$$\text{DCG}@K = \sum_{i=1}^K \frac{2^{y_i} - 1}{\log_2(i + 1)}, \quad \text{NDCG}@K = \frac{\text{DCG}@K}{\text{IDCG}@K}$$
Where $y_i \in \{0, 1, 2, 3\}$ is the multi-level relevance grade resolved via terminal-state attribution.

### 2.2 Mean Average Precision (MAP@10)
$$\text{MAP}@K = \frac{1}{|Q|} \sum_{q=1}^{|Q|} \text{AP}_q@K$$
Where relevance is binary-thresholded ($y_i \ge 2$, i.e. `INTENT_HIGH` or `CONVERTED`).

### 2.3 Mean Reciprocal Rank (MRR)
$$\text{MRR} = \frac{1}{|Q|} \sum_{q=1}^{|Q|} \frac{1}{\text{rank}_q^*}$$
Where $\text{rank}_q^*$ is the position of the first scheme with $y_i \ge 2$.

---

## 3. Mandatory Safeguards & Invariants

1. **Eligibility Filter Precedence**: Under no circumstances will NDCG or MAP be computed over schemes that fail statutory eligibility. All candidates in the ranking pool must satisfy `EligibilityEngine.evaluate() == ELIGIBLE`.
2. **Session-Level Isolation**: No session in `TEST` may have appeared in `TRAIN` or `VALIDATION`.
3. **No Premature Promotion**: A candidate ML model may only be promoted over `2.2.0-hybrid-semantic-384d` if:
   - $\text{NDCG}@5 \ge 0.75$ on genuine citizen test telemetry
   - Latency $P_{99} < 150 \text{ ms}$
   - Fallback circuit breaker rate $< 0.1\%$
   - Statutory eligibility violation rate strictly $= 0.00\%$.
