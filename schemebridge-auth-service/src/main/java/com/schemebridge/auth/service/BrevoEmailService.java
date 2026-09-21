package com.schemebridge.auth.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.schemebridge.auth.exception.EmailDeliveryException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;
import java.util.List;

@Service
@Primary
@Slf4j
public class BrevoEmailService implements EmailService {

    @Value("${brevo.enabled:false}")
    private boolean brevoEnabled;

    @Value("${brevo.api-key:}")
    private String apiKey;

    @Value("${brevo.sender-email:}")
    private String senderEmail;

    @Value("${brevo.sender-name:SchemeBridge}")
    private String senderName;

    @Value("${brevo.endpoint:https://api.brevo.com/v3/smtp/email}")
    private String endpoint;

    @Value("${otp.log-raw-value:false}")
    private boolean logRawOtp;

    private final RestClient restClient;
    private SmtpEmailService smtpEmailService;

    @org.springframework.beans.factory.annotation.Autowired
    public BrevoEmailService(SmtpEmailService smtpEmailService) {
        this.smtpEmailService = smtpEmailService;
        this.restClient = RestClient.builder().build();
    }

    public BrevoEmailService() {
        this.restClient = RestClient.builder().build();
    }

    public BrevoEmailService(RestClient restClient) {
        this.restClient = restClient;
    }

    public BrevoEmailService(RestClient restClient, SmtpEmailService smtpEmailService) {
        this.restClient = restClient;
        this.smtpEmailService = smtpEmailService;
    }

    public void setSmtpEmailService(SmtpEmailService smtpEmailService) {
        this.smtpEmailService = smtpEmailService;
    }

    @Override
    public void sendEmailVerificationOtp(String toEmail, String recipientName, String rawOtp) {
        String subject = "SchemeBridge — Email Verification OTP";
        String htmlContent = buildOtpEmailTemplate(
                recipientName,
                "Email Verification",
                rawOtp,
                "Thank you for registering on SchemeBridge, the National Public Welfare and Government Scheme Portal.",
                "Please enter the 6-digit verification code below to verify your email address and activate your citizen account:"
        );
        String plainText = "Dear " + (recipientName != null ? recipientName : "Citizen") + ",\n\n"
                + "Your SchemeBridge Email Verification OTP is: " + rawOtp + "\n\n"
                + "This OTP is valid for 10 minutes. Do not share this OTP with anyone for security reasons.\n\n"
                + "Regards,\nSchemeBridge National Portal Team";

        dispatchEmail(toEmail, recipientName, subject, htmlContent, plainText, rawOtp, true);
    }

    @Override
    public void sendPasswordResetOtp(String toEmail, String recipientName, String rawOtp) {
        String subject = "SchemeBridge — Password Reset OTP";
        String htmlContent = buildOtpEmailTemplate(
                recipientName,
                "Password Reset",
                rawOtp,
                "We received a request to reset the password for your SchemeBridge account.",
                "Use the 6-digit security code below to complete your password reset:"
        );
        String plainText = "Dear " + (recipientName != null ? recipientName : "Citizen") + ",\n\n"
                + "Your SchemeBridge Password Reset OTP is: " + rawOtp + "\n\n"
                + "This OTP is valid for 10 minutes. If you did not request a password reset, please ignore this email immediately.\n\n"
                + "Regards,\nSchemeBridge National Portal Team";

        dispatchEmail(toEmail, recipientName, subject, htmlContent, plainText, rawOtp, false);
    }

