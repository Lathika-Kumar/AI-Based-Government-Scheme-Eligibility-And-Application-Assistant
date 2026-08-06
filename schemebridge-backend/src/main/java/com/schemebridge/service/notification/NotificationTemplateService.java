package com.schemebridge.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class NotificationTemplateService {

    public NotificationTemplate renderTemplate(NotificationTemplateType type, Map<String, String> model) {
                return switch (type) {
                        case EMAIL_OTP -> renderEmailOtp(model);
                        case ACCOUNT_APPROVED -> renderAccountApproved(model);
                        case ACCOUNT_REJECTED -> renderAccountRejected(model);
                        case FORGOT_PASSWORD -> renderForgotPassword(model);
                        case RESET_PASSWORD -> renderResetPassword(model);
                };
    }

    private NotificationTemplate renderEmailOtp(Map<String, String> model) {
        String recipient = maskEmail(model.get("email"));
        String otpCode = model.get("otp");
        return NotificationTemplate.builder()
                .subject("Your SchemeBridge Email OTP Code")
                .htmlBody("<p>Dear " + model.getOrDefault("fullName", "Citizen") + ",</p>" +
                        "<p>Your SchemeBridge email verification OTP is <strong>" + otpCode + "</strong>. " +
                        "This code is valid for 5 minutes.</p>" +
                        "<p>If you did not request this, please ignore this message.</p>" +
                        "<p>Thank you,<br/>SchemeBridge Security Team</p>")
                .textBody("Dear " + model.getOrDefault("fullName", "Citizen") + ",\n\n" +
                        "Your SchemeBridge email verification OTP is " + otpCode + ". " +
                        "This code is valid for 5 minutes.\n\n" +
                        "If you did not request this, please ignore this message.\n\n" +
                        "Thank you,\nSchemeBridge Security Team")
                .build();
    }

    // Phone OTP template removed.

    private NotificationTemplate renderAccountApproved(Map<String, String> model) {
        return NotificationTemplate.builder()
                .subject("SchemeBridge Account Approved")
                .htmlBody("<p>Dear " + model.getOrDefault("fullName", "Citizen") + ",</p>" +
                        "<p>Your SchemeBridge account has been approved by the administrator.</p>" +
                        "<p>Next steps:</p>" +
                        "<ul>" +
                        "<li>Login using your registered email.</li>" +
                        "<li>Complete your profile and apply for schemes.</li>" +
                        "<li>For support, visit the SchemeBridge portal.</li>" +
                        "</ul>" +
                        "<p>Login here: <a href=\"https://localhost:5173/login\">https://localhost:5173/login</a></p>" +
                        "<p>Thank you,<br/>SchemeBridge Team</p>")
                .textBody("Dear " + model.getOrDefault("fullName", "Citizen") + ",\n\n" +
                        "Your SchemeBridge account has been approved by the administrator.\n\n" +
                        "Next steps:\n" +
                        "- Login using your registered email.\n" +
                        "- Complete your profile and apply for schemes.\n" +
                        "- For support, visit the SchemeBridge portal.\n\n" +
                        "Login here: https://localhost:5173/login\n\n" +
                        "Thank you,\nSchemeBridge Team")
                .build();
    }

    private NotificationTemplate renderAccountRejected(Map<String, String> model) {
        return NotificationTemplate.builder()
                .subject("SchemeBridge Account Update")
                .htmlBody("<p>Dear " + model.getOrDefault("fullName", "Citizen") + ",</p>" +
                        "<p>Your SchemeBridge account has been reviewed and has not been approved at this time.</p>" +
                        "<p>Reason: " + model.getOrDefault("reason", "Please contact support for more details.") + "</p>" +
                        "<p>Thank you,<br/>SchemeBridge Team</p>")
                .textBody("Dear " + model.getOrDefault("fullName", "Citizen") + ",\n\n" +
                        "Your SchemeBridge account has been reviewed and has not been approved at this time.\n" +
                        "Reason: " + model.getOrDefault("reason", "Please contact support for more details.") + "\n\n" +
                        "Thank you,\nSchemeBridge Team")
                .build();
    }

    private NotificationTemplate renderForgotPassword(Map<String, String> model) {
        return NotificationTemplate.builder()
                .subject("SchemeBridge Password Reset Request")
                .htmlBody("<p>Dear " + model.getOrDefault("fullName", "Citizen") + ",</p>" +
                        "<p>Use the following password reset token: <strong>" + model.getOrDefault("token", "N/A") + "</strong></p>" +
                        "<p>If you did not request this, please ignore this message.</p>" +
                        "<p>Thank you,<br/>SchemeBridge Support Team</p>")
                .textBody("Dear " + model.getOrDefault("fullName", "Citizen") + ",\n\n" +
                        "Use the following password reset token: " + model.getOrDefault("token", "N/A") + "\n\n" +
                        "If you did not request this, please ignore this message.\n\n" +
                        "Thank you,\nSchemeBridge Support Team")
                .build();
    }

    private NotificationTemplate renderResetPassword(Map<String, String> model) {
        return NotificationTemplate.builder()
                .subject("SchemeBridge Password Reset Complete")
                .htmlBody("<p>Dear " + model.getOrDefault("fullName", "Citizen") + ",</p>" +
                        "<p>Your password reset was completed successfully.</p>" +
                        "<p>If you did not perform this action, please contact support immediately.</p>" +
                        "<p>Thank you,<br/>SchemeBridge Support Team</p>")
                .textBody("Dear " + model.getOrDefault("fullName", "Citizen") + ",\n\n" +
                        "Your password reset was completed successfully.\n\n" +
                        "If you did not perform this action, please contact support immediately.\n\n" +
                        "Thank you,\nSchemeBridge Support Team")
                .build();
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "[hidden email]";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex - 1);
    }
}
