package com.schemebridge.service.email;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimulatedEmailProvider implements EmailProvider {

    @Override
    public void sendEmail(String recipient, String subject, String htmlBody, String textBody) {
        log.warn("[SimulatedEmailProvider] Email delivery is not configured. Recipient={}, subject={}", recipient, subject);
        log.debug("Simulated email body (text): {}", textBody);
    }
}
