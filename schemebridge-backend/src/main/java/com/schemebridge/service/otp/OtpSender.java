package com.schemebridge.service.otp;

import com.schemebridge.enums.VerificationMethod;

public interface OtpSender {
    void sendOtp(String recipient, String otpCode, VerificationMethod method);
}
