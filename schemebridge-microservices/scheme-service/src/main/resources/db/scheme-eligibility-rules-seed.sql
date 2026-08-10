-- ============================================================
-- SchemeBridge – Module 6: Scheme Service Seed Script
-- File: scheme-eligibility-rules-seed.sql
-- Database: Oracle XE (XEPDB1) | Schema: SYSTEM
-- Purpose: Seeds realistic eligibility rules for government schemes
-- ============================================================

SET DEFINE OFF;

-- Delete old rules for clean idempotent execution
DELETE FROM SCHEME_ELIGIBILITY_RULES;

-- ------------------------------------------------------------
-- 1. Rules for PM_KISAN_2026 (PM-KISAN Samman Nidhi)
-- ------------------------------------------------------------
INSERT INTO SCHEME_ELIGIBILITY_RULES (id, scheme_id, rule_type, operator, value_string, value_number_min, value_number_max, description, mandatory, display_order, status)
VALUES (
    SYS_GUID(),
    (SELECT id FROM SCHEMES WHERE scheme_code = 'PM_KISAN_2026'),
    'FARMER', 'EQ', 'true', NULL, NULL,
    'Applicant must be a registered small or marginal farmer', 1, 1, 'ACTIVE'
);

INSERT INTO SCHEME_ELIGIBILITY_RULES (id, scheme_id, rule_type, operator, value_string, value_number_min, value_number_max, description, mandatory, display_order, status)
VALUES (
    SYS_GUID(),
    (SELECT id FROM SCHEMES WHERE scheme_code = 'PM_KISAN_2026'),
    'INCOME', 'LTE', NULL, NULL, 200000,
    'Annual family income must not exceed Rs. 2,00,000', 1, 2, 'ACTIVE'
);

INSERT INTO SCHEME_ELIGIBILITY_RULES (id, scheme_id, rule_type, operator, value_string, value_number_min, value_number_max, description, mandatory, display_order, status)
VALUES (
    SYS_GUID(),
    (SELECT id FROM SCHEMES WHERE scheme_code = 'PM_KISAN_2026'),
    'AGE', 'GTE', NULL, 18, NULL,
    'Applicant age must be at least 18 years', 1, 3, 'ACTIVE'
);

-- ------------------------------------------------------------
-- 2. Rules for PMAY_URBAN_2026 (PM Awas Yojana - Urban)
-- ------------------------------------------------------------
INSERT INTO SCHEME_ELIGIBILITY_RULES (id, scheme_id, rule_type, operator, value_string, value_number_min, value_number_max, description, mandatory, display_order, status)
VALUES (
    SYS_GUID(),
    (SELECT id FROM SCHEMES WHERE scheme_code = 'PMAY_URBAN_2026'),
    'INCOME', 'LTE', NULL, NULL, 300000,
    'Annual household income must not exceed Rs. 3,00,000 (EWS/LIG category)', 1, 1, 'ACTIVE'
);

INSERT INTO SCHEME_ELIGIBILITY_RULES (id, scheme_id, rule_type, operator, value_string, value_number_min, value_number_max, description, mandatory, display_order, status)
VALUES (
    SYS_GUID(),
    (SELECT id FROM SCHEMES WHERE scheme_code = 'PMAY_URBAN_2026'),
    'AGE', 'GTE', NULL, 21, NULL,
    'Head of household age must be at least 21 years', 1, 2, 'ACTIVE'
);

INSERT INTO SCHEME_ELIGIBILITY_RULES (id, scheme_id, rule_type, operator, value_string, value_number_min, value_number_max, description, mandatory, display_order, status)
VALUES (
    SYS_GUID(),
    (SELECT id FROM SCHEMES WHERE scheme_code = 'PMAY_URBAN_2026'),
    'CATEGORY', 'IN', 'OBC,SC,ST,GENERAL,EWS', NULL, NULL,
    'Open to all social categories under urban poor mission', 1, 3, 'ACTIVE'
);

COMMIT;
EXIT;