    private void dispatchEmail(String toEmail, String recipientName, String subject,
                               String htmlContent, String plainText, String rawOtp, boolean isVerification) {
        String maskedEmail = com.schemebridge.auth.util.LogUtils.maskEmail(toEmail);
        log.info("Initiating email dispatch to: {} [Subject: {}]", maskedEmail, subject);

        if (!brevoEnabled) {
            if (smtpEmailService != null) {
                log.info("Brevo email disabled; routing dispatch to SMTP service for {}", maskedEmail);
                if (isVerification) {
                    smtpEmailService.sendEmailVerificationOtp(toEmail, recipientName, rawOtp);
                } else {
                    smtpEmailService.sendPasswordResetOtp(toEmail, recipientName, rawOtp);
                }
                return;
            }
            if (logRawOtp) {
                log.info("[TEST-ONLY] Generated OTP for user {}: {}", toEmail, rawOtp);
            } else {
                log.debug("Brevo email disabled; simulated delivery for {}", toEmail);
            }
            return;
        }

        // Validate required configuration when Brevo is enabled
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Brevo email delivery failed: BREVO_API_KEY is missing or empty.");
            if (smtpEmailService != null) {
                log.warn("Falling back to SMTP service due to missing BREVO_API_KEY for {}", toEmail);
                if (isVerification) {
                    smtpEmailService.sendEmailVerificationOtp(toEmail, recipientName, rawOtp);
                } else {
                    smtpEmailService.sendPasswordResetOtp(toEmail, recipientName, rawOtp);
                }
                return;
            }
            throw new EmailDeliveryException("Email service configuration error. Please check BREVO_API_KEY.");
        }

        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            log.error("Brevo email delivery failed: BREVO_SENDER_EMAIL is missing or empty.");
            if (smtpEmailService != null) {
                log.warn("Falling back to SMTP service due to missing BREVO_SENDER_EMAIL for {}", toEmail);
                if (isVerification) {
                    smtpEmailService.sendEmailVerificationOtp(toEmail, recipientName, rawOtp);
                } else {
                    smtpEmailService.sendPasswordResetOtp(toEmail, recipientName, rawOtp);
                }
                return;
            }
            throw new EmailDeliveryException("Email service configuration error. Please check BREVO_SENDER_EMAIL.");
        }

        String effectiveSenderName = (senderName != null && !senderName.trim().isEmpty()) ? senderName.trim() : "SchemeBridge";
        String effectiveRecipientName = (recipientName != null && !recipientName.trim().isEmpty()) ? recipientName.trim() : "Citizen";

        BrevoEmailRequest requestPayload = BrevoEmailRequest.builder()
                .sender(new BrevoContact(effectiveSenderName, senderEmail.trim()))
                .to(Collections.singletonList(new BrevoContact(effectiveRecipientName, toEmail.trim())))
                .subject(subject)
                .htmlContent(htmlContent)
                .textContent(plainText)
                .build();

        try {
            ResponseEntity<BrevoEmailResponse> response = restClient.post()
                    .uri(endpoint)
                    .header("api-key", apiKey.trim())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .body(requestPayload)
                    .retrieve()
                    .toEntity(BrevoEmailResponse.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String messageId = response.getBody() != null ? response.getBody().getMessageId() : "N/A";
                log.info("Brevo transactional email sent successfully to: {} [messageId: {}]", toEmail, messageId);
            } else {
                log.error("Brevo API returned non-2xx status: {}", response.getStatusCode());
                throw new EmailDeliveryException("Brevo API returned unexpected status: " + response.getStatusCode());
            }
        } catch (RestClientResponseException ex) {
            log.error("Brevo API error response (HTTP {}): {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            if (smtpEmailService != null) {
                log.warn("Brevo API failed with HTTP {}. Attempting SMTP fallback for {}...", ex.getStatusCode().value(), toEmail);
                try {
                    if (isVerification) {
                        smtpEmailService.sendEmailVerificationOtp(toEmail, recipientName, rawOtp);
                    } else {
                        smtpEmailService.sendPasswordResetOtp(toEmail, recipientName, rawOtp);
                    }
                    return;
                } catch (Exception smtpEx) {
                    log.error("SMTP fallback also failed for {}: {}", toEmail, smtpEx.getMessage());
                }
            }
            throw new EmailDeliveryException(extractBrevoErrorMessage(ex), ex);
        } catch (Exception ex) {
            log.error("Unexpected error during Brevo email dispatch to {}: {}", toEmail, ex.getMessage());
            if (smtpEmailService != null) {
                log.warn("Brevo error. Attempting SMTP fallback for {}...", toEmail);
                try {
                    if (isVerification) {
                        smtpEmailService.sendEmailVerificationOtp(toEmail, recipientName, rawOtp);
                    } else {
                        smtpEmailService.sendPasswordResetOtp(toEmail, recipientName, rawOtp);
                    }
                    return;
                } catch (Exception smtpEx) {
                    log.error("SMTP fallback also failed for {}: {}", toEmail, smtpEx.getMessage());
                }
            }
            throw new EmailDeliveryException("Failed to dispatch email via Brevo: " + ex.getMessage(), ex);
        }
    }

    private String extractBrevoErrorMessage(RestClientResponseException ex) {
        String detail = "";
        try {
            String responseBody = ex.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isBlank()) {
                if (responseBody.contains("unrecognised IP address")) {
                    detail = "Unrecognised IP address. Add your current IP address to authorized IPs at https://app.brevo.com/security/authorised_ips or update BREVO_API_KEY.";
                } else if (responseBody.contains("Key not found") || responseBody.contains("unauthorized")) {
                    detail = "Invalid or unauthorized API key. Please check BREVO_API_KEY.";
                } else if (responseBody.contains("message")) {
                    detail = responseBody.replaceAll("[{}\"]", "").trim();
                }
            }
        } catch (Exception ignored) {}
        if (!detail.isBlank()) {
            return "Failed to dispatch email via Brevo (HTTP " + ex.getStatusCode().value() + "): " + detail;
        }
        return "Failed to dispatch email via Brevo: HTTP " + ex.getStatusCode().value();
    }

    private String buildOtpEmailTemplate(String name, String actionTitle, String otp, String introText, String instructionText) {
        String displayName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Citizen";
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>SchemeBridge Notification</title>"
                + "</head>"
                + "<body style='margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; color: #1e293b;'>"
                + "<table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);'>"
                + "  <!-- Tricolor Banner -->"
                + "  <tr>"
                + "    <td style='height: 6px; background: linear-gradient(to right, #FF9933 33.3%, #ffffff 33.3%, #ffffff 66.6%, #138808 66.6%);'></td>"
                + "  </tr>"
                + "  <!-- Header -->"
                + "  <tr>"
                + "    <td style='padding: 28px 36px; background-color: #0f2a4a; text-align: center;'>"
                + "      <h1 style='margin: 0; color: #ffffff; font-size: 24px; font-weight: 800; letter-spacing: -0.5px;'>SchemeBridge</h1>"
                + "      <p style='margin: 4px 0 0 0; color: #94a3b8; font-size: 12px; font-weight: 500;'>National Public Welfare & Scheme Eligibility Platform</p>"
                + "    </td>"
                + "  </tr>"
                + "  <!-- Body -->"
                + "  <tr>"
                + "    <td style='padding: 36px 36px 24px 36px;'>"
                + "      <h2 style='margin: 0 0 16px 0; color: #0f172a; font-size: 20px; font-weight: 700;'>" + actionTitle + "</h2>"
                + "      <p style='margin: 0 0 16px 0; font-size: 14px; line-height: 1.6; color: #334155;'>Dear <strong>" + displayName + "</strong>,</p>"
                + "      <p style='margin: 0 0 16px 0; font-size: 14px; line-height: 1.6; color: #475569;'>" + introText + "</p>"
                + "      <p style='margin: 0 0 24px 0; font-size: 14px; line-height: 1.6; color: #475569;'>" + instructionText + "</p>"
                + "      <!-- OTP Box -->"
                + "      <div style='text-align: center; margin: 28px 0;'>"
                + "        <div style='display: inline-block; padding: 16px 36px; background-color: #f8fafc; border: 2px dashed #cbd5e1; border-radius: 12px;'>"
                + "          <span style='font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #0f2a4a; font-family: monospace;'>" + otp + "</span>"
                + "        </div>"
                + "      </div>"
                + "      <!-- Expiry Warning -->"
                + "      <div style='background-color: #fffbeb; border: 1px solid #fef3c7; border-radius: 8px; padding: 12px 16px; margin-bottom: 24px;'>"
                + "        <p style='margin: 0; font-size: 12px; color: #92400e; line-height: 1.5;'>"
                + "          <strong>Important:</strong> This verification code is valid for <strong>10 minutes</strong>. Do not share this code with anyone. SchemeBridge representatives will never ask for your OTP."
                + "        </p>"
                + "      </div>"
                + "      <p style='margin: 0; font-size: 13px; color: #64748b; line-height: 1.5;'>"
                + "        If you did not initiate this request, please disregard this email or contact the SchemeBridge Helpdesk immediately."
                + "      </p>"
                + "    </td>"
                + "  </tr>"
                + "  <!-- Footer -->"
                + "  <tr>"
                + "    <td style='padding: 24px 36px; background-color: #f8fafc; border-top: 1px solid #e2e8f0; text-align: center;'>"
                + "      <p style='margin: 0; font-size: 11px; color: #94a3b8;'>SchemeBridge — Government Welfare Scheme Delivery Platform</p>"
                + "      <p style='margin: 4px 0 0 0; font-size: 10px; color: #cbd5e1;'>This is an automated administrative notification. Please do not reply to this email.</p>"
                + "    </td>"
                + "  </tr>"
                + "</table>"
                + "</body>"
                + "</html>";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTOs for Brevo API communication
    // ─────────────────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrevoEmailRequest {
        private BrevoContact sender;
        private List<BrevoContact> to;
        private String subject;
        private String htmlContent;
        private String textContent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrevoContact {
        private String name;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BrevoEmailResponse {
        @JsonProperty("messageId")
        private String messageId;
    }
}
