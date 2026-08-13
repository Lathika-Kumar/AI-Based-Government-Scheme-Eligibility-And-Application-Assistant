# Phase 2 — Real myScheme Data Mapping Specification

## 1. Executive Summary
This document establishes the official field-by-field mapping between the **myScheme raw JSON schema** and **SchemeBridge Java Core Service Entity / Oracle Database Schema**.

- **Source Dataset**: 4,679 verified raw scheme detail JSON files in `myscheme-data/raw/details/`.
- **Target Architecture**: SchemeBridge `core-service` Spring Boot microservice & Oracle DB (`SCHEMES`, `SCHEME_CATEGORIES`, `SCHEME_DEPARTMENTS`, `SCHEME_BENEFITS`, `SCHEME_ELIGIBILITY_RULES`, `SCHEME_DOCUMENTS`, `SCHEME_TAGS`, `SCHEME_FAQS`).
- **Scope**: Data modeling and transformation definition ONLY. No database mutation or application code modification is performed in Phase 2A.

---

## 2. Field-by-Field Mapping Specification

### 2.1 Core Identity & Basic Metadata

#### 1. myScheme Scheme Identifier (`_id`)
```text
data._id
      ↓
Scheme.id
      ↓
SCHEMES.ID (VARCHAR2(36))
      ↓
DIRECT
```
- **Transformation**: Direct string assignment (`VARCHAR2(36)` Primary Key).

#### 2. myScheme Short Title / Code (`schemeShortTitle` / `slug`)
```text
data.en.basicDetails.schemeShortTitle
      ↓
Scheme.schemeCode
      ↓
SCHEMES.SCHEME_CODE (VARCHAR2(100))
      ↓
TRANSFORM
```
- **Transformation**: Uppercase string. If `schemeShortTitle` is null or empty, falls back to `slug.upper()`.

#### 3. myScheme Slug (`slug`)
```text
data.slug
      ↓
Scheme.slug
      ↓
SCHEMES.SLUG (VARCHAR2(200))
      ↓
NO TARGET FIELD (REQUIRES NEW COLUMN)
```
- **Transformation**: Direct string assignment to new column `SLUG`.

#### 4. myScheme Scheme Name (`schemeName`)
```text
data.en.basicDetails.schemeName
      ↓
Scheme.titleEnglish
      ↓
SCHEMES.TITLE_ENGLISH (VARCHAR2(300))
      ↓
DIRECT
```
- **Transformation**: Direct string assignment.

#### 5. myScheme Brief Description (`briefDescription` / `description`)
```text
data.en.basicDetails.briefDescription
      ↓
Scheme.descriptionEnglish
      ↓
SCHEMES.DESCRIPTION_ENGLISH (CLOB)
      ↓
TRANSFORM
```
- **Transformation**: Clean HTML tags (`clean_html`) and store as CLOB.

#### 6. myScheme Level / Level Type (`level`)
```text
data.en.basicDetails.level.value
      ↓
Scheme.schemeType
      ↓
SCHEMES.SCHEME_TYPE (VARCHAR2(50))
      ↓
TRANSFORM
```
- **Transformation**: If `level.value` contains `"central"`, set `"CENTRAL"`. If it contains `"state"`, set `"STATE"`.

#### 7. myScheme State Name (`state`)
```text
data.en.basicDetails.state.label
      ↓
Scheme.applicableStates
      ↓
SCHEMES.APPLICABLE_STATES (VARCHAR2(2000))
      ↓
TRANSFORM
```
- **Transformation**: If `state` object exists, map state name string; otherwise default to `"ALL_INDIA"`.

---

### 2.2 Taxonomy & Classification

#### 8. myScheme Scheme Category (`schemeCategory`)
```text
data.en.basicDetails.schemeCategory[0].label
      ↓
Scheme.category -> SchemeCategory
      ↓
SCHEMES.CATEGORY_ID -> SCHEME_CATEGORIES.ID (VARCHAR2(36))
      ↓
DERIVED
```
- **Transformation**: Derived foreign key lookup/creation in `SCHEME_CATEGORIES` table by category name string (e.g. `"Agriculture,Rural & Environment"`, `"Health & Wellness"`).

#### 9. myScheme Ministry / Nodal Department (`nodalMinistryName` / `nodalDepartmentName`)
```text
data.en.basicDetails.nodalMinistryName || nodalDepartmentName
      ↓
Scheme.department -> SchemeDepartment
      ↓
SCHEMES.DEPARTMENT_ID -> SCHEME_DEPARTMENTS.ID (VARCHAR2(36))
      ↓
DERIVED
```
- **Transformation**: Derived foreign key lookup/creation in `SCHEME_DEPARTMENTS` table.

#### 10. myScheme Tags (`tags`)
```text
data.en.basicDetails.tags (Array<String>)
      ↓
Scheme.tags -> List<SchemeTag>
      ↓
SCHEME_TAGS Table
      ↓
ARRAY NORMALIZATION
```
- **Transformation**: Iterate string array, insert child rows into `SCHEME_TAGS` table (`SCHEME_ID`, `TAG_NAME`).

---

### 2.3 Benefits, Eligibility & Content

