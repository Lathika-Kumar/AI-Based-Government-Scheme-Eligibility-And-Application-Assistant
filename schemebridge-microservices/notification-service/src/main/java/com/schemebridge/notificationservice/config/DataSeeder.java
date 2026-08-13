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
                // General & System
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

                // 1. Scheme Created
                NotificationTemplate.builder()
                        .templateId("TMPL-SCHEME-CREATED-EMAIL")
                        .templateName("New Scheme Email Template")
                        .notificationType(NotificationType.SCHEME_CREATED)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("New Government Scheme Launched: {{schemeName}}")
                        .titleTemplate("New Scheme Announcement")
                        .bodyTemplate("Hello Citizen,\n\nA new government scheme '{{schemeName}}' under {{category}} has been launched. Check your eligibility now.")
                        .htmlTemplate("<h2>New Scheme Launched!</h2><p><b>{{schemeName}}</b> (Category: {{category}}) is now open for applications.</p>")
                        .defaultVariables(Map.of("schemeName", "Government Scheme", "category", "Welfare"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-SCHEME-CREATED-INAPP")
                        .templateName("New Scheme In-App Template")
                        .notificationType(NotificationType.SCHEME_CREATED)
                        .channel(Channel.IN_APP)
                        .subjectTemplate("New Scheme Available: {{schemeName}}")
                        .titleTemplate("New Scheme Launched")
                        .bodyTemplate("New scheme '{{schemeName}}' is available. Apply now if eligible.")
                        .htmlTemplate("<p>New scheme <b>{{schemeName}}</b> launched.</p>")
                        .defaultVariables(Map.of("schemeName", "Government Scheme"))
                        .build(),

                // 2. Scheme Updated
                NotificationTemplate.builder()
                        .templateId("TMPL-SCHEME-UPDATED-EMAIL")
                        .templateName("Scheme Update Email Template")
                        .notificationType(NotificationType.SCHEME_UPDATED)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("Update on Scheme: {{schemeName}}")
                        .titleTemplate("Scheme Details Updated")
                        .bodyTemplate("Hello Citizen,\n\nDetails for scheme '{{schemeName}}' have been updated:\n{{updateSummary}}")
                        .htmlTemplate("<h3>Scheme Update</h3><p>Scheme <b>{{schemeName}}</b> updated: {{updateSummary}}</p>")
                        .defaultVariables(Map.of("schemeName", "Government Scheme", "updateSummary", "Updated guidelines"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-SCHEME-UPDATED-INAPP")
                        .templateName("Scheme Update In-App Template")
                        .notificationType(NotificationType.SCHEME_UPDATED)
                        .channel(Channel.IN_APP)
                        .subjectTemplate("Scheme Updated: {{schemeName}}")
                        .titleTemplate("Scheme Updated")
                        .bodyTemplate("Scheme '{{schemeName}}' has been updated: {{updateSummary}}")
                        .htmlTemplate("<p>Scheme <b>{{schemeName}}</b> updated.</p>")
                        .defaultVariables(Map.of("schemeName", "Government Scheme", "updateSummary", "Updated guidelines"))
                        .build(),

                // 4. Scheme Deadline
                NotificationTemplate.builder()
                        .templateId("TMPL-SCHEME-DEADLINE-EMAIL")
                        .templateName("Scheme Deadline Alert Email")
                        .notificationType(NotificationType.SCHEME_DEADLINE_UPDATE)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("Important Deadline Alert: {{schemeName}}")
                        .titleTemplate("Application Deadline Approaching")
                        .bodyTemplate("Hello Citizen,\n\nApplication deadline for '{{schemeName}}' is {{deadlineDate}}. Submit your application before the cutoff.")
                        .htmlTemplate("<h3>Deadline Alert</h3><p>The deadline for <b>{{schemeName}}</b> is <b>{{deadlineDate}}</b>.</p>")
                        .defaultVariables(Map.of("schemeName", "Government Scheme", "deadlineDate", "31st Dec 2026"))
                        .build(),

                // 5. Application Submitted
                NotificationTemplate.builder()
                        .templateId("TMPL-APP-SUBMITTED-EMAIL")
                        .templateName("Application Submitted Email")
                        .notificationType(NotificationType.APPLICATION_SUBMITTED)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("Application Received: {{applicationNumber}}")
                        .titleTemplate("Application Submitted Successfully")
                        .bodyTemplate("Hello Citizen,\n\nYour application {{applicationNumber}} for '{{schemeName}}' has been submitted successfully.")
                        .htmlTemplate("<h2>Application Received</h2><p>Application <b>{{applicationNumber}}</b> for <i>{{schemeName}}</i> received successfully.</p>")
                        .defaultVariables(Map.of("applicationNumber", "APP-1001", "schemeName", "Welfare Scheme"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-APP-SUBMITTED-SMS")
                        .templateName("Application Submitted SMS")
                        .notificationType(NotificationType.APPLICATION_SUBMITTED)
                        .channel(Channel.SMS)
                        .subjectTemplate("Application {{applicationNumber}} Submitted")
                        .titleTemplate("Application Submitted")
                        .bodyTemplate("SchemeBridge: Application {{applicationNumber}} for {{schemeName}} submitted successfully.")
                        .htmlTemplate("<p>Application {{applicationNumber}} submitted.</p>")
                        .defaultVariables(Map.of("applicationNumber", "APP-1001", "schemeName", "Welfare Scheme"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-APP-SUBMITTED-INAPP")
                        .templateName("Application Submitted In-App")
                        .notificationType(NotificationType.APPLICATION_SUBMITTED)
                        .channel(Channel.IN_APP)
                        .subjectTemplate("Application Submitted: {{applicationNumber}}")
                        .titleTemplate("Application Submitted")
                        .bodyTemplate("Your application {{applicationNumber}} for {{schemeName}} is currently pending review.")
                        .htmlTemplate("<p>Application <b>{{applicationNumber}}</b> submitted.</p>")
                        .defaultVariables(Map.of("applicationNumber", "APP-1001", "schemeName", "Welfare Scheme"))
                        .build(),

                // 6. & 14. Application Status Changed
                NotificationTemplate.builder()
                        .templateId("TMPL-APP-STATUS-EMAIL")
                        .templateName("Application Status Change Email")
                        .notificationType(NotificationType.APPLICATION_STATUS)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("Status Update for Application: {{applicationNumber}}")
                        .titleTemplate("Application Status Changed")
                        .bodyTemplate("Hello Citizen,\n\nYour application {{applicationNumber}} status has changed to: {{status}}. Remarks: {{remarks}}")
                        .htmlTemplate("<h3>Status Update</h3><p>Application <b>{{applicationNumber}}</b> is now <b>{{status}}</b>. Remarks: {{remarks}}</p>")
                        .defaultVariables(Map.of("applicationNumber", "APP-1001", "status", "UNDER_REVIEW", "remarks", "Document verification in progress"))
                        .build(),

                // 7. Application Approved
                NotificationTemplate.builder()
                        .templateId("TMPL-APP-APPROVED-EMAIL")
                        .templateName("Application Approved Email Template")
                        .notificationType(NotificationType.APPLICATION_APPROVED)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("Congratulations! Application Approved: {{schemeName}}")
                        .titleTemplate("Application Approved")
                        .bodyTemplate("Hello Citizen,\n\nYour application {{applicationNumber}} for {{schemeName}} has been APPROVED!\nBenefit: {{benefitDetails}}.")
                        .htmlTemplate("<h2>Congratulations!</h2><p>Your application <b>{{applicationNumber}}</b> for <i>{{schemeName}}</i> is <b>APPROVED</b>.</p><p>Benefit Details: {{benefitDetails}}</p>")
                        .defaultVariables(Map.of("applicationNumber", "APP-1001", "schemeName", "PM-KISAN", "benefitDetails", "₹6,000/year"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-APP-APPROVED-SMS")
                        .templateName("Application Approved SMS")
                        .notificationType(NotificationType.APPLICATION_APPROVED)
                        .channel(Channel.SMS)
                        .subjectTemplate("Application {{applicationNumber}} Approved")
                        .titleTemplate("Application Approved")
                        .bodyTemplate("SchemeBridge: Congratulations! Your application {{applicationNumber}} for {{schemeName}} is APPROVED. Benefit: {{benefitDetails}}")
                        .htmlTemplate("<p>Application {{applicationNumber}} approved.</p>")
                        .defaultVariables(Map.of("applicationNumber", "APP-1001", "schemeName", "PM-KISAN", "benefitDetails", "Direct Benefit Transfer"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-APP-APPROVED-INAPP")
                        .templateName("Application Approved In-App")
                        .notificationType(NotificationType.APPLICATION_APPROVED)
                        .channel(Channel.IN_APP)
                        .subjectTemplate("Application Approved: {{applicationNumber}}")
                        .titleTemplate("Application Approved")
                        .bodyTemplate("Great news! Your application {{applicationNumber}} for {{schemeName}} has been approved.")
                        .htmlTemplate("<p>Application <b>{{applicationNumber}}</b> is APPROVED.</p>")
                        .defaultVariables(Map.of("applicationNumber", "APP-1001", "schemeName", "PM-KISAN"))
                        .build(),

                // 8. Application Rejected
                NotificationTemplate.builder()
                        .templateId("TMPL-APP-REJECTED-EMAIL")
                        .templateName("Application Rejected Email")
                        .notificationType(NotificationType.APPLICATION_REJECTED)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("Update on Application {{applicationNumber}}: REJECTED")
                        .titleTemplate("Application Rejected")
                        .bodyTemplate("Hello Citizen,\n\nRegrettably, your application {{applicationNumber}} for {{schemeName}} was REJECTED.\nReason: {{rejectionReason}}")
                        .htmlTemplate("<h3>Application Status Update</h3><p>Your application <b>{{applicationNumber}}</b> for <i>{{schemeName}}</i> was <b>REJECTED</b>.</p><p>Reason: {{rejectionReason}}</p>")
                        .defaultVariables(Map.of("applicationNumber", "APP-1001", "schemeName", "Welfare Scheme", "rejectionReason", "Eligibility income threshold exceeded"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-APP-REJECTED-SMS")
                        .templateName("Application Rejected SMS")
                        .notificationType(NotificationType.APPLICATION_REJECTED)
                        .channel(Channel.SMS)
                        .subjectTemplate("Application {{applicationNumber}} Rejected")
                        .titleTemplate("Application Rejected")
                        .bodyTemplate("SchemeBridge: Application {{applicationNumber}} for {{schemeName}} was REJECTED. Reason: {{rejectionReason}}")
                        .htmlTemplate("<p>Application {{applicationNumber}} rejected.</p>")
                        .defaultVariables(Map.of("applicationNumber", "APP-1001", "schemeName", "Welfare Scheme", "rejectionReason", "Criteria not met"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-APP-REJECTED-INAPP")
                        .templateName("Application Rejected In-App")
                        .notificationType(NotificationType.APPLICATION_REJECTED)
                        .channel(Channel.IN_APP)
                        .subjectTemplate("Application Rejected: {{applicationNumber}}")
                        .titleTemplate("Application Rejected")
                        .bodyTemplate("Your application {{applicationNumber}} for {{schemeName}} was rejected. Reason: {{rejectionReason}}")
                        .htmlTemplate("<p>Application <b>{{applicationNumber}}</b> rejected.</p>")
                        .defaultVariables(Map.of("applicationNumber", "APP-1001", "schemeName", "Welfare Scheme", "rejectionReason", "Incomplete criteria"))
                        .build(),

                // 9. Document Verified
                NotificationTemplate.builder()
                        .templateId("TMPL-DOC-VERIFIED-EMAIL")
                        .templateName("Document Verified Email")
                        .notificationType(NotificationType.DOCUMENT_VERIFIED)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("Document Verified: {{documentName}}")
                        .titleTemplate("Document Verification Successful")
                        .bodyTemplate("Hello Citizen,\n\nYour uploaded document {{documentName}} ({{documentType}}) has been successfully verified.")
                        .htmlTemplate("<h3>Document Verified</h3><p>Document <b>{{documentName}}</b> ({{documentType}}) is verified successfully.</p>")
                        .defaultVariables(Map.of("documentName", "Aadhaar Card", "documentType", "IDENTITY"))
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
                        .build(),

                // 10. Document Rejected
                NotificationTemplate.builder()
                        .templateId("TMPL-DOC-REJECTED-EMAIL")
                        .templateName("Document Rejected Email")
                        .notificationType(NotificationType.DOCUMENT_REJECTED)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("Action Required: Document Rejected ({{documentName}})")
                        .titleTemplate("Document Verification Rejected")
                        .bodyTemplate("Hello Citizen,\n\nYour uploaded document {{documentName}} ({{documentType}}) was REJECTED.\nReason: {{rejectionReason}}\nPlease re-upload a clear copy.")
                        .htmlTemplate("<h3>Document Verification Failed</h3><p>Your document <b>{{documentName}}</b> was <b>REJECTED</b>.</p><p>Reason: {{rejectionReason}}</p><p>Please upload a valid copy.</p>")
                        .defaultVariables(Map.of("documentName", "Income Certificate", "documentType", "INCOME_PROOF", "rejectionReason", "Expired certificate"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-DOC-REJECTED-SMS")
                        .templateName("Document Rejected SMS")
                        .notificationType(NotificationType.DOCUMENT_REJECTED)
                        .channel(Channel.SMS)
                        .subjectTemplate("Document Rejected: {{documentName}}")
                        .titleTemplate("Document Rejected")
                        .bodyTemplate("SchemeBridge: Your document {{documentName}} was rejected. Reason: {{rejectionReason}}. Please re-upload.")
                        .htmlTemplate("<p>Document {{documentName}} rejected.</p>")
                        .defaultVariables(Map.of("documentName", "Income Certificate", "rejectionReason", "Illegible text"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-DOC-REJECTED-INAPP")
                        .templateName("Document Rejected In-App")
                        .notificationType(NotificationType.DOCUMENT_REJECTED)
                        .channel(Channel.IN_APP)
                        .subjectTemplate("Document Rejected: {{documentName}}")
                        .titleTemplate("Document Rejected")
                        .bodyTemplate("Your document {{documentName}} was rejected: {{rejectionReason}}. Re-upload required.")
                        .htmlTemplate("<p>Document <b>{{documentName}}</b> rejected.</p>")
                        .defaultVariables(Map.of("documentName", "Income Certificate", "rejectionReason", "Illegible image"))
                        .build(),

                // 11. Document Required
                NotificationTemplate.builder()
                        .templateId("TMPL-DOC-REQUIRED-EMAIL")
                        .templateName("Missing Document Reminder Email")
                        .notificationType(NotificationType.DOCUMENT_REQUIRED)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("Action Required: Missing Document for Application")
                        .titleTemplate("Required Document Missing")
                        .bodyTemplate("Hello Citizen,\n\nPlease upload the required document type '{{documentType}}' for application processing.\nRemarks: {{remarks}}")
                        .htmlTemplate("<h3>Missing Document Action Required</h3><p>Please upload document type: <b>{{documentType}}</b>.</p><p>Remarks: {{remarks}}</p>")
                        .defaultVariables(Map.of("documentType", "RATION_CARD", "remarks", "Verification pending document upload"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-DOC-REQUIRED-INAPP")
                        .templateName("Missing Document In-App")
                        .notificationType(NotificationType.DOCUMENT_REQUIRED)
                        .channel(Channel.IN_APP)
                        .subjectTemplate("Missing Document: {{documentType}}")
                        .titleTemplate("Required Document Pending")
                        .bodyTemplate("Document upload required: {{documentType}}. Remarks: {{remarks}}")
                        .htmlTemplate("<p>Upload required document <b>{{documentType}}</b>.</p>")
                        .defaultVariables(Map.of("documentType", "RATION_CARD", "remarks", "Upload clear copy"))
                        .build(),

                // 12. Announcement Published
                NotificationTemplate.builder()
                        .templateId("TMPL-ANNOUNCEMENT-EMAIL")
                        .templateName("Announcement Broadcast Email")
                        .notificationType(NotificationType.ANNOUNCEMENT)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("Government Announcement: {{title}}")
                        .titleTemplate("Official Announcement")
                        .bodyTemplate("Hello Citizen,\n\n{{title}}\n\n{{content}}")
                        .htmlTemplate("<h2>Official Announcement</h2><h3>{{title}}</h3><p>{{content}}</p>")
                        .defaultVariables(Map.of("title", "Portal Maintenance", "content", "System will undergo scheduled upgrade."))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-ANNOUNCEMENT-INAPP")
                        .templateName("Announcement In-App")
                        .notificationType(NotificationType.ANNOUNCEMENT)
                        .channel(Channel.IN_APP)
                        .subjectTemplate("Announcement: {{title}}")
                        .titleTemplate("Government Announcement")
                        .bodyTemplate("{{title}}: {{content}}")
                        .htmlTemplate("<p><b>{{title}}</b>: {{content}}</p>")
                        .defaultVariables(Map.of("title", "Portal Update", "content", "New scheme features added."))
                        .build(),

                // 13. Grievance Status Updated
                NotificationTemplate.builder()
                        .templateId("TMPL-GRIEVANCE-EMAIL")
                        .templateName("Grievance Status Update Email")
                        .notificationType(NotificationType.GRIEVANCE_UPDATED)
                        .channel(Channel.EMAIL)
                        .subjectTemplate("Grievance {{grievanceId}} Resolution Update")
                        .titleTemplate("Grievance Status Update")
                        .bodyTemplate("Hello Citizen,\n\nYour grievance {{grievanceId}} status is now: {{status}}.\nResolution: {{resolution}}")
                        .htmlTemplate("<h3>Grievance Status Update</h3><p>Grievance <b>{{grievanceId}}</b> status: <b>{{status}}</b></p><p>Resolution: {{resolution}}</p>")
                        .defaultVariables(Map.of("grievanceId", "GRV-5001", "status", "RESOLVED", "resolution", "Issue resolved by Officer"))
                        .build(),

                NotificationTemplate.builder()
                        .templateId("TMPL-GRIEVANCE-INAPP")
                        .templateName("Grievance In-App")
                        .notificationType(NotificationType.GRIEVANCE_UPDATED)
                        .channel(Channel.IN_APP)
                        .subjectTemplate("Grievance {{grievanceId}} {{status}}")
                        .titleTemplate("Grievance Update")
                        .bodyTemplate("Grievance {{grievanceId}} updated to {{status}}. Resolution: {{resolution}}")
                        .htmlTemplate("<p>Grievance <b>{{grievanceId}}</b> is {{status}}.</p>")
                        .defaultVariables(Map.of("grievanceId", "GRV-5001", "status", "RESOLVED", "resolution", "Resolved"))
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
