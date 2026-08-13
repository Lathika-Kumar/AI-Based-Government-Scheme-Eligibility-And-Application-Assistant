# Phase 1 Extraction Walkthrough

## 1. Executive Summary
This document provides full mandatory technical documentation for **Phase 1 & Phase 1B: myScheme Real Data Extraction & Audit**.

All operations were performed strictly within `myscheme-data/`. No frontend code, microservices, or mock dataset files (`mockSchemes.js`) were modified.

---

## 2. What Was Extracted
1. **Listing Dataset**: Extracted 4,772 raw scheme search items from myScheme API `GET /search/v6/schemes`.
2. **Detail Dataset**: Extracted 4,679 full scheme detail JSON files from myScheme API `GET /schemes/v6/public/schemes?slug={slug}&lang=en`.
3. **Normalized Dataset**: Created 50 normalized scheme JSON samples under `myscheme-data/normalized/` mapping myScheme schema to SchemeBridge's Java `Scheme` entity attributes.

---

## 3. How Listing Pagination Works
- **Base Endpoint**: `https://api.myscheme.gov.in/search/v6/schemes`
- **Page Size**: `size=50` items per request.
- **Starting Offset**: `from=0, 50, 100, 150 ... 4750`.
- **Total Calculation**: `data.hits.page.total = 4772`.
- **Expected Pages**: `ceil(4772 / 50) = 96 pages`.
- **Files Saved**: 96 listing page files (`listing-page-000.json` through `listing-page-095.json`) and 1 combined file (`all-schemes-list.json`).

---

## 4. How Detail Extraction Works
- **Base Endpoint**: `https://api.myscheme.gov.in/schemes/v6/public/schemes?slug={slug}&lang=en`
- **Slug Resolution**: Extracted 4,679 unique scheme slugs from `all-schemes-list.json`.
- **Rate-Resilient Resume Strategy**:
  - `max_workers=2` (rate-controlled pool)
  - `delay_sec=0.5` inter-request delay
  - Exponential backoff on `HTTP 429` (`15s * (2^attempt)`)
  - Respects `Retry-After` response headers
  - Immediate file persistence to `raw/details/{slug}.json`
  - Immediate manifest update to `raw/details-manifest.json` after every single attempt
  - Checkpointed against filesystem to guarantee zero overwrites of existing successful files

---

## 5. Files Generated
```text
myscheme-data/
├── raw/
│   ├── listing-page-000.json ... listing-page-095.json (96 files)
│   ├── all-schemes-list.json
│   ├── missing-details-before-resume.json
│   ├── details/*.json (4,679 files)
│   └── details-manifest.json
├── normalized/
│   ├── aamgsiscs-normalized.json
│   ├── sui-normalized.json
│   └── sample-schemes-normalized.json
├── scripts/
│   ├── find_key.py
│   ├── extract_scheme_list.py
│   ├── extract_scheme_details.py
│   ├── build_missing_list.py
│   ├── resume_missing_details.py
│   ├── analyze_dataset.py
│   ├── normalize_samples.py
│   └── audit_existing_dataset.py
├── logs/
│   ├── extraction_listing.log
│   ├── extraction_details.log
│   └── extraction_resume.log
├── DATA_AUDIT.md
├── PHASE_1_EXTRACTION_AUDIT.md
├── PHASE_1_EXTRACTION_WALKTHROUGH.md
└── README.md
```

---

## 6. Validation Performed
- Checked for empty files (`0` empty files found).
- Checked for malformed JSON (`0` malformed files found).
- Checked for orphan detail files (`0` orphan files found).
- Verified slug mapping between listing and details (`4,679` exact matches).

---

## 7. Current Completion Percentage
- **Listing Extraction**: **100.0%** (4,772 / 4,772 records)
- **Detail Extraction**: **100.0%** (4,679 / 4,679 unique scheme slugs)

---

## 8. Exact Reason for Missing Details
- Initial rapid run using `max_workers=60` triggered myScheme API rate limits (`HTTP 429`).
- The rate-resilient script (`resume_missing_details.py`) successfully recovered all missing items under rate-controlled concurrency. All missing details are now **0**.

---

## 9. Commands Used for Verification
```powershell
python myscheme-data/scripts/audit_existing_dataset.py
```

---

## 10. Current Status

**PHASE 1 COMPLETE**

---

## 11. Next Step
Proceed to Phase 2 (importing the 4,679 verified real scheme detail records into Core Service Oracle DB).
