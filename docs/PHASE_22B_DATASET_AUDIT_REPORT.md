# PHASE 22B: ML DATASET FORENSIC AUDIT REPORT

**Audit Execution Timestamp:** `2026-09-04T21:44:35.752598`  
**Dataset Location:** `E:/SCHEMEBRIDGE/data/ml_dataset`  
**Overall Compliance Status:** **PASS**  

---

## 1. Executive Summary & Dataset Inventory

| Dataset File | Record Count | Unique Schemes | File Size | Syntax Errors | Duplicates |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `training_dataset_master.jsonl` | **4,682** | 4682 | 35.66 MB | 0 | 0 |
| `ai_tasks_training_data.jsonl` | **18,728** | 4682 | 15.26 MB | 0 | 18724 |
| `official_document_checklists.json` | **736** | 736 | 5.84 MB | 0 | 0 |
| `eligibility_criteria_dataset.json` | **4,675** | 4675 | 15.85 MB | 0 | 0 |
| `scheme_to_document_relationships.json` | **436** | 259 | 0.07 MB | 0 | 0 |
| `raw_source_evidence.json` | **4,682** | 4682 | 4.32 MB | 0 | 0 |
| `human_review_queue.json` | **3,818** | 3818 | 1.38 MB | 0 | 0 |

---

## 2. PII Protection & Data Sanitization Audit

- **Citizen Names Found:** `None` (0.00%)
- **Phone Numbers Found:** `None` (0.00%)
- **Email Addresses Found:** `None` (0.00%)
- **Aadhaar Numbers Found:** `None` (0.00%)
- **Ration Card Identifiers Found:** `None` (0.00%)
- **Credentials / JWTs Found:** `None` (0.00%)

> [!NOTE]
> The dataset contains zero direct citizen PII. All text fields describe statutory government scheme requirements and institutional policies only.

---

## 3. Document Intelligence & ONE_OF Group Validation

- **Total Document Requirements:** 6,389
- **Mandatory Requirements:** 4,805
- **Optional Requirements:** 0
- **Conditional Requirements:** 0
- **Alternative Document Requirements:** 1,584
- **Structured ONE_OF Groups:** **1,584**
- **Total Options inside ONE_OF Groups:** 4,414
- **Invalid ONE_OF Groups (<2 choices):** **0**

### Document Types Distribution

| Document Type | Occurrences |
| :--- | :---: |
| `SCHEME_SPECIFIC_PROOF` | 3,276 |
| `IDENTITY_PROOF` | 1,148 |
| `FINANCIAL_PROOF` | 532 |
| `PHOTO_PROOF` | 347 |
| `AGE_OR_EDUCATION_PROOF` | 248 |
| `CATEGORY_PROOF` | 239 |
| `PROPERTY_OR_LEGAL_PROOF` | 208 |
| `ADDRESS_PROOF` | 189 |
| `INCOME_PROOF` | 180 |
| `SKILL_OR_TRAINING_PROOF` | 22 |

---

## 4. Multi-Task AI Sample Breakdown (`ai_tasks_training_data.jsonl`)

- **Total Multi-Task Samples:** **18,728**
- **Task Families:**
  - `TASK`: 18,728 samples

---

## 5. Audit Invariants & Integrity Certificate

1. **100% JSON/JSONL Syntax Compliance:** 0 syntax errors across all 7 files.
2. **Zero Duplicate Scheme Codes:** Master schemes catalog has 0 duplicate scheme codes.
3. **Zero Duplicate Slugs:** Master schemes catalog has 0 duplicate slugs.
4. **Zero Missing Identifiers:** All canonical master schemes possess valid alphanumeric `schemeCode` and URL `slug`.
5. **ONE_OF Semantics Intact:** All 1,584 alternative groups contain 2 or more distinct document options.
6. **Original Datasets Untouched:** Zero mutations performed on source data files.
