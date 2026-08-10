package com.schemebridge.notificationservice.config;

import com.schemebridge.notificationservice.entity.NotificationTemplate;
import com.schemebridge.notificationservice.enums.Channel;
import com.schemebridge.notificationservice.enums.NotificationType;
import com.schemebridge.notificationservice.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final NotificationTemplateRepository templateRepository;

    @Override
    public void run(String... args) {
        log.info("Seeding default Notification Templates into MongoDB...");

        List<NotificationTemplate> seedTemplates = List.of(
                NotificationTemplate.builder()
                        .templateId("TMPL-WELCOME-EMAIL")
                        .templateName("Welcome Email Template")
                        .notificationType(NotificationType.GENERAL)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("Welcome to SchemeBridge, {{fullName}}!")
                        .titleTemplate("Account Successfully Created")
                        .bodyTemplate("Hello {{fullName}},\n\nWelcome to SchemeBridge! Your account is active.")
                        .htmlTemplate("<h2>Welcome {{fullName}}!</h2><p>Your SchemeBridge account has been set up.</p>")
                        .defaultVariables(Map.of("fullName", "Citizen"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-OTP-EMAIL")
                        .templateName("OTP Verification Email Template")
                        .notificationType(NotificationType.OTP)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("Your SchemeBridge Verification Code: {{otpCode}}")
                        .titleTemplate("OTP Verification Code")
                        .bodyTemplate("Hello {{fullName}},\n\nYour OTP is {{otpCode}}. Valid for 10 minutes.")
                        .htmlTemplate("<h3>Verification Code</h3><p>Your OTP code is <b>{{otpCode}}</b>.</p>")
                        .defaultVariables(Map.of("fullName", "User", "otpCode", "123456"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-APP-APPROVED-EMAIL")
                        .templateName("Application Approved Email Template")
                        .notificationType(NotificationType.APPLICATION_STATUS)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("Application Approved: {{schemeName}}")
                        .titleTemplate("Application Approved")
                        .bodyTemplate("Hello {{fullName}},\n\nYour application {{applicationNumber}} for {{schemeName}} has been APPROVED! Benefit: {{benefitAmount}}.")
                        .htmlTemplate("<h2>Congratulations {{fullName}}!</h2><p>Your application <b>{{applicationNumber}}</b> for <i>{{schemeName}}</i> is <b>APPROVED</b>.</p><p>Benefit: {{benefitAmount}}</p>")
                        .defaultVariables(Map.of("fullName", "Applicant", "applicationNumber", "APP-1001", "schemeName", "PM-KISAN", "benefitAmount", "₹6,000/year"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-DOC-VERIFIED-INAPP")
                        .templateName("Document Verified In-App Template")
                        .notificationType(NotificationType.DOCUMENT_VERIFIED)
                        .channel(Channel.IN_APP)
                        .subjectTemplate("Document Verified: {{documentName}}")
                        .titleTemplate("Document Verified")
                        .bodyTemplate("Your {{documentName}} ({{documentType}}) has been verified successfully.")
                        .htmlTemplate("<p>Your document <b>{{documentName}}</b> is verified.</p>")
                        .defaultVariables(Map.of("documentName", "Aadhaar Card", "documentType", "Identity Proof"))
                        .build()
        );

        for (NotificationTemplate tmpl : seedTemplates) {
            if (!templateRepository.existsByTemplateId(tmpl.getTemplateId())) {
                templateRepository.save(tmpl);
                log.info("Seeded template: {}", tmpl.getTemplateId());
            }
        }
        log.info("Notification Template Seeding complete.");
    }
}
