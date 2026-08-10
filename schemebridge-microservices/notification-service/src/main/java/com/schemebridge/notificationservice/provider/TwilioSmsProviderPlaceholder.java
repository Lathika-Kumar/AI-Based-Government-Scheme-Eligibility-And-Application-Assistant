package com.schemebridge.notificationservice.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TwilioSmsProviderPlaceholder implements SmsProvider {

    @Value("${schemebridge.notification.sms.twilio.account-sid:AC_test_placeholder_sid}")
    private String accountSid;

    @Value("${schemebridge.notification.sms.twilio.from-number:+18005550199}")
    private String fromNumber;

    @Override
    public boolean sendSms(String toPhone, String messageText) {
        log.info("[SIMULATED TWILIO SMS] Sending SMS via Twilio from {} to {}: {}", fromNumber, toPhone, messageText);
        // Twilio integration placeholder ready for production SDK injection
        return true;
    }

    @Override
    public String getProviderName() {
        return "TwilioSmsProviderPlaceholder";
    }

    @Override
    public boolean isAvailable() {
        return accountSid != null && !accountSid.trim().isEmpty();
    }
}
