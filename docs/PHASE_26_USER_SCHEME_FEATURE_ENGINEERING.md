# Phase 26: User–Scheme Feature Engineering & Comparison Specification

## 1. Architectural Overview & Non-Negotiable Safety Gate

Phase 26 establishes the foundational feature engineering and eligibility comparison pipeline that transforms citizen demographic profiles and verified government scheme criteria into deterministic, privacy-safe, and explainable feature vectors.

### Execution Order & Invariants

```
Citizen Profile
    ↓
EligibilityEngine.evaluate()  [STATUTORY HARD GATE]
    ↓
Remove INELIGIBLE / Insufficient Schemes
    ↓
User Features Extraction (Zero PII)
    ↓
Scheme Features Extraction (Authoritative Criteria)
    ↓
Deterministic Comparison (MATCH / MISMATCH / UNKNOWN)
    ↓
UserSchemeFeatureVector Construction (Schema 1.0.0)
    ↓
Existing Active Ranking Model (2.2.0-hybrid-semantic-384d / fallback 1.0.0-deterministic)
    ↓
Personalized Recommendations with Explainable Justification
```

**Non-Negotiable Safety Invariant**:
- ML and feature comparisons are **strictly forbidden** from determining statutory eligibility.
- Statutory evaluation is executed strictly before ranking candidate extraction.
- **Statutory Eligibility Violation Rate**: `0.00%`.

---

## 2. Canonical User Features (`UserFeatures`)

Extracted via `UserFeatureExtractor.java` from `CitizenProfile` / `CitizenEligibilityProfile`.

| Feature Name | Type | Description | PII Protection / Missing Handling |
| :--- | :--- | :--- | :--- |
| `ageBucket` | String | Discrete bucket (e.g., `AGE_18_25`, `AGE_26_35`, `AGE_60_PLUS`) | Coarsened demographic; defaults to `UNKNOWN` if null |
| `stateCode` | String | State/UT identifier (e.g., `TN`, `DL`, `KA`) | Preserved for jurisdictional matching |
| `district` | String | District identifier | Optional geographical locality |
| `occupationCode` | String | Standardized occupation (e.g., `FARMER`, `STUDENT`, `ARTISAN`) | Defaults to null if unspecified |
| `incomeTier` | String | Coarsened income bracket (`TIER_0_1L`, `TIER_1_3L`, `BPL`, etc.) | Exact rupees masked into privacy-safe tiers |
| `categoryCode` | String | Social reservation category (`GEN`, `OBC`, `SC`, `ST`, `EWS`) | Used for affirmative-action scheme matching |
| `genderCode` | String | Gender identifier (`MALE`, `FEMALE`, `TRANSGENDER`, `OTHER`) | Standardized enum string |
| `disabilityStatus` | Boolean | True if citizen has physical disability / PwD certificate | Defaults to null |
| `isFarmer` | Boolean | Derived or explicit agricultural status | Defaults to null |
| `isStudent` | Boolean | Derived or explicit student enrollment status | Defaults to null |
| `bplStatus` | Boolean | Below Poverty Line ration card flag | Defaults to null |
| `educationLevel` | String | Educational attainment level | Defaults to null |

> [!IMPORTANT]
> **Strict Zero-PII Guarantee**: The feature model does not contain and cannot capture `fullName`, `email`, `mobileNumber`, `aadhaarNumber`, `address`, `pincode`, `udidNumber`, or any direct citizen identifiers.

---

## 3. Canonical Scheme Features (`SchemeFeatures`)

Extracted via `SchemeFeatureExtractor.java` from authoritative master `Scheme` and canonical `SchemeVerifiedData`.

| Feature Name | Type | Description | Source |
| :--- | :--- | :--- | :--- |
| `schemeCode` | String | Canonical scheme code (e.g. `SCH-PMKISAN-001`) | Authoritative scheme record |
| `schemeCategory` | String | Category code (e.g. `AGRICULTURE`, `EDUCATION`) | Master scheme category ref |
| `benefitCategory` | String | Financial assistance, subsidy, scholarship, etc. | Scheme benefit definition |
| `schemeLevel` | String | `CENTRAL` or `STATE` | Scheme jurisdiction level |
| `stateOrUt` | String | Domicile state requirement | Master scheme metadata |
| `minAge` | Integer | Minimum statutory age threshold | Parsed RuleGroup or verified structured criteria |
| `maxAge` | Integer | Maximum statutory age threshold | Parsed RuleGroup or verified structured criteria |
| `maxIncome` | Double | Maximum annual income cap in INR | Parsed RuleGroup or verified structured criteria |
| `eligibleOccupations` | List\<String\> | List of eligible occupations | Structured rule conditions & beneficiary hints |
| `eligibleCategories` | List\<String\> | List of eligible social categories | Structured rule conditions |
| `eligibleGenders` | List\<String\> | List of eligible genders | Structured rule conditions & beneficiary hints |
| `disabilityApplicable` | Boolean | Flag indicating PwD / Divyang eligibility | Structured rule conditions |

---

## 4. Deterministic Comparison Engine (`UserSchemeComparisonService`)

Every criterion comparison produces **strictly one of three states**:
- `MATCH`: Citizen profile positively meets the scheme criterion.
- `MISMATCH`: Citizen profile positively violates the scheme criterion.
- `UNKNOWN`: Criterion or citizen attribute is unspecified, missing, or inconclusive.

```
+------------------+-----------------------+-------------------+
| User Attribute   | Scheme Criterion      | Comparison Result |
+------------------+-----------------------+-------------------+
| Age = 24         | Age: 18–35            | MATCH             |
| Income = 80,000  | Max Income <= 250,000 | MATCH             |
| State = TN       | State = TN (or CENTRAL)| MATCH             |
| Occupation = Student| Occ = [STUDENT]    | MATCH             |
| Category = General| Cat = [OBC, SC, ST]  | MISMATCH          |
| Gender = MALE    | Not specified         | UNKNOWN           |
| Missing info     | Any criterion         | UNKNOWN           |
+------------------+-----------------------+-------------------+
```

### Deterministic Comparison Rules:
1. **Age**: User's `ageBucket` range is compared against `[minAge, maxAge]`. Overlapping valid ages yield `MATCH`, disjoint yields `MISMATCH`, missing bounds yield `UNKNOWN`.
2. **Income**: Evaluates `incomeTier` upper limit against `maxIncome`. `BPL` tier unconditionally matches standard poverty caps.
3. **State**: Central schemes (`schemeLevel == CENTRAL` or `stateOrUt == null`) automatically yield `MATCH`. State schemes require case-insensitive state code match.
4. **Occupation**: Case-insensitive substring and semantic alias checks across `eligibleOccupations`.
5. **Missing Information**: Missing demographic attributes **never** convert to `MATCH`. They strictly resolve to `UNKNOWN`.

---

## 5. Versioned Feature Vector (`UserSchemeFeatureVector`)

- **Schema Version**: `1.0.0`
- **Builder**: `FeatureVectorBuilder.java`
- **Properties**:
  - Deterministic: Identical user and scheme inputs yield identical feature vectors.
  - Privacy-safe: Contains zero PII.
  - Explainable: Directly links tri-state match flags to human-readable recommendation explanations.
  - Serializable: Compatible with future JSON and Parquet offline datasets.
