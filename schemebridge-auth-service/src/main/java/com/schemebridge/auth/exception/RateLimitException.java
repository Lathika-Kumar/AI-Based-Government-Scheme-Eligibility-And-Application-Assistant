package com.schemebridge.auth.exception;

/**
 * Thrown when a rate-limited operation is attempted too frequently.
 * e.g., OTP resend within the cooldown window.
 */
public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
