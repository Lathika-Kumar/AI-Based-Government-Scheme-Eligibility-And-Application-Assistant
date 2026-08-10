package com.schemebridge.common.constants;

public final class SecurityConstants {

    private SecurityConstants() {
        // Private constructor to prevent instantiation
    }

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String CLAIMS_ROLES = "roles";
    public static final String CLAIMS_USER_ID = "userId";
    public static final String CLAIMS_EMAIL = "email";
    public static final long ACCESS_TOKEN_EXPIRATION_MS = 86400000L; // 24 hours
    public static final long REFRESH_TOKEN_EXPIRATION_MS = 604800000L; // 7 days
}
