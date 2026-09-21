package com.schemebridge.auth.util;

/**
 * Utility for safe diagnostic logging without exposing sensitive citizen data.
 */
public final class LogUtils {

    private LogUtils() {}

    /**
     * Masks an email address for safe logging.
     * Example: citizen@schemebridge.gov.in -> ci***n@schemebridge.gov.in
     *
     * @param email original email
     * @return masked email or safe placeholder
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "<empty>";
        }
        String trimmed = email.trim();
        int atIdx = trimmed.indexOf('@');
        if (atIdx <= 0) {
            return "***";
        }
        String local = trimmed.substring(0, atIdx);
        String domain = trimmed.substring(atIdx);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.substring(0, 2) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
