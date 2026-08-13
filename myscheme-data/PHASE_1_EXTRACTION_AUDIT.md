# Phase 1 Extraction Audit

## 1. Listing Extraction
- **API Total**: 4,772
- **Pages Expected**: 96 (ceil(4772 / 50))
- **Pages Present**: 96 (`listing-page-000.json` through `listing-page-095.json`)
- **Records Extracted**: 4,772
- **Unique Records**: 4,680 unique IDs (4,679 unique slugs)

---

## 2. Detail Extraction
- **Details Expected**: 4,679
- **Details Present**: 4,679
- **Matching Details**: 4,679
- **Missing Details**: 0
- **Percentage Completed**: 100.0%

---

## 3. Missing Detail Analysis
Initial extraction encountered `HTTP 429 Too Many Requests` due to high concurrency (`max_workers=60`). 

Under the Phase 1B rate-resilient resume protocol (`resume_missing_details.py` with controlled concurrency `max_workers=2`, inter-request delay `delay_sec=0.5`, exponential backoff, and filesystem checkpointing), **all 3,315 previously missing scheme details** were successfully recovered and verified on disk.

---

## 4. Extraction Errors
Counts grouped by HTTP status / error type:

| HTTP Status / Error Type | Count | Description |
|---|---|---|
| **HTTP 200 OK** | **4,679** | Successfully downloaded & saved to `raw/details/*.json` |
| **HTTP Errors / Failures** | **0** | All scheme details extracted with 100% success rate |

---

## 5. Data Integrity
- **Duplicate Listing IDs**: 92 (due to pagination overlaps across search result pages)
- **Malformed JSON Files**: 0
- **Empty Files**: 0
- **Invalid Records**: 0
- **Orphan Detail Files**: 0 (all 4,679 detail files map 100% to valid listing slugs)

---

## 6. Current Dataset Readiness
**READY (100% COMPLETE)**

- **Listing Dataset**: **100% COMPLETE** (4,772 records across 96 pages).
- **Detail Dataset**: **100% COMPLETE** (4,679 verified scheme detail JSON files downloaded).
- **Zero missing details**, **zero orphan files**, **zero empty or malformed files**.

---

## 7. Recommended Next Action
1. Proceed to Phase 2: Import the complete real myScheme dataset (`4,679` schemes) into Core Service Oracle DB and connect the frontend REST service layers.
