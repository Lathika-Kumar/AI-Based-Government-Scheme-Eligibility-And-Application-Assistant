-- ============================================================
-- SchemeBridge – Module 6: Scheme Service Oracle DDL
-- Schema: SYSTEM | Database: Oracle XE (XEPDB1)
-- Port: 8083
-- ============================================================

-- SCHEME_CATEGORIES
CREATE TABLE SCHEME_CATEGORIES (
    id                  VARCHAR2(36)    DEFAULT SYS_GUID() PRIMARY KEY,
    name                VARCHAR2(150)   NOT NULL,
    name_tamil          VARCHAR2(150),
    name_english        VARCHAR2(150),
    code                VARCHAR2(50)    NOT NULL UNIQUE,
    description         CLOB,
    icon_url            VARCHAR2(500),
    display_order       NUMBER(5)       DEFAULT 0,
    status              VARCHAR2(20)    DEFAULT 'ACTIVE',
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    created_by          VARCHAR2(100),
    updated_by          VARCHAR2(100)
);

-- SCHEME_DEPARTMENTS
CREATE TABLE SCHEME_DEPARTMENTS (
    id                  VARCHAR2(36)    DEFAULT SYS_GUID() PRIMARY KEY,
    name                VARCHAR2(200)   NOT NULL,
    ministry            VARCHAR2(200),
    code                VARCHAR2(50)    NOT NULL UNIQUE,
    description         CLOB,
    website_url         VARCHAR2(500),
    helpline_number     VARCHAR2(50),
    status              VARCHAR2(20)    DEFAULT 'ACTIVE',
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    created_by          VARCHAR2(100),
    updated_by          VARCHAR2(100)
);

-- SCHEMES (core catalog table)
CREATE TABLE SCHEMES (
    id                      VARCHAR2(36)    DEFAULT SYS_GUID() PRIMARY KEY,
    scheme_code             VARCHAR2(100)   NOT NULL UNIQUE,
    title_english           VARCHAR2(300)   NOT NULL,
    title_tamil             VARCHAR2(300),
    description_english     CLOB,
    description_tamil       CLOB,
    category_id             VARCHAR2(36)    REFERENCES SCHEME_CATEGORIES(id),
    department_id           VARCHAR2(36)    REFERENCES SCHEME_DEPARTMENTS(id),
    scheme_type             VARCHAR2(50),   -- CENTRAL, STATE, CENTRALLY_SPONSORED
    launch_year             NUMBER(4),
    scheme_url              VARCHAR2(500),
    application_url         VARCHAR2(500),
    helpline_number         VARCHAR2(50),
    state_specific          NUMBER(1)       DEFAULT 0,
    applicable_states       VARCHAR2(2000),
    applicable_districts    VARCHAR2(2000),
    version                 NUMBER(5)       DEFAULT 1,
    effective_from          DATE,
    effective_to            DATE,
    -- Recommendation metadata
    priority                NUMBER(5)       DEFAULT 0,
    popularity_score        NUMBER(10,2)    DEFAULT 0,
    featured                NUMBER(1)       DEFAULT 0,
    newly_added             NUMBER(1)       DEFAULT 0,
    status                  VARCHAR2(20)    DEFAULT 'ACTIVE',
    created_at              TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at              TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    created_by              VARCHAR2(100),
    updated_by              VARCHAR2(100)
);

-- SCHEME_BENEFITS
CREATE TABLE SCHEME_BENEFITS (
    id                  VARCHAR2(36)    DEFAULT SYS_GUID() PRIMARY KEY,
    scheme_id           VARCHAR2(36)    NOT NULL REFERENCES SCHEMES(id),
    benefit_type        VARCHAR2(100),  -- FINANCIAL, IN_KIND, SERVICE, SUBSIDY, SCHOLARSHIP
    title               VARCHAR2(300),
    description_english CLOB,
    description_tamil   CLOB,
    amount_min          NUMBER(15,2),
    amount_max          NUMBER(15,2),
    frequency           VARCHAR2(50),   -- ONE_TIME, MONTHLY, ANNUALLY
    currency            VARCHAR2(10)    DEFAULT 'INR',
    display_order       NUMBER(5)       DEFAULT 0,
    status              VARCHAR2(20)    DEFAULT 'ACTIVE',
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL
);

