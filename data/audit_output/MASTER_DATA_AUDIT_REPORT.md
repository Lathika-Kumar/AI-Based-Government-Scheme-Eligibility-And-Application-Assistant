# SCHEMEBRIDGE — MASTER SCHEME DATA AUDIT REPORT

**Date:** 2026-09-03T04:54:05.357Z  
**Scope:** Full Catalog Audit across all **4682** Schemes  
**Primary Source Authority:** Government of India Official Portal (https://www.myscheme.gov.in/)

---

## 1. Executive Summary & Core Metrics

| Audit Metric | Count | Percentage |
| :--- | :--- | :--- |
| **Total Schemes Audited** | **4682** | 100.0% |
| **Schemes with High Data Completeness (>=85%)** | **103** | 2.2% |
| **Schemes with Partial Data Completeness (<85%)** | **4579** | 97.8% |
| **Schemes with Verified Official Documents** | **736** | 15.7% |
| **Schemes with Document Requirements Not Confirmed** | **3946** | 84.3% |
| **Schemes with Official Application URLs** | **6** | 0.1% |
| **Total Fields Audited** | **98322** | 100.0% |
| **Total Available Field Values** | **74354.5** | 75.6% |
| **Total Missing / Gap Field Values** | **21955** | 22.3% |
| **Identified Data Conflicts** | **0** | Preserved with non-destructive rules |
| **Overall Catalog Quality Score** | **75.6%** | High Source-Grounded Integrity |

---

## 2. Scheme Data Inventory

| Field | Existing | Missing | Partial | Source | Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `schemeCode` | 4682 | 0 | 0 | SchemeBridge/Oracle | **100.0%** |
| `slug` | 4675 | 7 | 0 | SchemeBridge/Oracle | **99.9%** |
| `title_english` | 4682 | 0 | 0 | SchemeBridge/Oracle | **100.0%** |
| `title_tamil` | 7 | 4675 | 0 | SchemeBridge/Oracle | **0.1%** |
| `shortDescription` | 4682 | 0 | 0 | Oracle/myScheme | **100.0%** |
| `detailedDescription` | 4682 | 0 | 0 | Oracle/myScheme | **100.0%** |
| `schemeLevel` | 4682 | 0 | 0 | Oracle/myScheme | **100.0%** |
| `stateOrUT` | 4682 | 0 | 0 | Oracle/myScheme | **100.0%** |
| `ministry` | 657 | 0 | 4025 | Oracle/myScheme | **14.0%** |
| `department` | 4682 | 0 | 0 | Oracle/myScheme | **100.0%** |
| `schemeCategory` | 4682 | 0 | 0 | Oracle/myScheme | **100.0%** |
| `beneficiaryType` | 4682 | 0 | 0 | Oracle/myScheme | **100.0%** |
| `officialMySchemeUrl` | 864 | 3818 | 0 | myScheme | **18.5%** |
| `eligibility_rawText` | 4675 | 7 | 0 | Oracle/myScheme | **99.9%** |
| `eligibility_structuredCriteria` | 4546 | 136 | 0 | myScheme/AST | **97.1%** |
| `requiredDocuments` | 736 | 3946 | 0 | myScheme | **15.7%** |
| `benefits` | 4674 | 8 | 0 | Oracle/myScheme | **99.8%** |
| `applicationMode` | 4682 | 0 | 0 | Oracle/myScheme | **100.0%** |
| `officialApplicationUrl` | 6 | 4676 | 0 | Oracle/myScheme | **0.1%** |
| `applicationProcedure` | 4675 | 7 | 0 | Oracle/myScheme | **99.9%** |
| `helpline` | 7 | 4675 | 0 | Oracle/myScheme | **0.1%** |

---

## 3. Mandatory Validation Case Check: `SO2YT5YLM`

- **Scheme Code:** `SO2YT5YLM`
- **Title:** *Scheme for the Welfare of Schedule Caste Families in Fisheries Sector: Subsidy on 2nd Year to 5th Year Lease Money*
- **Official URL:** https://www.myscheme.gov.in/schemes/so2yt5ylm
- **Completeness Status:** **100% (HIGH_COMPLETENESS)**
- **Document Checklist:** **9 Verified Official Documents Captured**
  1. `Agreement 1` (`AGREEMENT_DEED`, Mandatory)
  2. `Agreement 2` (`AGREEMENT_DEED`, Mandatory)
  3. `Date of Birth Certificate` (`BIRTH_CERTIFICATE`, **ALTERNATIVE: ONE_OF [Birth Certificate, Matriculation, PAN, Voter Card, Driving License]**)
  4. `Identity Proof` (`IDENTITY_PROOF`, **ALTERNATIVE: ONE_OF [Ration Card, Aadhar Card, PAN Card, Voter Card]**)
  5. `Caste Certificate` (`CASTE_CERTIFICATE`, Mandatory, Issued by: 1st Class Magistrate)
  6. `Training Certificate` (`TRAINING_CERTIFICATE`, Mandatory, Issued by: Govt. Institute)
  7. `Lease deed` (`LEASE_DEED`, Mandatory)
  8. `Receipt of Fish Seed` (`PAYMENT_OR_PURCHASE_RECEIPT`, Mandatory)
  9. `Photographs of the Pond Site` (`PHOTOGRAPHS`, Mandatory)

---

## 4. Final Safety & Pre-Migration Assessment (MongoDB Invariants)

> [!IMPORTANT]
> **STOP CONDITION VERIFIED:** No production MongoDB records were altered during this audit.
> All audit findings, normalized checklists, and data gap matrices have been saved to standalone additive artifact files.

### Invariant Status:
1. **Total Schemes in Catalog:** 4,682 genuine records (**UNTOUCHED**)
2. **Total Embedded Benefits:** 35,560 records (**UNTOUCHED**)
3. **Total Embedded Tags:** 22,500 records (**UNTOUCHED**)
4. **Duplicate Scheme Codes:** 0 (100% Unique)
5. **Duplicate Slugs:** 0 (100% Unique)
