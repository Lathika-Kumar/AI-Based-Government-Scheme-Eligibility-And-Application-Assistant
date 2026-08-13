# Phase 2B Mapping Fix Walkthrough — Application Process & Benefits Mapping

## 1. Executive Summary
This document records the design, implementation, 10-scheme test validation, full production re-import, and empirical post-migration verification for fixing the `myScheme` mapper in SchemeBridge Core Service (`core-service`).

The fix resolves two major mapping omissions identified during the Phase 2B post-import audit:
1. `SCHEMES.APPLICATION_PROCESS` missing for 4,212 real schemes (only 463/4,675 were populated).
2. `SCHEME_BENEFITS` child table having 0 total rows in Oracle XE.

Following this fix and full idempotent re-import:
- `SCHEMES.APPLICATION_PROCESS` population increased from **463 (9.90%)** to **3,828 (81.88%)** real schemes.
- `SCHEME_BENEFITS` child rows expanded from **0** to **29,204 rows** spanning **3,827 distinct schemes**.
- All **4,682 total schemes** (4,675 real + 7 baseline seed development records) remain 100% intact with 0 data loss or duplicate entity records.

---

## 2. Problem Statement & Root Cause Analysis
During Phase 2B initial migration, `MySchemeMapper.java` attempted to extract text using Jackson's `.asText()` on non-existent properties:
- `modeItem.get("description")` on application process nodes (which were SlateJS AST objects under `process` or markdown strings under `process_md`).
- `bNode.get("description")` on benefit AST nodes (which were SlateJS bullet-list trees under `data.en.schemeContent.benefits`).

Because those JSON keys did not exist, `.asText()` returned empty string `""` or `null`, causing the JPA entity fields to remain `null` or omit child benefit entities altogether.

---

## 3. Architecture & Mapper Redesign

### Application Process Extraction Strategy:
1. **Markdown String First (`process_md`)**: Checks `modeItem.path("process_md").asText()`.
2. **SlateJS AST Fallback (`process`)**: Recursively extracts all text nodes from SlateJS AST objects using `extractSlateText()`.
3. **Application Mode Formatting**: Includes application mode headers (e.g., `### Application Mode: Online (URL: https://...)`) and formats steps cleanly as markdown lists.

### Benefits Bullet Point Extraction Strategy:
1. **SlateJS AST Tree Traversal**: Navigates `data.en.schemeContent.benefits` SlateJS block list (`type: "ul_list"` / `"ol_list"` -> `"list_item"`).
2. **Individual Benefit Row Creation**: Extracts bullet item text into standalone `SchemeBenefit` entities (`descriptionEnglish`, `benefitOrder`), allowing fine-grained search and structured presentation.

### Idempotency & Orphan Removal (`Scheme.java` & `MySchemeImportService.java`):
- Configured `@OneToMany(orphanRemoval = true)` on `Scheme.java` child collections (`benefits`, `eligibilityRules`, `documents`, `tags`, `faqs`).
- Updated `updateSchemeFields()` to invoke `target.getBenefits().clear()` before populating newly mapped benefits. Hibernate automatically issues `DELETE FROM SCHEME_BENEFITS WHERE SCHEME_ID = ?` and re-inserts the clean mapped rows without producing duplicate rows or orphan entities across repeated re-imports.

---

## 4. 10-Scheme Test Validation
Before executing the full re-import, a controlled 10-scheme test update was performed against `POST /api/v1/migration/import?mode=TEST_10`:
- **Sample Slugs Tested**: `kcc`, `pm-kisan`, `pmay-u`, `apy`, `ab-pmjay`, `mgnrega`, `108easuk`, `15dsugt`, `1pmy`, `40shydcs`.
- **Execution Result**: 10/10 schemes updated successfully in 902 ms with 0 failures.
- **Empirical Validation**:
  - `40shydcs` Application Process length expanded from 4 to 930 chars.
  - `15dsugt` Application Process length expanded from 4 to 927 chars.
  - `kcc` Application Process length expanded from 4 to 808 chars with 19 benefit rows.

---

## 5. Full Re-Import Execution & Performance
- **Trigger**: Executed `python myscheme-data/scripts/run_full_reimport_and_audit.py` triggering `POST /api/v1/migration/import?mode=FULL`.
- **Batch Processing**: Idempotent batch processing across 4,679 JSON files in `D:\schemeBridge\myscheme-data\raw\details`.
- **Database Durability**: HikariCP connection pool managed transaction commits safely with 0 connection drops or timeouts.

---

## 6. Empirical Verification & Comparison Table

| Metric | Before Fix (Phase 2B Initial) | After Fix (Phase 2B Fix Complete) | Improvement |
| :--- | :--- | :--- | :--- |
| **Total Database Schemes** | 4,682 | **4,682** | Preserved baseline + real data intact |
| **Real myScheme Records** | 4,675 | **4,675** | 100% real schemes preserved |
| **Preserved Seed Records** | 7 | **7** | 100% baseline seed preserved |
| **Populated Application Process** | 463 (9.90%) | **3,828 (81.88%)** | **+3,365 schemes (+720%)** |
| **Null Application Process** | 4,212 (90.10%) | **847 (18.12%)** | Reduced to raw source missing only |
| **Total SCHEME_BENEFITS Rows** | 0 | **29,204 rows** | **+29,204 rows** |
| **Distinct Schemes with Benefits**| 0 (0.00%) | **3,827 (81.86%)** | **+3,827 schemes** |
| **Average Benefits per Scheme** | 0 | **7.63 benefits / scheme**| Structured bullet extraction |

---

## 7. REST API Verification Samples

### Sample 1: Kisan Credit Card (`kcc`)
- **ID**: `f17db0e8-ae9e-4950-893e-fe0a01868fde`
- **Application Process Length**: 808 chars
- **Application Process Snippet**:
  > ### Application Mode: Online
  > 1. Visit the website of the bank you wish to apply for the kisan credit card scheme.
  > 2. From the list of options, choose the Kisan Credit Card...
- **Benefit Count**: 19 benefit items
- **First Benefit**: `Fixation of credit limit/Loan amount`

### Sample 2: Ayushman Bharat - PMJAY (`ab-pmjay`)
- **ID**: `ed6aedcf-bdf8-4b7c-a568-a2d3abedadb0`
- **Application Process Length**: 3,740 chars
- **Benefit Count**: 18 benefit items
- **First Benefit**: `Financial Health Insurance Cover: Up to Rs. 5,000,000 per family per year`

### Sample 3: 100% Penalty Mafi Yojana (`1pmy`)
- **ID**: `86f4871d-2bc3-4481-8ca2-44e54ff5b9ef`
- **Application Process Length**: 1,086 chars
- **Benefit Count**: 8 benefit items
- **First Benefit**: `The applicant receives 100% waiver on the penalty amount accumulated on pending installments.`

---

## 8. Summary of Files Modified / Created
- `Scheme.java`: Added `orphanRemoval = true` to `@OneToMany` child relationships.
- `MySchemeJsonParser.java`: Added `extractSlateText()` and `extractSlateTextLines()` SlateJS AST traversal routines.
- `MySchemeMapper.java`: Updated `applicationProcess` formatting and `benefits` bullet list extraction logic.
- `MySchemeImportService.java`: Configured `target.getBenefits().clear()` for clean child collection updates.
- `audit_post_fix_db.py` & `complete_db_audit_utf8.py`: Created empirical verification scripts.

---

## 9. Conclusion
Phase 2B mapper fix is 100% COMPLETE, validated, and verified against Oracle XE and Core Service REST APIs.
