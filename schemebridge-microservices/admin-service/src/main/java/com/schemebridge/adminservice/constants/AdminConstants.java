package com.schemebridge.adminservice.constants;

public final class AdminConstants {

    private AdminConstants() {
        // Prevent instantiation
    }

    // Security & Header Constants
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String DEFAULT_ADMIN_ID = "admin-001";
    public static final String SYSTEM_ACTOR = "SYSTEM_ADMIN";

    // Pagination Constants
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_SORT_BY = "createdAt";

    // ID Prefixes
    public static final String OFFICER_ID_PREFIX = "OFFICER-";
    public static final String ANNOUNCEMENT_ID_PREFIX = "ANN-";
    public static final String FEEDBACK_ID_PREFIX = "FB-";
    public static final String REPORT_ID_PREFIX = "REP-";
    public static final String AUDIT_ID_PREFIX = "AUD-";

    // Error Messages
    public static final String ERR_OFFICER_NOT_FOUND = "Officer not found with ID: ";
    public static final String ERR_ANNOUNCEMENT_NOT_FOUND = "Announcement not found with ID: ";
    public static final String ERR_FEEDBACK_NOT_FOUND = "Feedback not found with ID: ";
    public static final String ERR_REPORT_NOT_FOUND = "Report not found with ID: ";
}
