# PHASE 40 — OFFLINE BEHAVIORAL ML TRAINING PIPELINE WITH HARD PRODUCTION SAFETY GATE

## Executive Summary

Phase 40 establishes the complete offline behavioral ML training pipeline and multi-gate safety firewall for the SchemeBridge platform.

### Non-Negotiable System Baseline

Under the verified production baseline:
- **Historical Users**: `0`
- **Historical Behavioral Records**: `0`
- **Legitimate Outcome Sessions**: `0 / 100`
- **Remaining Outcomes Required**: `100`
- **Historical Synthetic Fixtures**: `29` (100% Quarantined)
- **Synthetic Events Generated**: `0`
- **Current-Citizen Recommendation Engine**: **`READY`**
- **Active Production Model**: `2.2.0-hybrid-semantic-384d`
- **Fallback Recommender**: `1.0.0-deterministic`
- **Circuit Breaker**: `200 ms`
- **Behavioral ML Training Execution**: **`NOT READY / NOT EXECUTED`**
- **Training Gate Status**: `TRAINING_BLOCKED_BELOW_THRESHOLD`
- **modelTrainingAllowed**: `false`
- **modelPromotionAllowed**: `false` (Permanent Firewall)
- **Production Database Mutations**: `0` (`INSERT=0, UPDATE=0, DELETE=0, DROP=0`)

The primary objective of Phase 40 is to construct the real offline training machinery, candidate artifact schemas, offline evaluation framework, and hard pre-training safety gates, while proving that under current database conditions the trainer **refuses to execute** and safely halts at the pre-training boundary.

---

## 1. Architectural Separation

SchemeBridge strictly isolates online citizen-facing recommendation operations from offline ML experimentation:

```
================================================================================
1. LIVE CITIZEN RECOMMENDATION (READY - OPERATIONAL)
================================================================================
Authenticated Current Citizen
          ↓
Citizen Profile Normalization
          ↓
EligibilityEngine.evaluate() [SOLE STATUTORY AUTHORITY]
          ↓
Remove NOT_ELIGIBLE & INSUFFICIENT_DATA
          ↓
Eligible Canonical Scheme Catalog (4,734 schemes, 4,682 verified)
          ↓
Active Hybrid Semantic Recommender (2.2.0-hybrid-semantic-384d)
[Fallback: 1.0.0-deterministic | Circuit Breaker: 200 ms]
          ↓
Top-K Personalized Recommendations
          ↓
Telemetry Ingestion (Session-Attributed Events)

================================================================================
2. OFFLINE BEHAVIORAL ML TRAINING PIPELINE (BLOCKED AT SAFETY GATE)
================================================================================
Real Citizen Telemetry
          ↓
BehavioralTrainingDatasetQualificationService (0 / 100 Legitimate Outcomes)
          ↓
DatasetFreezeService.freezeDataset()
          [BLOCKED_BELOW_THRESHOLD]
          ↓
Phase40TrainingGate (8-Gate Audit)
          [TRAINING_BLOCKED_BELOW_THRESHOLD]
          ↓
OfflineBehavioralModelTrainer: TRAINING NOT EXECUTED
          ↓
OfflineBehavioralModelEvaluator: INSUFFICIENT_DATA
          ↓
Promotion Firewall: modelPromotionAllowed = false (PERMANENT)
```

---

## 2. Hard Training Gate Specification (Phase40TrainingGate)

Training execution is governed by eight strict, non-bypassable pre-conditions:

| Gate | Requirement | Current Baseline | Status |
| :--- | :--- | :--- | :--- |
| **Gate A** | `legitimateOutcomeSessions >= 100` | `0 / 100` | **BLOCKED** |
| **Gate B** | `datasetFreezeSuccessful == true` | `BLOCKED_BELOW_THRESHOLD` | **BLOCKED** |
| **Gate C** | `piiViolations == 0` | `0` | **PASS** |
| **Gate D** | `targetLeakageViolations == 0` | `0` | **PASS** |
| **Gate E** | `syntheticContamination == 0` | `0` (Quarantined) | **PASS** |
| **Gate F** | `allTrainingExamplesValid == true` | `0` examples | **LOCKED** |
| **Gate G** | `chronologicalSequencesValid == true` | Enforced | **PASS** |
| **Gate H** | `recommendationAnchorsValid == true` | Enforced | **PASS** |

**Gate Decision**: `TRAINING_BLOCKED_BELOW_THRESHOLD`  
**modelTrainingAllowed**: `false`  
**modelPromotionAllowed**: `false`

Any attempt to force training execution under this state throws `IllegalStateException`.

---

## 3. Dataset Freeze Contract & Offline Trainer

### `DatasetFreezeSnapshot`
- Strictly immutable container wrapping dataset version, qualification timestamp, qualifying outcome count, source event IDs, pre-training schema versions, and SHA-256 cryptographic digest.
- If legitimate outcomes $< 100$, dataset version is deterministically `"NONE"`, hash is `"NONE"`, and status is `"BLOCKED_BELOW_THRESHOLD"`.

### `OfflineBehavioralModelTrainer`
- Accepts ONLY an immutable `DatasetFreezeSnapshot`.
- Verifies SHA-256 integrity hash, 8 safety gates, feature/label separation, and zero PII/leakage.
- Generates candidate artifacts designated with prefix `offline-candidate-<dataset-version>`.
- Preserves active model `2.2.0-hybrid-semantic-384d` and fallback `1.0.0-deterministic`.

---

## 4. Offline Candidate Model Artifact (`OfflineCandidateModelArtifact`)

An immutable domain record representing candidate models produced during offline training:
- `candidateModelVersion`: `"offline-candidate-<datasetVersion>"`
- `baselineModelVersion`: `"2.2.0-hybrid-semantic-384d"`
- `fallbackModelVersion`: `"1.0.0-deterministic"`
- `isProductionActive`: `false` (Strictly non-negotiable)
- `modelPromotionAllowed`: `false` (Strictly non-negotiable)
- Contains hyperparameters (`LambdaMART-LTR`, learning rate, num trees, max depth) and learned feature weights.

---

## 5. Offline Evaluation Framework (`OfflineBehavioralModelEvaluator`)

- Compares candidate models against production baseline `2.2.0-hybrid-semantic-384d`.
- Evaluates metrics: NDCG@5, MRR, Precision, Recall, F1.
- Evaluates compliance: PII compliance, target leakage compliance, statutory eligibility compliance, dataset integrity.
- If fewer than 100 legitimate outcome sessions exist, returns `INSUFFICIENT_DATA` without fabricating synthetic numbers.
- Invariant: `modelPromotionAllowed = false`. Model candidate is never automatically deployed or activated.

---

## 6. Permanent Promotion Firewall

Automatic model promotion is structurally impossible across the SchemeBridge platform:
1. `modelPromotionAllowed` is hardcoded to `false` in `Phase40TrainingGate`.
2. `modelPromotionAllowed` is hardcoded to `false` in `OfflineCandidateModelArtifact`.
3. `modelPromotionAllowed` is hardcoded to `false` in `OfflineBehavioralModelTrainer`.
4. `modelPromotionAllowed` is hardcoded to `false` in `OfflineBehavioralModelEvaluator`.
5. Production serving in `EligibleSchemeRecommendationService` only reads approved production configuration.
6. Promotion requires explicit, offline human governance review and deployment sign-off.