-- SCHEME_ELIGIBILITY_RULES (dynamic, stored in Oracle – no hardcoding)
CREATE TABLE SCHEME_ELIGIBILITY_RULES (
    id                  VARCHAR2(36)    DEFAULT SYS_GUID() PRIMARY KEY,
    scheme_id           VARCHAR2(36)    NOT NULL REFERENCES SCHEMES(id),
    rule_type           VARCHAR2(50)    NOT NULL,  -- AGE, GENDER, INCOME, CATEGORY, RELIGION, OCCUPATION, STATE, etc.
    operator            VARCHAR2(20),              -- EQ, LTE, GTE, IN, BETWEEN, NOT_EQ
    value_string        VARCHAR2(1000),
    value_number_min    NUMBER(15,2),
    value_number_max    NUMBER(15,2),
    description         VARCHAR2(500),
    mandatory           NUMBER(1)       DEFAULT 1,
    display_order       NUMBER(5)       DEFAULT 0,
    status              VARCHAR2(20)    DEFAULT 'ACTIVE',
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL
);

-- SCHEME_DOCUMENTS
CREATE TABLE SCHEME_DOCUMENTS (
    id                  VARCHAR2(36)    DEFAULT SYS_GUID() PRIMARY KEY,
    scheme_id           VARCHAR2(36)    NOT NULL REFERENCES SCHEMES(id),
    document_name       VARCHAR2(300)   NOT NULL,
    document_type       VARCHAR2(100),
    description         VARCHAR2(1000),
    mandatory           NUMBER(1)       DEFAULT 1,
    display_order       NUMBER(5)       DEFAULT 0,
    status              VARCHAR2(20)    DEFAULT 'ACTIVE',
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL
);

-- SCHEME_TAGS
CREATE TABLE SCHEME_TAGS (
    id          VARCHAR2(36)    DEFAULT SYS_GUID() PRIMARY KEY,
    scheme_id   VARCHAR2(36)    NOT NULL REFERENCES SCHEMES(id),
    tag         VARCHAR2(100)   NOT NULL,
    created_at  TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL
);

-- SCHEME_FAQS
CREATE TABLE SCHEME_FAQS (
    id                  VARCHAR2(36)    DEFAULT SYS_GUID() PRIMARY KEY,
    scheme_id           VARCHAR2(36)    NOT NULL REFERENCES SCHEMES(id),
    question_english    VARCHAR2(1000),
    question_tamil      VARCHAR2(1000),
    answer_english      CLOB,
    answer_tamil        CLOB,
    display_order       NUMBER(5)       DEFAULT 0,
    status              VARCHAR2(20)    DEFAULT 'ACTIVE',
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL
);

-- SCHEME_VERSIONS (versioning history)
CREATE TABLE SCHEME_VERSIONS (
    id                  VARCHAR2(36)    DEFAULT SYS_GUID() PRIMARY KEY,
    scheme_id           VARCHAR2(36)    NOT NULL REFERENCES SCHEMES(id),
    version_number      NUMBER(5)       NOT NULL,
    change_summary      VARCHAR2(2000),
    effective_from      DATE,
    effective_to        DATE,
    changed_by          VARCHAR2(100),
    status              VARCHAR2(20)    DEFAULT 'ACTIVE',
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL
);

