# PHASE 30 — PRE-TRAINING DATASET VALIDATION SPECIFICATION & INTEGRITY PROTOCOL

## 1. Scope & Objective

This document defines the strict pre-training validation requirements that any future supervised Learning-to-Rank (LTR) dataset must pass before it may be consumed by model training pipelines.

Pre-training validation acts as an automated security and compliance firewall, guaranteeing:
- Zero statutory eligibility contamination
- Zero PII leakage
- Zero synthetic fixture contamination
- Zero target/label leakage
- Zero session cross-split leakage
- Deterministic cryptographic reproducibility via SHA-256 integrity manifests

---

## 2. Validation Check Matrix

| Check ID | Dimension | Acceptance Criteria | Fail-Safe Behavior |
|---|---|---|---|
| **PV-01** | Statutory Eligibility | 100% of candidate pairs evaluated as `ELIGIBLE` by `EligibilityEngine.evaluate()`. Violation rate = 0.00%. | Dataset generation halts; affected pairs purged. |
| **PV-02** | Zero PII Compliance | Zero forbidden keys (`aadhaar`, `uid`, `pan`, `phone`, `mobile`, `email`, `name`, `firstname`, `lastname`, `address`, `street`, `ip`, `location`, `pincode`, `udid`, `lat`, `lon`) in root, metadata, payloads, or feature contexts. | Immediate exclusion; PII values never logged. |
| **PV-03** | Synthetic Quarantine | Zero synthetic pairs. All 29 historical application_events and test user patterns permanently quarantined. | Quarantined immediately. |
| **PV-04** | Target Leakage Detection | `UserSchemeFeatureVector` contains zero target fields (`relevanceGrade`, `label`, `target`) or post-ranking behavioral states (`applicationOutcome`, `clickOutcome`, `eventType`). | `IllegalStateException` thrown; dataset generation aborted. |
| **PV-05** | Session Split Isolation | Session-level SHA-256 assignment ($TRAIN = 70\%$, $VAL = 15\%$, $TEST = 15\%$). $TRAIN \cap VAL = \emptyset$, $TRAIN \cap TEST = \emptyset$, $VAL \cap TEST = \emptyset$. | Split verification fails if cross-session overlap > 0. |
| **PV-06** | Duplicate Sessions | Zero duplicate session assignments across dataset splits. | Enforced by deterministic hashing. |
| **PV-07** | Relevance Label Validity | Multi-level relevance grades strictly restricted to $\{0, 1, 2, 3\}$. | Invalid grades rejected. |
| **PV-08** | Feature Completeness | `UserSchemeFeatureVector` conforms to schema `1.0.0` with non-null user, scheme, and comparison features. | Incomplete feature vectors rejected; no silent fabrication. |

---

## 3. Cryptographic Integrity Manifest (`phase30_integrity_manifest.json`)

To prevent tampering and ensure reproducibility across training runs, each dataset artifact is hashed with SHA-256:

```json
{
  "datasetVersion": "phase30-pretraining-v1",
  "hashAlgorithm": "SHA-256",
  "generatedAt": "2026-09-05T15:41:32.315697",
  "artifacts": {
    "readinessReport": "8125c0c36a6e163939fc53656625d40a651c9c9007ddc56b99cc2a94ed49c171",
    "outcomeProgress": "07802e6128cafabeb103e16cb4c5aea204f580adcc0d209f1b46ff62e3df876d",
    "pretrainingValidation": "99428862a03a3c0f3fa801c8fcd72e6b07824c2d46f4d2e8ca41fc3e157772f4"
  },
  "reproducibleHash": "6469bc0cd6e3231ba4cfb6fee0726d7f90c8813d1c37e092db43bf5e65cdd321"
}
```

Any discrepancy between computed and stored hashes invalidates the pre-training dataset and halts subsequent pipeline steps.
