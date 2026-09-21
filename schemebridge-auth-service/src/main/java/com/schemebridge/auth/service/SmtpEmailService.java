package com.schemebridge.auth.service;

import com.schemebridge.auth.exception.EmailDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${otp.log-raw-value:false}")
    private boolean logRawOtp;

    @Value("${mail.from:noreply@schemebridge.gov.in}")
    private String mailFrom;

    @Value("${mail.from-name:SchemeBridge Portal}")
    private String mailFromName;

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

        sendMimeEmail(toEmail, subject, htmlContent, plainText);
    }

    @Override
    public void sendPasswordResetOtp(String toEmail, String recipientName, String rawOtp) {
        String subject = "SchemeBridge — Password Reset Request";
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

        sendMimeEmail(toEmail, subject, htmlContent, plainText);
    }

    private void sendMimeEmail(String toEmail, String subject, String htmlContent, String plainText) {
        String maskedEmail = com.schemebridge.auth.util.LogUtils.maskEmail(toEmail);
        log.info("[EMAIL] Attempting to send OTP email");
        log.info("[EMAIL] SMTP configuration loaded: YES");
        log.info("[EMAIL] Sending email to: {}", maskedEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            try {
                helper.setFrom(mailFrom, mailFromName);
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(mailFrom);
            }

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plainText, htmlContent);

            mailSender.send(message);
            log.info("[EMAIL] Email sent successfully to: {}", maskedEmail);
        } catch (MailException | MessagingException ex) {
            log.error("[EMAIL] Failed to send OTP email: {}", ex.getMessage());
            if (logRawOtp) {
                log.warn("[EMAIL] Development mode active (OTP logged to console). Bypassing fatal delivery failure: {}", ex.getMessage());
                return;
            }
            throw new EmailDeliveryException("Failed to send OTP email: " + ex.getMessage(), ex);
        }
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
}