-- TRENDING_SCHEMES
CREATE TABLE TRENDING_SCHEMES (
    id                      VARCHAR2(36)    DEFAULT SYS_GUID() PRIMARY KEY,
    scheme_id               VARCHAR2(36)    NOT NULL REFERENCES SCHEMES(id) UNIQUE,
    rank_position           NUMBER(5)       NOT NULL,
    view_count              NUMBER(15)      DEFAULT 0,
    application_count       NUMBER(15)      DEFAULT 0,
    trending_score          NUMBER(10,2)    DEFAULT 0,
    trending_since          TIMESTAMP,
    status                  VARCHAR2(20)    DEFAULT 'ACTIVE',
    created_at              TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at              TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL
);

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX idx_schemes_category     ON SCHEMES(category_id);
CREATE INDEX idx_schemes_department   ON SCHEMES(department_id);
CREATE INDEX idx_schemes_status       ON SCHEMES(status);
CREATE INDEX idx_schemes_featured     ON SCHEMES(featured);
CREATE INDEX idx_schemes_type         ON SCHEMES(scheme_type);
CREATE INDEX idx_eligibility_scheme   ON SCHEME_ELIGIBILITY_RULES(scheme_id);
CREATE INDEX idx_eligibility_type     ON SCHEME_ELIGIBILITY_RULES(rule_type);
CREATE INDEX idx_benefits_scheme      ON SCHEME_BENEFITS(scheme_id);
CREATE INDEX idx_documents_scheme     ON SCHEME_DOCUMENTS(scheme_id);
CREATE INDEX idx_faqs_scheme          ON SCHEME_FAQS(scheme_id);
CREATE INDEX idx_tags_scheme          ON SCHEME_TAGS(scheme_id);
CREATE INDEX idx_versions_scheme      ON SCHEME_VERSIONS(scheme_id);
CREATE INDEX idx_trending_rank        ON TRENDING_SCHEMES(rank_position);

-- ============================================================
-- SEED DATA – Scheme Categories
-- ============================================================
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order) VALUES
    (SYS_GUID(), 'Agriculture', 'Agriculture', 'விவசாயம்', 'AGRICULTURE', 'Schemes for farmers and agriculture sector', 1);
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order) VALUES
    (SYS_GUID(), 'Education', 'Education', 'கல்வி', 'EDUCATION', 'Scholarship and education support schemes', 2);
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order) VALUES
    (SYS_GUID(), 'Health', 'Health', 'சுகாதாரம்', 'HEALTH', 'Health insurance and medical support schemes', 3);
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order) VALUES
    (SYS_GUID(), 'Housing', 'Housing', 'வீட்டுவசதி', 'HOUSING', 'Affordable housing and shelter schemes', 4);
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order) VALUES
    (SYS_GUID(), 'Women Empowerment', 'Women Empowerment', 'பெண் மேம்பாடு', 'WOMEN', 'Schemes for women empowerment and protection', 5);
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order) VALUES
    (SYS_GUID(), 'Social Welfare', 'Social Welfare', 'சமூக நலன்', 'SOCIAL_WELFARE', 'Pension, disability, and welfare schemes', 6);
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order) VALUES
    (SYS_GUID(), 'Skill Development', 'Skill Development', 'திறன் மேம்பாடு', 'SKILL', 'Vocational training and livelihood schemes', 7);
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order) VALUES
    (SYS_GUID(), 'Financial Inclusion', 'Financial Inclusion', 'நிதி சேர்க்கை', 'FINANCIAL', 'Banking, insurance, and microfinance schemes', 8);

-- ============================================================
-- SEED DATA – Departments
-- ============================================================
INSERT INTO SCHEME_DEPARTMENTS (id, name, ministry, code, helpline_number) VALUES
    (SYS_GUID(), 'Ministry of Agriculture & Farmers Welfare', 'Ministry of Agriculture', 'MOA', '1800-180-1551');
INSERT INTO SCHEME_DEPARTMENTS (id, name, ministry, code, helpline_number) VALUES
    (SYS_GUID(), 'Ministry of Education', 'Ministry of Education', 'MOE', '1800-111-265');
INSERT INTO SCHEME_DEPARTMENTS (id, name, ministry, code, helpline_number) VALUES
    (SYS_GUID(), 'Ministry of Health & Family Welfare', 'Ministry of Health', 'MOHFW', '104');
INSERT INTO SCHEME_DEPARTMENTS (id, name, ministry, code, helpline_number) VALUES
    (SYS_GUID(), 'Ministry of Housing & Urban Affairs', 'Ministry of Housing', 'MOHUA', '1800-11-6163');
INSERT INTO SCHEME_DEPARTMENTS (id, name, ministry, code, helpline_number) VALUES
    (SYS_GUID(), 'Ministry of Women & Child Development', 'Ministry of WCD', 'MOWCD', '181');

COMMIT;
