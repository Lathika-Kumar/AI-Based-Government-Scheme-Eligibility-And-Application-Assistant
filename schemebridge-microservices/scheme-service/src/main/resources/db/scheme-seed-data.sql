SET DEFINE OFF;

INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order, status) VALUES
    (SYS_GUID(), 'Agriculture', 'Agriculture', 'விவசாயம்', 'AGRICULTURE', 'Schemes for farmers and agriculture sector', 1, 'ACTIVE');
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order, status) VALUES
    (SYS_GUID(), 'Education', 'Education', 'கல்வி', 'EDUCATION', 'Scholarship and education support schemes', 2, 'ACTIVE');
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order, status) VALUES
    (SYS_GUID(), 'Health', 'Health', 'சுகாதாரம்', 'HEALTH', 'Health insurance and medical support schemes', 3, 'ACTIVE');
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order, status) VALUES
    (SYS_GUID(), 'Housing', 'Housing', 'வீட்டுவசதி', 'HOUSING', 'Affordable housing and shelter schemes', 4, 'ACTIVE');
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order, status) VALUES
    (SYS_GUID(), 'Women Empowerment', 'Women Empowerment', 'பெண் மேம்பாடு', 'WOMEN', 'Schemes for women empowerment and protection', 5, 'ACTIVE');
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order, status) VALUES
    (SYS_GUID(), 'Social Welfare', 'Social Welfare', 'சமூக நலன்', 'SOCIAL_WELFARE', 'Pension, disability, and welfare schemes', 6, 'ACTIVE');
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order, status) VALUES
    (SYS_GUID(), 'Skill Development', 'Skill Development', 'திறன் மேம்பாடு', 'SKILL', 'Vocational training and livelihood schemes', 7, 'ACTIVE');
INSERT INTO SCHEME_CATEGORIES (id, name, name_english, name_tamil, code, description, display_order, status) VALUES
    (SYS_GUID(), 'Financial Inclusion', 'Financial Inclusion', 'நிதி சேர்க்கை', 'FINANCIAL', 'Banking, insurance, and microfinance schemes', 8, 'ACTIVE');

INSERT INTO SCHEME_DEPARTMENTS (id, name, ministry, code, helpline_number, status) VALUES
    (SYS_GUID(), 'Ministry of Agriculture and Farmers Welfare', 'Ministry of Agriculture', 'MOA', '1800-180-1551', 'ACTIVE');
INSERT INTO SCHEME_DEPARTMENTS (id, name, ministry, code, helpline_number, status) VALUES
    (SYS_GUID(), 'Ministry of Education', 'Ministry of Education', 'MOE', '1800-111-265', 'ACTIVE');
INSERT INTO SCHEME_DEPARTMENTS (id, name, ministry, code, helpline_number, status) VALUES
    (SYS_GUID(), 'Ministry of Health and Family Welfare', 'Ministry of Health', 'MOHFW', '104', 'ACTIVE');
INSERT INTO SCHEME_DEPARTMENTS (id, name, ministry, code, helpline_number, status) VALUES
    (SYS_GUID(), 'Ministry of Housing and Urban Affairs', 'Ministry of Housing', 'MOHUA', '1800-11-6163', 'ACTIVE');
INSERT INTO SCHEME_DEPARTMENTS (id, name, ministry, code, helpline_number, status) VALUES
    (SYS_GUID(), 'Ministry of Women and Child Development', 'Ministry of WCD', 'MOWCD', '181', 'ACTIVE');

COMMIT;
EXIT;
