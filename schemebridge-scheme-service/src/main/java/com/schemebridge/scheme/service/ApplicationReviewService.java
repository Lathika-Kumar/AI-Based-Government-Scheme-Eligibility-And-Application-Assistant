package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.ApplicationResponse;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResponse;
import com.schemebridge.scheme.document.EvaluationStatus;
import com.schemebridge.scheme.dto.response.PagedApplicationResponse;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.repository.ApplicationDocumentRepository;
import com.schemebridge.scheme.repository.ApplicationRepository;
import com.schemebridge.scheme.repository.ApplicationReviewRepository;
import com.schemebridge.scheme.repository.DocumentOcrResultRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationReviewService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final ApplicationReviewRepository applicationReviewRepository;
    private final SchemeRepository schemeRepository;
    private final ApplicationEventService applicationEventService;
    private final ApplicationStatusTransitionService applicationStatusTransitionService;
    private final DetailedDocumentStatusTransitionService detailedDocumentStatusTransitionService;
    private final ApplicationService applicationService;
    private final MongoOperations mongoOperations;
    private final NotificationService notificationService;
    private final AdminAuditService adminAuditService;

    @Autowired(required = false)
    private CitizenProfileService citizenProfileService;

    @Autowired(required = false)
    private DocumentOcrResultRepository ocrResultRepository;

    @Autowired(required = false)
    private EligibilityEngine eligibilityEngine;

    @Transactional(readOnly = true)
    public PagedApplicationResponse getApplicationsQueue(
            String status,
            String schemeCode,
            int page,
            int size,
            String sort,
            String direction
    ) {
        log.info("Fetching application review queue: status={}, schemeCode={}, page={}, size={}", status, schemeCode, page, size);

        Query query = new Query();

        if (StringUtils.hasText(status)) {
            query.addCriteria(Criteria.where("status").is(ApplicationStatus.valueOf(status.toUpperCase())));
        }
        if (StringUtils.hasText(schemeCode)) {
            query.addCriteria(Criteria.where("schemeCode").is(schemeCode));
        }

        long totalElements = mongoOperations.count(query, Application.class);

        Sort.Direction dir = Sort.Direction.fromString(direction != null ? direction : "DESC");
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort != null ? sort : "createdAt"));
        query.with(pageable);

        List<Application> apps = mongoOperations.find(query, Application.class);
        List<ApplicationResponse> content = apps.stream()
                .map(applicationService::mapToResponse)
                .collect(Collectors.toList());

        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        return PagedApplicationResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    @Transactional
    public ApplicationResponse startReview(String applicationId, String reviewerId, String reviewerRole) {
        log.info("Starting review for application: {}, reviewer: {}", applicationId, reviewerId);

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        ApplicationStatus oldStatus = app.getStatus();
        if (!applicationStatusTransitionService.isValidTransition(oldStatus, ApplicationStatus.UNDER_REVIEW)) {
            throw new IllegalStateException("Cannot transition application from " + oldStatus + " to UNDER_REVIEW.");
        }

        app.setStatus(ApplicationStatus.UNDER_REVIEW);
        app.setUpdatedAt(Instant.now());
        Application savedApp = applicationRepository.save(app);

        // Upsert review record
        ApplicationReview review = applicationReviewRepository.findByApplicationId(applicationId)
                .orElse(new ApplicationReview());
        
        review.setApplicationId(applicationId);
        review.setReviewerId(reviewerId);
        review.setReviewerRole(reviewerRole);
        review.setReviewStatus(ApplicationStatus.UNDER_REVIEW.name());
        review.setCreatedAt(review.getCreatedAt() != null ? review.getCreatedAt() : Instant.now());
        review.setUpdatedAt(Instant.now());
        applicationReviewRepository.save(review);

        // Record UNDER_REVIEW timeline event
        applicationEventService.recordEvent(
                applicationId,
                app.getUserId(),
                ApplicationEventType.UNDER_REVIEW,
                oldStatus,
                ApplicationStatus.UNDER_REVIEW,
                "Application review started by administrative officer.",
                Map.of("reviewerId", reviewerId)
        );

        // Notify Citizen
        notificationService.sendNotification(
                app.getUserId(),
                null,
                NotificationType.APPLICATION_UNDER_REVIEW,
                "Application Under Review: " + app.getApplicationNumber(),
                "Your application " + app.getApplicationNumber() + " is now under active review.",
                "IN_APP",
                "APPLICATION",
                applicationId,
                reviewerId,
                Map.of("applicationNumber", app.getApplicationNumber())
        );

        // Record Admin Audit Log
        adminAuditService.recordAction(
                reviewerId,
                reviewerRole,
                "APPLICATION_REVIEW_STARTED",
                "APPLICATION",
                applicationId,
                Map.of("status", oldStatus.name()),
                Map.of("status", "UNDER_REVIEW"),
                null,
                null,
                Map.of("applicationNumber", app.getApplicationNumber())
        );

        return applicationService.mapToResponse(savedApp);
    }

    @Transactional
    public ApplicationResponse verifyDocument(String applicationId, String documentCode, String reviewerId) {
        log.info("Verifying document: application={}, code={}, reviewer={}", applicationId, documentCode, reviewerId);

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Application must be in UNDER_REVIEW state to verify documents.");
        }

        ApplicationDocument doc = applicationDocumentRepository.findByApplicationIdAndDocumentCode(applicationId, documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found for code: " + documentCode));

        if (!doc.isUploaded()) {
            throw new IllegalStateException("Cannot verify an unuploaded document.");
        }

        doc.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
        doc.setVerifiedAt(Instant.now());
        doc.setVerifiedBy(reviewerId);
        doc.setRejectionReason(null);
        detailedDocumentStatusTransitionService.transitionDocumentStatus(
                doc, DetailedDocumentStatus.VERIFIED, reviewerId, "Document verified by administrative officer.");

        // Promote OCR-extracted attributes to legally verified state in citizen profile if present
        if (ocrResultRepository != null && citizenProfileService != null) {
            int ver = doc.getVersion() != null ? doc.getVersion() : 1;
            Optional<DocumentOcrResult> ocrOpt = ocrResultRepository.findByApplicationIdAndDocumentCodeAndVersion(
                    applicationId, documentCode, ver);
            if (ocrOpt.isPresent()) {
                DocumentOcrResult ocr = ocrOpt.get();
                if (ocr.getExtractedFields() != null) {
                    ocr.getExtractedFields().forEach((fieldKey, fieldDetail) -> {
                        if (fieldDetail != null && fieldDetail.getValue() != null) {
                            citizenProfileService.linkVerifiedDocumentAttribute(
                                    app.getUserId(),
                                    fieldKey,
                                    fieldDetail.getValue(),
                                    "DOCUMENT_VERIFIED",
                                    doc.getId(),
                                    documentCode,
                                    fieldDetail.getConfidence() != null ? fieldDetail.getConfidence() : 1.0
                            );
                        }
                    });
                }
            }
        }

        // Record DOCUMENT_VERIFIED timeline event
        applicationEventService.recordEvent(
                applicationId,
                app.getUserId(),
                ApplicationEventType.DOCUMENT_VERIFIED,
                app.getStatus(),
                app.getStatus(),
                "Document " + documentCode + " has been verified.",
                Map.of("documentCode", documentCode, "reviewerId", reviewerId)
        );

        // Notify Citizen
        notificationService.sendNotification(
                app.getUserId(),
                null,
                NotificationType.DOCUMENT_VERIFIED,
                "Document Verified: " + doc.getDocumentName(),
                "Your uploaded document " + doc.getDocumentName() + " has been verified.",
                "IN_APP",
                "APPLICATION",
                applicationId,
                reviewerId,
                Map.of("documentCode", documentCode)
        );

        // Record Audit Log
        adminAuditService.recordAction(
                reviewerId,
                "ADMIN",
                "DOCUMENT_VERIFIED",
                "APPLICATION_DOCUMENT",
                doc.getId() != null ? doc.getId() : documentCode,
                Map.of("status", "PENDING"),
                Map.of("status", "VERIFIED"),
                null,
                null,
                Map.of("applicationId", applicationId, "documentCode", documentCode)
        );

        return applicationService.mapToResponse(app);
    }

    @Transactional
    public ApplicationResponse rejectDocument(String applicationId, String documentCode, String reason, String reviewerId) {
        log.info("Rejecting document: application={}, code={}, reason={}, reviewer={}", applicationId, documentCode, reason, reviewerId);

        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("Rejection reason is mandatory.");
        }

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Application must be in UNDER_REVIEW state to reject documents.");
        }

        ApplicationDocument doc = applicationDocumentRepository.findByApplicationIdAndDocumentCode(applicationId, documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found for code: " + documentCode));

        if (!doc.isUploaded()) {
            throw new IllegalStateException("Cannot reject an unuploaded document.");
        }

        doc.setVerificationStatus(DocumentVerificationStatus.REJECTED);
        doc.setRejectedAt(Instant.now());
        doc.setRejectionReason(reason);
        detailedDocumentStatusTransitionService.transitionDocumentStatus(
                doc, DetailedDocumentStatus.REJECTED, reviewerId, reason);

        // Record DOCUMENT_REJECTED event
        applicationEventService.recordEvent(
                applicationId,
                app.getUserId(),
                ApplicationEventType.DOCUMENT_REJECTED,
                app.getStatus(),
                app.getStatus(),
                "Document " + documentCode + " rejected: " + reason,
                Map.of("documentCode", documentCode, "rejectionReason", reason, "reviewerId", reviewerId)
        );

        // Transition application state to CORRECTION_REQUIRED
        ApplicationStatus oldStatus = app.getStatus();
        app.setStatus(ApplicationStatus.CORRECTION_REQUIRED);
        app.setUpdatedAt(Instant.now());
        Application savedApp = applicationRepository.save(app);

        // Record CORRECTION_REQUIRED event
        applicationEventService.recordEvent(
                applicationId,
                app.getUserId(),
                ApplicationEventType.CORRECTION_REQUIRED,
                oldStatus,
                ApplicationStatus.CORRECTION_REQUIRED,
                "Application returned for correction. Action required by citizen.",
                Map.of("reviewerId", reviewerId)
        );

        // Notify Citizen
        notificationService.sendNotification(
                app.getUserId(),
                null,
                NotificationType.CORRECTION_REQUIRED,
                "Action Required: Document Rejected for " + app.getApplicationNumber(),
                "Document " + doc.getDocumentName() + " requires correction: " + reason + ". Please upload a replacement.",
                "IN_APP",
                "APPLICATION",
                applicationId,
                reviewerId,
                Map.of("documentCode", documentCode, "rejectionReason", reason)
        );

        // Record Audit Log
        adminAuditService.recordAction(
                reviewerId,
                "ADMIN",
                "DOCUMENT_REJECTED",
                "APPLICATION_DOCUMENT",
                doc.getId() != null ? doc.getId() : documentCode,
                Map.of("status", "PENDING"),
                Map.of("status", "REJECTED", "reason", reason),
                null,
                null,
                Map.of("applicationId", applicationId, "documentCode", documentCode)
        );

        return applicationService.mapToResponse(savedApp);
    }

    @Transactional
    public ApplicationResponse approveApplication(String applicationId, String remarks, String reviewerId, String reviewerRole) {
        log.info("Approving application: {}, reviewer: {}", applicationId, reviewerId);

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        ApplicationStatus oldStatus = app.getStatus();
        if (!applicationStatusTransitionService.isValidTransition(oldStatus, ApplicationStatus.APPROVED)) {
            throw new IllegalStateException("Cannot approve application in " + oldStatus + " state.");
        }

        // Verify all mandatory documents are VERIFIED
        List<ApplicationDocument> docs = applicationDocumentRepository.findAllByApplicationId(applicationId);
        for (ApplicationDocument d : docs) {
            if (d.isMandatory()) {
                if (!d.isUploaded()) {
                    throw new IllegalStateException("Cannot approve application. Mandatory document " + d.getDocumentCode() + " is missing.");
                }
                boolean isVerified = d.getDetailedStatus() == DetailedDocumentStatus.VERIFIED || d.getVerificationStatus() == DocumentVerificationStatus.VERIFIED;
                if (!isVerified || d.getDetailedStatus() == DetailedDocumentStatus.REJECTED || d.getDetailedStatus() == DetailedDocumentStatus.REUPLOAD_REQUIRED) {
                    throw new IllegalStateException("Cannot approve application. Mandatory document " + d.getDocumentCode() + " is not verified.");
                }
            }
        }

        // Re-evaluate deterministic legal eligibility against citizen's latest profile (including verified attributes)
        if (citizenProfileService != null && eligibilityEngine != null) {
            Scheme scheme = schemeRepository.findBySchemeCode(app.getSchemeCode())
                    .orElse(null);
            if (scheme != null) {
                CitizenProfile latestProfile = citizenProfileService.findProfile(app.getUserId()).orElse(null);
                if (latestProfile != null) {
                    CitizenEligibilityProfile evalProfile = citizenProfileService.toCitizenEligibilityProfile(latestProfile);
                    EligibilityEvaluationResponse reEval = eligibilityEngine.evaluateScheme(scheme, evalProfile);
                    if (reEval.getStatus() == EvaluationStatus.NOT_ELIGIBLE) {
                        throw new IllegalStateException("Cannot approve application: Applicant is NOT_ELIGIBLE based on verified profile attributes. Failed: " + reEval.getFailedConditions());
                    }
                    if (reEval.getStatus() == EvaluationStatus.INDETERMINATE) {
                        throw new IllegalStateException("Cannot approve application: Applicant eligibility is INDETERMINATE. Missing: " + reEval.getMissingInformation());
                    }
                }
            }
        }

        app.setStatus(ApplicationStatus.APPROVED);
        app.setUpdatedAt(Instant.now());
        Application savedApp = applicationRepository.save(app);

        // Update review record
        ApplicationReview review = applicationReviewRepository.findByApplicationId(applicationId)
                .orElse(new ApplicationReview());
        review.setApplicationId(applicationId);
        review.setReviewerId(reviewerId);
        review.setReviewerRole(reviewerRole);
        review.setReviewStatus(ApplicationStatus.APPROVED.name());
        review.setRemarks(remarks);
        review.setUpdatedAt(Instant.now());
        review.setCompletedAt(Instant.now());
        applicationReviewRepository.save(review);

        // Record APPROVED event
        applicationEventService.recordEvent(
                applicationId,
                app.getUserId(),
                ApplicationEventType.APPROVED,
                oldStatus,
                ApplicationStatus.APPROVED,
                "Application approved by administrative authority.",
                Map.of("remarks", remarks != null ? remarks : "", "reviewerId", reviewerId)
        );

        // Notify Citizen
        notificationService.sendNotification(
                app.getUserId(),
                null,
                NotificationType.APPLICATION_APPROVED,
                "Application Approved! " + app.getApplicationNumber(),
                "Your application " + app.getApplicationNumber() + " for scheme " + app.getSchemeCode() + " has been approved.",
                "IN_APP",
                "APPLICATION",
                applicationId,
                reviewerId,
                Map.of("applicationNumber", app.getApplicationNumber(), "remarks", remarks != null ? remarks : "")
        );

        // Record Audit Log
        adminAuditService.recordAction(
                reviewerId,
                reviewerRole,
                "APPLICATION_APPROVED",
                "APPLICATION",
                applicationId,
                Map.of("status", oldStatus.name()),
                Map.of("status", "APPROVED", "remarks", remarks != null ? remarks : ""),
                null,
                null,
                Map.of("applicationNumber", app.getApplicationNumber())
        );

        return applicationService.mapToResponse(savedApp);
    }

    @Transactional
    public ApplicationResponse rejectApplication(String applicationId, String reason, String reviewerId, String reviewerRole) {
        log.info("Rejecting application: {}, reason: {}, reviewer: {}", applicationId, reason, reviewerId);

        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("Rejection reason is mandatory.");
        }

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        ApplicationStatus oldStatus = app.getStatus();
        if (!applicationStatusTransitionService.isValidTransition(oldStatus, ApplicationStatus.REJECTED)) {
            throw new IllegalStateException("Cannot reject application in " + oldStatus + " state.");
        }

        app.setStatus(ApplicationStatus.REJECTED);
        app.setUpdatedAt(Instant.now());
        Application savedApp = applicationRepository.save(app);

        // Update review record
        ApplicationReview review = applicationReviewRepository.findByApplicationId(applicationId)
                .orElse(new ApplicationReview());
        review.setApplicationId(applicationId);
        review.setReviewerId(reviewerId);
        review.setReviewerRole(reviewerRole);
        review.setReviewStatus(ApplicationStatus.REJECTED.name());
        review.setDecisionReason(reason);
        review.setUpdatedAt(Instant.now());
        review.setCompletedAt(Instant.now());
        applicationReviewRepository.save(review);

        // Record REJECTED event
        applicationEventService.recordEvent(
                applicationId,
                app.getUserId(),
                ApplicationEventType.REJECTED,
                oldStatus,
                ApplicationStatus.REJECTED,
                "Application rejected by administrative authority. Reason: " + reason,
                Map.of("decisionReason", reason, "reviewerId", reviewerId)
        );

        // Notify Citizen
        notificationService.sendNotification(
                app.getUserId(),
                null,
                NotificationType.APPLICATION_REJECTED,
                "Application Decision: " + app.getApplicationNumber(),
                "Your application " + app.getApplicationNumber() + " was rejected: " + reason,
                "IN_APP",
                "APPLICATION",
                applicationId,
                reviewerId,
                Map.of("applicationNumber", app.getApplicationNumber(), "decisionReason", reason)
        );

        // Record Audit Log
        adminAuditService.recordAction(
                reviewerId,
                reviewerRole,
                "APPLICATION_REJECTED",
                "APPLICATION",
                applicationId,
                Map.of("status", oldStatus.name()),
                Map.of("status", "REJECTED", "reason", reason),
                null,
                null,
                Map.of("applicationNumber", app.getApplicationNumber())
        );

        return applicationService.mapToResponse(savedApp);
    }
}
