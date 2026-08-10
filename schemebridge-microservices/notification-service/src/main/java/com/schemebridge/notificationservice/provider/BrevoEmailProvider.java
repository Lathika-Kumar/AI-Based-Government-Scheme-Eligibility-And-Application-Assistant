package com.schemebridge.notificationservice.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class BrevoEmailProvider implements EmailProvider {

    @Value("${schemebridge.notification.email.brevo.api-key:xkeysib-placeholder-test-key}")
    private String apiKey;

    @Value("${schemebridge.notification.email.brevo.sender-email:no-reply@schemebridge.gov.in}")
    private String senderEmail;

    @Value("${schemebridge.notification.email.brevo.sender-name:SchemeBridge Notifications}")
    private String senderName;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean sendEmail(String toEmail, String subject, String textContent, String htmlContent) {
        log.info("Sending Email via Brevo API to: {}, Subject: {}", toEmail, subject);

        if (apiKey == null || apiKey.contains("placeholder")) {
            log.info("[SIMULATED EMAIL DISPATCH] Brevo API Key not configured or placeholder. Simulating successful email dispatch to {}", toEmail);
            return true;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            Map<String, Object> body = Map.of(
                    "sender", Map.of("name", senderName, "email", senderEmail),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", subject,
                    "htmlContent", htmlContent != null ? htmlContent : "<p>" + textContent + "</p>"
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(BREVO_API_URL, request, String.class);
            log.info("Brevo email dispatched successfully to {}", toEmail);
            return true;
        } catch (Exception ex) {
            log.error("Failed to dispatch email via Brevo API to {}: {}", toEmail, ex.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "BrevoEmailProvider";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