#### 11. myScheme Benefits (`benefits`)
```text
data.en.benefits (Array<Object>)
      ↓
Scheme.benefits -> List<SchemeBenefit>
      ↓
SCHEME_BENEFITS Table
      ↓
ARRAY NORMALIZATION
```
- **Transformation**: Iterate benefits objects, clean HTML formatting, store description in `SCHEME_BENEFITS.DESCRIPTION_ENGLISH` (CLOB) with `BENEFIT_TYPE = "GENERAL"`.

#### 12. myScheme Eligibility Criteria (`eligibilityCriteria`)
```text
data.en.eligibilityCriteria (Array<Object>)
      ↓
Scheme.eligibilityRules -> List<SchemeEligibilityRule>
      ↓
SCHEME_ELIGIBILITY_RULES Table
      ↓
ARRAY NORMALIZATION
```
- **Transformation**: Parse criteria array into structured `SchemeEligibilityRule` rows (`RULE_TYPE = "GENERAL"`, `OPERATOR = "EQUALS"`, `VALUE_STRING = <rule_text>`).

#### 13. myScheme Application Process (`applicationProcess`)
```text
data.en.applicationProcess (Array<Object> / String)
      ↓
Scheme.applicationProcess
      ↓
SCHEMES.APPLICATION_PROCESS (CLOB)
      ↓
NO TARGET FIELD (REQUIRES NEW COLUMN)
```
- **Transformation**: Join step descriptions into structured text block and store as CLOB in new column `APPLICATION_PROCESS`.

#### 14. myScheme FAQs (`faqs`)
```text
data.en.faqs (Array<Object>)
      ↓
Scheme.faqs -> List<SchemeFaq>
      ↓
SCHEME_FAQS Table
      ↓
ARRAY NORMALIZATION
```
- **Transformation**: Iterate question/answer objects, insert child rows into `SCHEME_FAQS` table (`QUESTION_ENGLISH`, `ANSWER_ENGLISH`).

#### 15. myScheme DBT Status (`dbtScheme`)
```text
data.en.basicDetails.dbtScheme
      ↓
Scheme.dbtScheme
      ↓
SCHEMES.DBT_SCHEME (NUMBER(1,0))
      ↓
NO TARGET FIELD (REQUIRES NEW COLUMN)
```
- **Transformation**: Convert boolean/string `"Yes"`/`true` to `1` / `0`.

#### 16. myScheme Target Beneficiaries (`targetBeneficiaries` / `schemeFor`)
```text
data.en.basicDetails.targetBeneficiaries
      ↓
Scheme.targetBeneficiaries
      ↓
SCHEMES.TARGET_BENEFICIARIES (VARCHAR2(1000))
      ↓
NO TARGET FIELD (REQUIRES NEW COLUMN)
```
- **Transformation**: Join beneficiary labels into string.

---

### 2.4 Unsupported / Excluded Fields

| myScheme Field | Classification | Reason for Exclusion |
|---|---|---|
| `data.en.basicDetails.sponsor` | `NOT USED` | Not part of current SchemeBridge application model |
| `data.en.basicDetails.references` | `NOT USED` | Raw internal government web citations |

---

## 3. Data Gaps Analysis

1. **New Columns Required on `SCHEMES` Table**:
   - `SLUG (VARCHAR2(200))`
   - `APPLICATION_PROCESS (CLOB)`
   - `DBT_SCHEME (NUMBER(1,0))`
   - `TARGET_BENEFICIARIES (VARCHAR2(1000))`
   - `SCHEME_FOR (VARCHAR2(1000))`

2. **Unstructured Eligibility Parsing**:
   - myScheme eligibility rules are written in natural English text blocks (e.g. *"Applicant must be a small or marginal farmer"*).
   - In Phase 2B, these are stored in `SCHEME_ELIGIBILITY_RULES` with `RULE_TYPE = "GENERAL"` and text values, allowing the existing client-side `eligibilityEngine` to parse key keywords.

3. **Document Normalization**:
   - Document requirements in myScheme are embedded within step-by-step application instructions rather than a dedicated top-level `documentsRequired` array for 99.9% of schemes.
   - We extract document mentions from application text and normalize them into `SCHEME_DOCUMENTS`.

---

## 4. Summary Classification Breakdown

- **Total Fields Analyzed**: 16 core fields
- **Direct Mappings**: 3 (`ID`, `TITLE_ENGLISH`, `STATUS`)
- **Transformations**: 5 (`SCHEME_CODE`, `DESCRIPTION_ENGLISH`, `SCHEME_TYPE`, `APPLICABLE_STATES`, `DBT_SCHEME`)
- **Derived Fields**: 2 (`CATEGORY_ID`, `DEPARTMENT_ID`)
- **Array Normalizations**: 4 (`SCHEME_TAGS`, `SCHEME_BENEFITS`, `SCHEME_ELIGIBILITY_RULES`, `SCHEME_FAQS`)
- **No Target Field (New Columns Required)**: 4 (`SLUG`, `APPLICATION_PROCESS`, `DBT_SCHEME`, `TARGET_BENEFICIARIES`)
- **Unsupported / Not Used**: 2 (`sponsor`, `references`)
