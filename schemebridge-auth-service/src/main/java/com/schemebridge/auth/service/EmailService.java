package com.schemebridge.auth.service;

public interface EmailService {

    /**
     * Dispatches an OTP verification email to the user.
     *
     * @param toEmail       recipient email address
     * @param recipientName recipient full or first name
     * @param rawOtp        6-digit plaintext OTP token
     */
    void sendEmailVerificationOtp(String toEmail, String recipientName, String rawOtp);

    /**
     * Dispatches a password reset OTP email to the user.
     *
     * @param toEmail       recipient email address
     * @param recipientName recipient full or first name
     * @param rawOtp        6-digit plaintext OTP token
     */
    void sendPasswordResetOtp(String toEmail, String recipientName, String rawOtp);
}
