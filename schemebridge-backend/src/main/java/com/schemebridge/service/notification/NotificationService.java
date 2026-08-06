package com.schemebridge.service.notification;

import com.schemebridge.entity.User;
import com.schemebridge.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailService emailService;
    private final NotificationTemplateService notificationTemplateService;

    public void sendEmailOtp(String email, String otpCode) {
        NotificationTemplate template = notificationTemplateService.renderTemplate(
                NotificationTemplateType.EMAIL_OTP,
                Map.of("email", email, "otp", otpCode)
        );
        emailService.sendEmail(email, template.getSubject(), template.getHtmlBody(), template.getTextBody());
        log.info("Email OTP notification sent to {}", email);
    }

    // Phone/SMS notifications removed.

    public void sendAccountApproved(User user) {
        if (user == null) {
            return;
        }

        Map<String, String> model = new HashMap<>();
        model.put("fullName", user.getFullName() == null ? "Citizen" : user.getFullName());
        model.put("email", user.getEmail() == null ? "" : user.getEmail());

        NotificationTemplate template = notificationTemplateService.renderTemplate(
                NotificationTemplateType.ACCOUNT_APPROVED,
                model
        );

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            emailService.sendEmail(user.getEmail(), template.getSubject(), template.getHtmlBody(), template.getTextBody());
            log.info("Account approval email sent to {}", user.getEmail());
        }

        // SMS notifications removed; only email notifications are sent.
    }

    public void sendAccountRejected(User user, String reason) {
        if (user == null) {
            return;
        }

        Map<String, String> model = new HashMap<>();
        model.put("fullName", user.getFullName() == null ? "Citizen" : user.getFullName());
        model.put("reason", reason == null ? "Please contact support for assistance." : reason);

        NotificationTemplate template = notificationTemplateService.renderTemplate(
                NotificationTemplateType.ACCOUNT_REJECTED,
                model
        );

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            emailService.sendEmail(user.getEmail(), template.getSubject(), template.getHtmlBody(), template.getTextBody());
            log.info("Account rejection email sent to {}", user.getEmail());
        }

        // SMS notifications removed; only email notifications are sent.
    }
}
