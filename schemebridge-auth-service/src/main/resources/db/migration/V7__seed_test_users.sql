-- Seed Default Active Admin and Citizen accounts for easy local testing & administration
-- Passwords:
--   admin@schemebridge.gov.in     => Admin@123456
--   citizen@schemebridge.gov.in   => Citizen@123456

MERGE INTO USERS t USING (
    SELECT 'admin@schemebridge.gov.in' AS EMAIL,
           'National' AS FIRST_NAME,
           'Admin' AS LAST_NAME,
           '$2a$10$pEbNm2VbR64zZfJxCGxqKuzIpCPifG4VS4mrrOIR9cw299JAXT83W' AS PASSWORD_HASH,
           '9876543200' AS PHONE_NUMBER,
           'ACTIVE' AS ACCOUNT_STATUS,
           1 AS EMAIL_VERIFIED
    FROM DUAL
) s ON (t.EMAIL = s.EMAIL)
WHEN NOT MATCHED THEN
    INSERT (FIRST_NAME, LAST_NAME, EMAIL, PASSWORD_HASH, PHONE_NUMBER, ACCOUNT_STATUS, EMAIL_VERIFIED)
    VALUES (s.FIRST_NAME, s.LAST_NAME, s.EMAIL, s.PASSWORD_HASH, s.PHONE_NUMBER, s.ACCOUNT_STATUS, s.EMAIL_VERIFIED);

MERGE INTO USERS t USING (
    SELECT 'citizen@schemebridge.gov.in' AS EMAIL,
           'Aarav' AS FIRST_NAME,
           'Sharma' AS LAST_NAME,
           '$2a$10$VtDiHLfzjuitlhYD5GsrHuAzacFx7lzA//PY2ktX1rJBl0hOGtx06' AS PASSWORD_HASH,
           '9876543201' AS PHONE_NUMBER,
           'ACTIVE' AS ACCOUNT_STATUS,
           1 AS EMAIL_VERIFIED
    FROM DUAL
) s ON (t.EMAIL = s.EMAIL)
WHEN NOT MATCHED THEN
    INSERT (FIRST_NAME, LAST_NAME, EMAIL, PASSWORD_HASH, PHONE_NUMBER, ACCOUNT_STATUS, EMAIL_VERIFIED)
    VALUES (s.FIRST_NAME, s.LAST_NAME, s.EMAIL, s.PASSWORD_HASH, s.PHONE_NUMBER, s.ACCOUNT_STATUS, s.EMAIL_VERIFIED);


-- Assign ROLE_ADMIN to admin@schemebridge.gov.in
MERGE INTO USER_ROLES t USING (
    SELECT u.ID AS USER_ID, r.ID AS ROLE_ID
    FROM USERS u, ROLES r
    WHERE u.EMAIL = 'admin@schemebridge.gov.in' AND r.NAME = 'ADMIN'
) s ON (t.USER_ID = s.USER_ID AND t.ROLE_ID = s.ROLE_ID)
WHEN NOT MATCHED THEN
    INSERT (USER_ID, ROLE_ID) VALUES (s.USER_ID, s.ROLE_ID);

-- Assign ROLE_USER to citizen@schemebridge.gov.in
MERGE INTO USER_ROLES t USING (
    SELECT u.ID AS USER_ID, r.ID AS ROLE_ID
    FROM USERS u, ROLES r
    WHERE u.EMAIL = 'citizen@schemebridge.gov.in' AND r.NAME = 'USER'
) s ON (t.USER_ID = s.USER_ID AND t.ROLE_ID = s.ROLE_ID)
WHEN NOT MATCHED THEN
    INSERT (USER_ID, ROLE_ID) VALUES (s.USER_ID, s.ROLE_ID);
