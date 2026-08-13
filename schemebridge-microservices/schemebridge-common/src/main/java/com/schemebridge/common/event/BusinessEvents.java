package com.schemebridge.common.event;

import java.util.List;
import java.util.Map;

/**
 * Factory class for constructing strongly-typed DomainEvent instances
 * for all SchemeBridge business events.
 */
public class BusinessEvents {

    // Event Type Constants
    public static final String SCHEME_CREATED = "SCHEME_CREATED";
    public static final String SCHEME_UPDATED = "SCHEME_UPDATED";
    public static final String SCHEME_ELIGIBILITY_UPDATED = "SCHEME_ELIGIBILITY_UPDATED";
    public static final String SCHEME_DEADLINE_UPDATE = "SCHEME_DEADLINE_UPDATE";
    public static final String APPLICATION_SUBMITTED = "APPLICATION_SUBMITTED";
    public static final String APPLICATION_STATUS_CHANGED = "APPLICATION_STATUS_CHANGED";
    public static final String APPLICATION_APPROVED = "APPLICATION_APPROVED";
    public static final String APPLICATION_REJECTED = "APPLICATION_REJECTED";
    public static final String DOCUMENT_VERIFIED = "DOCUMENT_VERIFIED";
    public static final String DOCUMENT_REJECTED = "DOCUMENT_REJECTED";
    public static final String DOCUMENT_REQUIRED = "DOCUMENT_REQUIRED";
    public static final String ANNOUNCEMENT_PUBLISHED = "ANNOUNCEMENT_PUBLISHED";
    public static final String GRIEVANCE_STATUS_UPDATED = "GRIEVANCE_STATUS_UPDATED";

    // Source Modules
    public static final String MODULE_CORE_SCHEME = "CORE_SCHEME";
    public static final String MODULE_CORE_APPLICATION = "CORE_APPLICATION";
    public static final String MODULE_CORE_DOCUMENT = "CORE_DOCUMENT";
    public static final String MODULE_ADMIN_ANNOUNCEMENT = "ADMIN_ANNOUNCEMENT";
    public static final String MODULE_ADMIN_GRIEVANCE = "ADMIN_GRIEVANCE";

    // 1. Admin creates a new scheme
    public static DomainEvent createSchemeCreatedEvent(String schemeId, String schemeCode, String schemeName, String category, List<String> targetAuthUserIds) {
        return DomainEvent.builder()
                .eventType(SCHEME_CREATED)
                .sourceModule(MODULE_CORE_SCHEME)
                .targetAuthUserIds(targetAuthUserIds)
                .targetAudience("ELIGIBLE_CITIZENS")
                .payload(Map.of(
                        "schemeId", schemeId != null ? schemeId : "",
                        "schemeCode", schemeCode != null ? schemeCode : "",
                        "schemeName", schemeName != null ? schemeName : "",
                        "category", category != null ? category : ""
                ))
                .build();
    }

    // 2. Admin updates scheme details
    public static DomainEvent createSchemeUpdatedEvent(String schemeId, String schemeCode, String schemeName, String updateSummary, List<String> targetAuthUserIds) {
        return DomainEvent.builder()
                .eventType(SCHEME_UPDATED)
                .sourceModule(MODULE_CORE_SCHEME)
                .targetAuthUserIds(targetAuthUserIds)
                .targetAudience("ELIGIBLE_CITIZENS")
                .payload(Map.of(
                        "schemeId", schemeId != null ? schemeId : "",
                        "schemeCode", schemeCode != null ? schemeCode : "",
                        "schemeName", schemeName != null ? schemeName : "",
                        "updateSummary", updateSummary != null ? updateSummary : "Scheme details have been updated."
                ))
                .build();
    }

    // 3. Admin changes scheme eligibility criteria
    public static DomainEvent createSchemeEligibilityUpdatedEvent(String schemeId, String schemeName, String newCriteriaSummary, List<String> targetAuthUserIds) {
        return DomainEvent.builder()
                .eventType(SCHEME_ELIGIBILITY_UPDATED)
                .sourceModule(MODULE_CORE_SCHEME)
                .targetAuthUserIds(targetAuthUserIds)
                .targetAudience("ELIGIBLE_CITIZENS")
                .payload(Map.of(
                        "schemeId", schemeId != null ? schemeId : "",
                        "schemeName", schemeName != null ? schemeName : "",
                        "criteriaSummary", newCriteriaSummary != null ? newCriteriaSummary : "Eligibility criteria updated."
                ))
                .build();
    }

    // 4. Scheme deadline/update
    public static DomainEvent createSchemeDeadlineUpdateEvent(String schemeId, String schemeName, String deadlineDate, List<String> targetAuthUserIds) {
        return DomainEvent.builder()
                .eventType(SCHEME_DEADLINE_UPDATE)
                .sourceModule(MODULE_CORE_SCHEME)
                .targetAuthUserIds(targetAuthUserIds)
                .targetAudience("ELIGIBLE_CITIZENS")
                .payload(Map.of(
                        "schemeId", schemeId != null ? schemeId : "",
                        "schemeName", schemeName != null ? schemeName : "",
                        "deadlineDate", deadlineDate != null ? deadlineDate : ""
                ))
                .build();
    }

    // 5. Citizen submits an application
    public static DomainEvent createApplicationSubmittedEvent(String authUserId, String applicationId, String applicationNumber, String schemeName) {
        return DomainEvent.builder()
                .eventType(APPLICATION_SUBMITTED)
                .sourceModule(MODULE_CORE_APPLICATION)
                .authUserId(authUserId)
                .payload(Map.of(
                        "applicationId", applicationId != null ? applicationId : "",
                        "applicationNumber", applicationNumber != null ? applicationNumber : "",
                        "schemeName", schemeName != null ? schemeName : ""
                ))
                .build();
    }

