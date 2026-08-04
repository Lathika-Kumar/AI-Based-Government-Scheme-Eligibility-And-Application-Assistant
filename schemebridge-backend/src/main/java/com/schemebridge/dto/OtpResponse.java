package com.schemebridge.dto;

import com.schemebridge.enums.VerificationMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpResponse {
    private String message;
    private VerificationMethod method;
    private String recipient;
    private long expirySeconds;
    private int remainingVerificationAttempts;
    private int resendCount;
    private String simulatedOtp; // Included in dev sandbox for easy testing
}
