package com.schemebridge.service.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "BREVO_API_KEY")
@Primary
@RequiredArgsConstructor
@Slf4j
public class BrevoEmailProvider implements EmailProvider {

    @Value("${BREVO_API_KEY:}")
    private String apiKey;

    @Value("${BREVO_SENDER_EMAIL:}")
    private String senderEmail;

    @Value("${BREVO_SENDER_NAME:}")
    private String senderName;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void sendEmail(String recipient, String subject, String htmlBody, String textBody) {
        if (apiKey == null || apiKey.isBlank() || senderEmail == null || senderEmail.isBlank() || senderName == null || senderName.isBlank()) {
            throw new IllegalStateException("Email delivery is unavailable. Please set BREVO_API_KEY, BREVO_SENDER_EMAIL, and BREVO_SENDER_NAME environment variables.");
        }

        try {
            String payload = buildPayload(recipient, subject, htmlBody, textBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("Content-Type", "application/json")
                    .header("api-key", apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                String body = response.body() == null ? "" : response.body();
                log.error("Brevo email delivery failed with status {} and body {}", response.statusCode(), body);
                throw new IllegalStateException("Brevo email delivery failed: " + response.statusCode());
            }

            log.info("Brevo email sent to {} successfully", recipient);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Brevo email delivery error", e);
            throw new IllegalStateException("Brevo email delivery failed: " + e.getMessage(), e);
        }
    }

    private String buildPayload(String recipient, String subject, String htmlBody, String textBody) {
        return "{" +
                "\"sender\":{\"email\":\"" + escapeJson(senderEmail.trim()) + "\",\"name\":\"" + escapeJson(senderName.trim()) + "\"}," +
                "\"to\":[{\"email\":\"" + escapeJson(recipient.trim()) + "\"}]," +
                "\"subject\":\"" + escapeJson(subject) + "\"," +
                "\"htmlContent\":\"" + escapeJson(htmlBody) + "\"," +
                "\"textContent\":\"" + escapeJson(textBody) + "\"" +
                "}";
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
