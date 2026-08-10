package com.schemebridge.authservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailService.class);

    private final RestTemplate restTemplate;

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.sender-email}")
    private String senderEmail;

    @Value("${brevo.sender-name}")
    private String senderName;

    public BrevoEmailService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendOtpEmail(String recipientEmail, String otpCode, String subject) {
        try {
            String url = "https://api.brevo.com/v3/smtp/email";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("email", senderEmail, "name", senderName));
            body.put("to", List.of(Map.of("email", recipientEmail)));
            body.put("subject", subject);
            body.put("htmlContent", "<html><body>" +
                    "<h2>SchemeBridge Verification Code</h2>" +
                    "<p>Your OTP verification code is: <strong style='font-size: 24px; color: #1e40af;'>" + otpCode + "</strong></p>" +
                    "<p>This code expires in 10 minutes. Do not share this code with anyone.</p>" +
                    "</body></html>");

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, requestEntity, String.class);
            log.info("Successfully sent OTP email via Brevo to: {}", recipientEmail);

        } catch (Exception ex) {
            log.warn("Brevo API email dispatch failed for {}: {}. Falling back to console log for testing: OTP={}", recipientEmail, ex.getMessage(), otpCode);
        }
    }
}