    // 6. Application status changes to UNDER_REVIEW or 14. any general status change
    public static DomainEvent createApplicationStatusChangedEvent(String authUserId, String applicationId, String applicationNumber, String schemeName, String newStatus, String remarks) {
        return DomainEvent.builder()
                .eventType(APPLICATION_STATUS_CHANGED)
                .sourceModule(MODULE_CORE_APPLICATION)
                .authUserId(authUserId)
                .payload(Map.of(
                        "applicationId", applicationId != null ? applicationId : "",
                        "applicationNumber", applicationNumber != null ? applicationNumber : "",
                        "schemeName", schemeName != null ? schemeName : "",
                        "status", newStatus != null ? newStatus : "",
                        "remarks", remarks != null ? remarks : ""
                ))
                .build();
    }

    // 7. Application is APPROVED
    public static DomainEvent createApplicationApprovedEvent(String authUserId, String applicationId, String applicationNumber, String schemeName, String benefitDetails) {
        return DomainEvent.builder()
                .eventType(APPLICATION_APPROVED)
                .sourceModule(MODULE_CORE_APPLICATION)
                .authUserId(authUserId)
                .payload(Map.of(
                        "applicationId", applicationId != null ? applicationId : "",
                        "applicationNumber", applicationNumber != null ? applicationNumber : "",
                        "schemeName", schemeName != null ? schemeName : "",
                        "status", "APPROVED",
                        "benefitDetails", benefitDetails != null ? benefitDetails : "Benefit details attached"
                ))
                .build();
    }

    // 8. Application is REJECTED
    public static DomainEvent createApplicationRejectedEvent(String authUserId, String applicationId, String applicationNumber, String schemeName, String rejectionReason) {
        return DomainEvent.builder()
                .eventType(APPLICATION_REJECTED)
                .sourceModule(MODULE_CORE_APPLICATION)
                .authUserId(authUserId)
                .payload(Map.of(
                        "applicationId", applicationId != null ? applicationId : "",
                        "applicationNumber", applicationNumber != null ? applicationNumber : "",
                        "schemeName", schemeName != null ? schemeName : "",
                        "status", "REJECTED",
                        "rejectionReason", rejectionReason != null ? rejectionReason : "Application did not meet requirements"
                ))
                .build();
    }

    // 9. Document is VERIFIED
    public static DomainEvent createDocumentVerifiedEvent(String authUserId, String documentId, String documentName, String documentType, String applicationId) {
        return DomainEvent.builder()
                .eventType(DOCUMENT_VERIFIED)
                .sourceModule(MODULE_CORE_DOCUMENT)
                .authUserId(authUserId)
                .payload(Map.of(
                        "documentId", documentId != null ? documentId : "",
                        "documentName", documentName != null ? documentName : "Document",
                        "documentType", documentType != null ? documentType : "IDENTITY",
                        "applicationId", applicationId != null ? applicationId : ""
                ))
                .build();
    }

    // 10. Document is REJECTED
    public static DomainEvent createDocumentRejectedEvent(String authUserId, String documentId, String documentName, String documentType, String rejectionReason, String applicationId) {
        return DomainEvent.builder()
                .eventType(DOCUMENT_REJECTED)
                .sourceModule(MODULE_CORE_DOCUMENT)
                .authUserId(authUserId)
                .payload(Map.of(
                        "documentId", documentId != null ? documentId : "",
                        "documentName", documentName != null ? documentName : "Document",
                        "documentType", documentType != null ? documentType : "IDENTITY",
                        "rejectionReason", rejectionReason != null ? rejectionReason : "Document image unclear",
                        "applicationId", applicationId != null ? applicationId : ""
                ))
                .build();
    }

    // 11. Missing/required document notification
    public static DomainEvent createDocumentRequiredEvent(String authUserId, String documentType, String applicationId, String remarks) {
        return DomainEvent.builder()
                .eventType(DOCUMENT_REQUIRED)
                .sourceModule(MODULE_CORE_DOCUMENT)
                .authUserId(authUserId)
                .payload(Map.of(
                        "documentType", documentType != null ? documentType : "REQUIRED_DOCUMENT",
                        "applicationId", applicationId != null ? applicationId : "",
                        "remarks", remarks != null ? remarks : "Please upload required document"
                ))
                .build();
    }

    // 12. Admin publishes a citizen-facing announcement
    public static DomainEvent createAnnouncementPublishedEvent(String announcementId, String title, String content, String targetAudience, List<String> targetAuthUserIds) {
        return DomainEvent.builder()
                .eventType(ANNOUNCEMENT_PUBLISHED)
                .sourceModule(MODULE_ADMIN_ANNOUNCEMENT)
                .targetAuthUserIds(targetAuthUserIds)
                .targetAudience(targetAudience != null ? targetAudience : "ALL_USERS")
                .payload(Map.of(
                        "announcementId", announcementId != null ? announcementId : "",
                        "title", title != null ? title : "",
                        "content", content != null ? content : ""
                ))
                .build();
    }

    // 13. Grievance status/resolution update
    public static DomainEvent createGrievanceStatusUpdatedEvent(String authUserId, String grievanceId, String status, String resolution) {
        return DomainEvent.builder()
                .eventType(GRIEVANCE_STATUS_UPDATED)
                .sourceModule(MODULE_ADMIN_GRIEVANCE)
                .authUserId(authUserId)
                .payload(Map.of(
                        "grievanceId", grievanceId != null ? grievanceId : "",
                        "status", status != null ? status : "",
                        "resolution", resolution != null ? resolution : ""
                ))
                .build();
    }
}
