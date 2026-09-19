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
import java.util.ArrayList;
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

    @Autowired(required = false)
    private com.schemebridge.scheme.repository.DocumentVerificationResultRepository documentVerificationResultRepository;

    @Autowired(required = false)
    private CitizenVaultDocumentService citizenVaultDocumentService;

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
        String sortField = (sort != null && !sort.isBlank()) ? sort : "createdAt";
        Sort sortSpec = Sort.by(dir, sortField).and(Sort.by(dir, "_id"));
        Pageable pageable = PageRequest.of(page, size, sortSpec);
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

    public Application findApplicationByIdOrNumber(String applicationId) {
        return applicationRepository.findById(applicationId)
                .or(() -> applicationRepository.findByApplicationNumber(applicationId))
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID or reference: " + applicationId));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationForReview(String applicationId) {
        Application app = findApplicationByIdOrNumber(applicationId);
        return applicationService.getApplicationDetails(app.getId(), null);
    }

    @Transactional
    public ApplicationResponse startReview(String applicationId, String reviewerId, String reviewerRole) {
        log.info("Starting review for application: {}, reviewer: {}", applicationId, reviewerId);

        Application app = findApplicationByIdOrNumber(applicationId);
        String resolvedId = app.getId();

        ApplicationStatus oldStatus = app.getStatus();
        if (!applicationStatusTransitionService.isValidTransition(oldStatus, ApplicationStatus.UNDER_REVIEW)) {
            throw new IllegalStateException("Cannot transition application from " + oldStatus + " to UNDER_REVIEW.");
        }

        app.setStatus(ApplicationStatus.UNDER_REVIEW);
        app.setUpdatedAt(Instant.now());
        Application savedApp = applicationRepository.save(app);

        // Upsert review record
        ApplicationReview review = applicationReviewRepository.findByApplicationId(resolvedId)
                .orElse(new ApplicationReview());
        
        review.setApplicationId(resolvedId);
        review.setReviewerId(reviewerId);
        review.setReviewerRole(reviewerRole);
        review.setReviewStatus(ApplicationStatus.UNDER_REVIEW.name());
        review.setCreatedAt(review.getCreatedAt() != null ? review.getCreatedAt() : Instant.now());
        review.setUpdatedAt(Instant.now());
        applicationReviewRepository.save(review);

        // Record UNDER_REVIEW timeline event
        applicationEventService.recordEvent(
                resolvedId,
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
                resolvedId,
                reviewerId,
                Map.of("applicationNumber", app.getApplicationNumber())
        );

        // Record Admin Audit Log
        adminAuditService.recordAction(
                reviewerId,
                reviewerRole,
                "APPLICATION_REVIEW_STARTED",
                "APPLICATION",
                resolvedId,
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

        Application app = findApplicationByIdOrNumber(applicationId);
        String resolvedId = app.getId();
        final String citizenUserId = app.getUserId();

        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            if (app.getStatus() == ApplicationStatus.DOCUMENTS_PENDING || app.getStatus() == ApplicationStatus.SUBMITTED || app.getStatus() == ApplicationStatus.READY_FOR_SUBMISSION) {
                startReview(resolvedId, reviewerId, "ADMIN");
                app = applicationRepository.findById(resolvedId).orElse(app);
            } else {
                throw new IllegalStateException("Application must be in UNDER_REVIEW state to verify documents.");
            }
        }

        ApplicationDocument doc = applicationDocumentRepository.findByApplicationIdAndDocumentCode(resolvedId, documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found for code: " + documentCode));

        if (!doc.isUploaded()) {
            throw new IllegalStateException("Cannot verify an unuploaded document.");
        }


        doc.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
        doc.setVerifiedAt(Instant.now());
        doc.setVerifiedBy(reviewerId);
        doc.setAdminReviewedBy(reviewerId);
        doc.setAdminReviewedAt(Instant.now());
        doc.setAdminVerificationResult("ADMIN_VERIFIED");
        doc.setRejectionReason(null);
        doc.setCorrectionReason(null);
        detailedDocumentStatusTransitionService.transitionDocumentStatus(
                doc, DetailedDocumentStatus.ADMIN_VERIFIED, reviewerId, "Document verified and approved by administrative officer.");

        // Synchronize officer verification to Citizen's permanent vault
        if (citizenVaultDocumentService != null) {
            try {
                citizenVaultDocumentService.syncOfficerVerification(citizenUserId, documentCode, reviewerId);
            } catch (Exception e) {
                log.warn("Failed to sync officer verification to Citizen Vault: {}", e.getMessage());
            }
        }

        // Update verification result if present
        if (documentVerificationResultRepository != null) {
            documentVerificationResultRepository.findTopByApplicationIdAndDocumentCodeOrderByVersionDesc(resolvedId, documentCode)
                    .ifPresent(vr -> {
                        vr.setOfficerStatus("ADMIN_VERIFIED");
                        vr.setReviewedBy(reviewerId);
                        vr.setReviewedAt(Instant.now());
                        vr.setRemarks("Document verified by administrative officer.");
                        documentVerificationResultRepository.save(vr);
                    });
        }

        // Promote OCR-extracted attributes to legally verified state in citizen profile if present
        if (ocrResultRepository != null && citizenProfileService != null) {
            int ver = doc.getVersion() != null ? doc.getVersion() : 1;
            Optional<DocumentOcrResult> ocrOpt = ocrResultRepository.findByApplicationIdAndDocumentCodeAndVersion(
                    resolvedId, documentCode, ver);
            if (ocrOpt.isPresent()) {
                DocumentOcrResult ocr = ocrOpt.get();
                if (ocr.getExtractedFields() != null) {
                    ocr.getExtractedFields().forEach((fieldKey, fieldDetail) -> {
                        if (fieldDetail != null && fieldDetail.getValue() != null) {
                            citizenProfileService.linkVerifiedDocumentAttribute(
                                    citizenUserId,
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
                resolvedId,
                app.getUserId(),
                ApplicationEventType.DOCUMENT_VERIFIED,
                app.getStatus(),
                app.getStatus(),
                "Document " + documentCode + " has been verified by administrative officer.",
                Map.of("documentCode", documentCode, "reviewerId", reviewerId)
        );

        // Notify Citizen
        notificationService.sendNotification(
                app.getUserId(),
                null,
                NotificationType.DOCUMENT_VERIFIED,
                "Document Verified: " + doc.getDocumentName(),
                "Your uploaded document " + doc.getDocumentName() + " has been verified by administrative officer.",
                "IN_APP",
                "APPLICATION",
                resolvedId,
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
                Map.of("status", "ADMIN_VERIFIED"),
                null,
                null,
                Map.of("applicationId", resolvedId, "documentCode", documentCode)
        );

        return applicationService.mapToResponse(app);
    }

    @Transactional
    public ApplicationResponse requestDocumentCorrection(String applicationId, String documentCode, String reason, String reviewerId) {
        log.info("Requesting document correction: application={}, code={}, reason={}, reviewer={}", applicationId, documentCode, reason, reviewerId);

        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("Correction reason is mandatory.");
        }

        Application app = findApplicationByIdOrNumber(applicationId);
        String resolvedId = app.getId();

        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            if (app.getStatus() == ApplicationStatus.DOCUMENTS_PENDING || app.getStatus() == ApplicationStatus.SUBMITTED || app.getStatus() == ApplicationStatus.READY_FOR_SUBMISSION) {
                startReview(resolvedId, reviewerId, "ADMIN");
                app = applicationRepository.findById(resolvedId).orElse(app);
            } else {
                throw new IllegalStateException("Application must be in UNDER_REVIEW state to request document correction.");
            }
        }

        ApplicationDocument doc = applicationDocumentRepository.findByApplicationIdAndDocumentCode(resolvedId, documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found for code: " + documentCode));

        if (!doc.isUploaded()) {
            throw new IllegalStateException("Cannot request correction for an unuploaded document.");
        }

        doc.setCorrectionReason(reason);
        doc.setRejectionReason(reason);
        doc.setRejectedAt(Instant.now());
        doc.setVerificationStatus(DocumentVerificationStatus.REJECTED);
        doc.setAdminVerificationResult("CORRECTION_REQUIRED");
        doc.setAdminReviewedBy(reviewerId);
        doc.setAdminReviewedAt(Instant.now());
        detailedDocumentStatusTransitionService.transitionDocumentStatus(
                doc, DetailedDocumentStatus.CORRECTION_REQUIRED, reviewerId, reason);

        // Update verification result if present
        if (documentVerificationResultRepository != null) {
            documentVerificationResultRepository.findTopByApplicationIdAndDocumentCodeOrderByVersionDesc(resolvedId, documentCode)
                    .ifPresent(vr -> {
                        vr.setOfficerStatus("CORRECTION_REQUIRED");
                        vr.setReviewedBy(reviewerId);
                        vr.setReviewedAt(Instant.now());
                        vr.setRemarks(reason);
                        documentVerificationResultRepository.save(vr);
                    });
        }

        // Record DOCUMENT_REJECTED event
        applicationEventService.recordEvent(
                resolvedId,
                app.getUserId(),
                ApplicationEventType.DOCUMENT_REJECTED,
                app.getStatus(),
                app.getStatus(),
                "Document " + documentCode + " correction requested: " + reason,
                Map.of("documentCode", documentCode, "correctionReason", reason, "reviewerId", reviewerId)
        );

        // Transition application state to CORRECTION_REQUIRED
        ApplicationStatus oldStatus = app.getStatus();
        app.setStatus(ApplicationStatus.CORRECTION_REQUIRED);
        app.setUpdatedAt(Instant.now());
        Application savedApp = applicationRepository.save(app);

        // Record CORRECTION_REQUIRED event
        applicationEventService.recordEvent(
                resolvedId,
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
                "Action Required: Document Correction Requested for " + app.getApplicationNumber(),
                "Document " + doc.getDocumentName() + " requires correction: " + reason + ". Please upload a replacement.",
                "IN_APP",
                "APPLICATION",
                resolvedId,
                reviewerId,
                Map.of("documentCode", documentCode, "correctionReason", reason)
        );

        // Record Audit Log
        adminAuditService.recordAction(
                reviewerId,
                "ADMIN",
                "DOCUMENT_CORRECTION_REQUESTED",
                "APPLICATION_DOCUMENT",
                doc.getId() != null ? doc.getId() : documentCode,
                Map.of("status", "PENDING"),
                Map.of("status", "CORRECTION_REQUIRED", "reason", reason),
                null,
                null,
                Map.of("applicationId", resolvedId, "documentCode", documentCode)
        );

        return applicationService.mapToResponse(savedApp);
    }

    @Transactional
    public ApplicationResponse rejectDocument(String applicationId, String documentCode, String reason, String reviewerId) {
        log.info("Rejecting document: application={}, code={}, reason={}, reviewer={}", applicationId, documentCode, reason, reviewerId);

        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("Rejection reason is mandatory.");
        }

        Application app = findApplicationByIdOrNumber(applicationId);
        String resolvedId = app.getId();

        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            if (app.getStatus() == ApplicationStatus.DOCUMENTS_PENDING || app.getStatus() == ApplicationStatus.SUBMITTED || app.getStatus() == ApplicationStatus.READY_FOR_SUBMISSION) {
                startReview(resolvedId, reviewerId, "ADMIN");
                app = applicationRepository.findById(resolvedId).orElse(app);
            } else {
                throw new IllegalStateException("Application must be in UNDER_REVIEW state to reject documents.");
            }
        }

        ApplicationDocument doc = applicationDocumentRepository.findByApplicationIdAndDocumentCode(resolvedId, documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found for code: " + documentCode));

        if (!doc.isUploaded()) {
            throw new IllegalStateException("Cannot reject an unuploaded document.");
        }

        doc.setVerificationStatus(DocumentVerificationStatus.REJECTED);
        doc.setRejectedAt(Instant.now());
        doc.setRejectionReason(reason);
        doc.setAdminVerificationResult("REJECTED");
        doc.setAdminReviewedBy(reviewerId);
        doc.setAdminReviewedAt(Instant.now());
        detailedDocumentStatusTransitionService.transitionDocumentStatus(
                doc, DetailedDocumentStatus.REJECTED, reviewerId, reason);

        // Update verification result if present
        if (documentVerificationResultRepository != null) {
            documentVerificationResultRepository.findTopByApplicationIdAndDocumentCodeOrderByVersionDesc(resolvedId, documentCode)
                    .ifPresent(vr -> {
                        vr.setOfficerStatus("REJECTED");
                        vr.setReviewedBy(reviewerId);
                        vr.setReviewedAt(Instant.now());
                        vr.setRemarks(reason);
                        documentVerificationResultRepository.save(vr);
                    });
        }

        // Record DOCUMENT_REJECTED event
        applicationEventService.recordEvent(
                resolvedId,
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
                resolvedId,
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
                resolvedId,
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
                Map.of("applicationId", resolvedId, "documentCode", documentCode)
        );

        return applicationService.mapToResponse(savedApp);
    }

    @Transactional
    public ApplicationResponse approveApplication(String applicationId, String remarks, String reviewerId, String reviewerRole) {
        log.info("Approving application: {}, reviewer: {}", applicationId, reviewerId);

        Application app = findApplicationByIdOrNumber(applicationId);
        String resolvedId = app.getId();

        if (app.getStatus() == ApplicationStatus.SUBMITTED || app.getStatus() == ApplicationStatus.DOCUMENTS_PENDING || app.getStatus() == ApplicationStatus.READY_FOR_SUBMISSION) {
            startReview(resolvedId, reviewerId, reviewerRole);
            app = applicationRepository.findById(resolvedId).orElse(app);
        }

        ApplicationStatus oldStatus = app.getStatus();
        if (!applicationStatusTransitionService.isValidTransition(oldStatus, ApplicationStatus.APPROVED)) {
            throw new IllegalStateException("Cannot approve application in " + oldStatus + " state.");
        }

        // Strict Application Approval Gate: All mandatory documents must be officer-verified (ADMIN_VERIFIED or VERIFIED)
        List<ApplicationDocument> docs = applicationDocumentRepository.findAllByApplicationId(resolvedId);
        List<String> unverifiedMandatory = new ArrayList<>();
        for (ApplicationDocument d : docs) {
            if (d.isMandatory()) {
                if (!d.isUploaded()) {
                    unverifiedMandatory.add(d.getDocumentCode());
                    continue;
                }
                boolean isOfficerVerified = d.getDetailedStatus() != null && d.getDetailedStatus().isOfficerVerified();
                if (!isOfficerVerified) {
                    unverifiedMandatory.add(d.getDocumentCode());
                }
            }
        }
        if (!unverifiedMandatory.isEmpty()) {
            throw new IllegalStateException("DOCUMENTS_NOT_VERIFIED: Application cannot be approved until all mandatory documents are officer-verified. Pending documents: " + unverifiedMandatory);
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
        ApplicationReview review = applicationReviewRepository.findByApplicationId(resolvedId)
                .orElse(new ApplicationReview());
        review.setApplicationId(resolvedId);
        review.setReviewerId(reviewerId);
        review.setReviewerRole(reviewerRole);
        review.setReviewStatus(ApplicationStatus.APPROVED.name());
        review.setRemarks(remarks);
        review.setUpdatedAt(Instant.now());
        review.setCompletedAt(Instant.now());
        applicationReviewRepository.save(review);

        // Record APPROVED event
        applicationEventService.recordEvent(
                resolvedId,
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
                resolvedId,
                reviewerId,
                Map.of("applicationNumber", app.getApplicationNumber(), "remarks", remarks != null ? remarks : "")
        );

        // Record Audit Log
        adminAuditService.recordAction(
                reviewerId,
                reviewerRole,
                "APPLICATION_APPROVED",
                "APPLICATION",
                resolvedId,
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

        Application app = findApplicationByIdOrNumber(applicationId);
        String resolvedId = app.getId();

        if (app.getStatus() == ApplicationStatus.SUBMITTED) {
            startReview(resolvedId, reviewerId, reviewerRole);
            app = applicationRepository.findById(resolvedId).orElse(app);
        }

        ApplicationStatus oldStatus = app.getStatus();
        if (!applicationStatusTransitionService.isValidTransition(oldStatus, ApplicationStatus.REJECTED)) {
            throw new IllegalStateException("Cannot reject application in " + oldStatus + " state.");
        }

        app.setStatus(ApplicationStatus.REJECTED);
        app.setUpdatedAt(Instant.now());
        Application savedApp = applicationRepository.save(app);

        // Update review record
        ApplicationReview review = applicationReviewRepository.findByApplicationId(resolvedId)
                .orElse(new ApplicationReview());
        review.setApplicationId(resolvedId);
        review.setReviewerId(reviewerId);
        review.setReviewerRole(reviewerRole);
        review.setReviewStatus(ApplicationStatus.REJECTED.name());
        review.setDecisionReason(reason);
        review.setRemarks(reason);
        review.setUpdatedAt(Instant.now());
        review.setCompletedAt(Instant.now());
        applicationReviewRepository.save(review);

        // Record REJECTED event
        applicationEventService.recordEvent(
                resolvedId,
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
                resolvedId,
                reviewerId,
                Map.of("applicationNumber", app.getApplicationNumber(), "decisionReason", reason)
        );

        // Record Audit Log
        adminAuditService.recordAction(
                reviewerId,
                reviewerRole,
                "APPLICATION_REJECTED",
                "APPLICATION",
                resolvedId,
                Map.of("status", oldStatus.name()),
                Map.of("status", "REJECTED", "reason", reason),
                null,
                null,
                Map.of("applicationNumber", app.getApplicationNumber())
        );

        return applicationService.mapToResponse(savedApp);
    }

    @Transactional
    public ApplicationResponse requestMoreDocuments(String applicationId, String reason, String reviewerId, String reviewerRole) {
        log.info("Requesting more documents for application: {}, reason: {}, reviewer: {}", applicationId, reason, reviewerId);

        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("Correction/document request reason is mandatory.");
        }

        Application app = findApplicationByIdOrNumber(applicationId);
        String resolvedId = app.getId();

        if (app.getStatus() == ApplicationStatus.SUBMITTED) {
            startReview(resolvedId, reviewerId, reviewerRole);
            app = applicationRepository.findById(resolvedId).orElse(app);
        }

        ApplicationStatus oldStatus = app.getStatus();
        if (!applicationStatusTransitionService.isValidTransition(oldStatus, ApplicationStatus.CORRECTION_REQUIRED)) {
            throw new IllegalStateException("Cannot request documents for application in " + oldStatus + " state.");
        }

        app.setStatus(ApplicationStatus.CORRECTION_REQUIRED);
        app.setUpdatedAt(Instant.now());
        Application savedApp = applicationRepository.save(app);

        // Update review record
        ApplicationReview review = applicationReviewRepository.findByApplicationId(resolvedId)
                .orElse(new ApplicationReview());
        review.setApplicationId(resolvedId);
        review.setReviewerId(reviewerId);
        review.setReviewerRole(reviewerRole);
        review.setReviewStatus(ApplicationStatus.CORRECTION_REQUIRED.name());
        review.setDecisionReason(reason);
        review.setRemarks(reason);
        review.setUpdatedAt(Instant.now());
        applicationReviewRepository.save(review);

        // Record CORRECTION_REQUIRED event
        applicationEventService.recordEvent(
                resolvedId,
                app.getUserId(),
                ApplicationEventType.CORRECTION_REQUIRED,
                oldStatus,
                ApplicationStatus.CORRECTION_REQUIRED,
                "Application returned for additional documents / correction. Reason: " + reason,
                Map.of("reason", reason, "reviewerId", reviewerId)
        );

        // Notify Citizen
        notificationService.sendNotification(
                app.getUserId(),
                null,
                NotificationType.CORRECTION_REQUIRED,
                "Action Required: Documents Requested for " + app.getApplicationNumber(),
                "Your application " + app.getApplicationNumber() + " requires additional documents or correction: " + reason,
                "IN_APP",
                "APPLICATION",
                resolvedId,
                reviewerId,
                Map.of("applicationNumber", app.getApplicationNumber(), "reason", reason)
        );

        // Record Audit Log
        adminAuditService.recordAction(
                reviewerId,
                reviewerRole,
                "APPLICATION_CORRECTION_REQUIRED",
                "APPLICATION",
                resolvedId,
                Map.of("status", oldStatus.name()),
                Map.of("status", "CORRECTION_REQUIRED", "reason", reason),
                null,
                null,
                Map.of("applicationNumber", app.getApplicationNumber())
        );

        return applicationService.mapToResponse(savedApp);
    }
}
