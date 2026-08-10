package com.schemebridge.notificationservice.constants;

public final class NotificationConstants {

    private NotificationConstants() {
        // Private constructor to prevent instantiation
    }

    // Security & Header Constants
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String DEFAULT_USER_ID = "citizen-001";
    public static final String SYSTEM_USER_ID = "SYSTEM";

    // Delivery & Retry Constants
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final String DEFAULT_LANGUAGE = "en";
    public static final String NOTIFICATION_ID_PREFIX = "NOTIF-";
    public static final String OUTBOX_ID_PREFIX = "OUTBOX-";
    public static final String DLQ_ID_PREFIX = "DLQ-";

    // Pagination Constants
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_SORT_BY = "createdAt";

    // Provider Identifiers
    public static final String PROVIDER_BREVO_EMAIL = "BrevoEmailProvider";
    public static final String PROVIDER_TWILIO_SMS = "TwilioSmsProviderPlaceholder";
    public static final String PROVIDER_FIREBASE_PUSH = "FirebasePushProviderPlaceholder";
    public static final String PROVIDER_IN_APP = "InAppNotificationProvider";

    // Exception Messages
    public static final String ERR_NOTIF_NOT_FOUND = "Notification not found with ID: ";
    public static final String ERR_PREF_NOT_FOUND = "Preferences not found for user: ";
}
