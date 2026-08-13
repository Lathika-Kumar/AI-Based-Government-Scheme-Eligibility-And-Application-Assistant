# Phase 2B — Pre-Import Database Baseline Audit

## 1. Executive Summary
This pre-import baseline audit documents the state of the **Oracle XE database** prior to executing the Phase 2B real myScheme data migration.

- **Target Microservice**: `core-service` (Port 8082)
- **Target Database**: Oracle XE (`jdbc:oracle:thin:@localhost:1521/XEPDB1`, User: `system`)
- **Authoritative Source Dataset**: 4,679 verified detail JSON files in `D:\schemeBridge\myscheme-data\raw\details\`.

---

## 2. Pre-Import Oracle Table Record Counts

| Table Name | Pre-Import Count | Record Classification | Action / Strategy |
|---|---|---|---|
| **`SCHEMES`** | **7** | Initial Seed / Testing Data | Preserve intact. Real data uses `_id` identity. |
| **`SCHEME_CATEGORIES`** | **8** | Initial Seed Categories | Reuse existing categories by normalized code/name matching; create new categories dynamically. |
| **`SCHEME_DEPARTMENTS`** | **5** | Initial Seed Departments | Reuse existing departments; dynamically seed missing ministries/departments. |
| **`SCHEME_BENEFITS`** | **0** | Empty Table | Insert child records per imported scheme. |
| **`SCHEME_ELIGIBILITY_RULES`** | **16** | Initial Seed Rules | Preserve intact. Seed general text rules per scheme. |
| **`SCHEME_DOCUMENTS`** | **0** | Empty Table | Remain empty (documents rendered dynamically from `applicationProcess` text). |
| **`SCHEME_FAQS`** | **0** | Empty Table | Remain empty (0 FAQs in myScheme detail payload). |
| **`SCHEME_TAGS`** | **0** | Empty Table | Insert child rows per imported scheme from `tags` array. |

---

## 3. Analysis of Existing Baseline Records
The 7 pre-existing scheme records (`PM_JAY_2026`, `PM_SCHOLARSHIP_2026`, `PM_MATRU_VANDANA_2026`, `IGNOAPS_PENSION_2026`, `PM_MUDRA_2026`, etc.) were created by `SchemeDataSeeder.java` for initial microservice testing.

### Migration Safety Rules:
1. **Zero Table Truncation**: No tables will be dropped, truncated, or cleared.
2. **Identity Integrity**: Real myScheme records use `data._id` (20-character alphanumeric string) as primary key `ID` and `data.slug` for `SLUG` column, eliminating collision risks with pre-existing seed records.
3. **Category & Department Deduping**: Categories and departments are normalized and deduplicated before insert to prevent duplicate rows.

---

## 4. Required Schema Enhancements for `SCHEMES` Table

The following 6 new columns are added to the `SCHEMES` entity and Oracle DB table to support 100% real myScheme data fidelity:

1. `SLUG (VARCHAR2(200))` — URL slug
2. `DETAILED_DESCRIPTION (CLOB)` — Full markdown description (`detailedDescription_md`)
3. `ELIGIBILITY_TEXT (CLOB)` — Full eligibility markdown text (`eligibilityDescription_md`)
4. `APPLICATION_PROCESS (CLOB)` — Step-by-step application instructions
5. `DBT_SCHEME (NUMBER(1,0))` — DBT indicator boolean
6. `TARGET_BENEFICIARIES (VARCHAR2(1000))` — Beneficiary labels string

---

## 5. Audit Sign-Off
- **Status**: PRE-IMPORT AUDIT COMPLETE
- **Proceed Permission**: GRANTED FOR STEP 4 & 5 (Walkthrough Creation & Import Subpackage Implementation)
