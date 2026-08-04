package com.schemebridge.service.otp.impl;

import com.schemebridge.enums.VerificationMethod;
import com.schemebridge.service.otp.OtpSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SimulatedOtpSenderImpl implements OtpSender {

    @Override
    public void sendOtp(String recipient, String otpCode, VerificationMethod method) {
        log.info("[OTP SANDBOX SENDER] Dispatched 6-digit OTP code [{}] via {} to recipient [{}]",
                otpCode, method, recipient);
    }
}
