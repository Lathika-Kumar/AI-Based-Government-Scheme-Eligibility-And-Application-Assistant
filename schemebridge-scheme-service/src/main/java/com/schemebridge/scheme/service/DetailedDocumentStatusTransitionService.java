package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.repository.ApplicationDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;

/**
 * Authoritative lifecycle state machine for citizen application documents.
 * Validates and enforces legal state transitions, version preservation,
 * mandatory rejection audit logs, and status consistency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DetailedDocumentStatusTransitionService {

    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final ApplicationEventService applicationEventService;

    // Allowed transition map
    private static final Map<DetailedDocumentStatus, Set<DetailedDocumentStatus>> LEGAL_TRANSITIONS = new EnumMap<>(DetailedDocumentStatus.class);

    static {
        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.NOT_UPLOADED, Set.of(
                DetailedDocumentStatus.UPLOADING,
                DetailedDocumentStatus.UPLOADED
        ));

        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.UPLOADING, Set.of(
                DetailedDocumentStatus.UPLOADED,
                DetailedDocumentStatus.PROCESSING,
                DetailedDocumentStatus.NOT_UPLOADED
        ));

        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.UPLOADED, Set.of(
                DetailedDocumentStatus.PROCESSING,
                DetailedDocumentStatus.OCR_PROCESSING,
                DetailedDocumentStatus.OCR_COMPLETED,
                DetailedDocumentStatus.AI_VERIFIED,
                DetailedDocumentStatus.AI_PASSED,
                DetailedDocumentStatus.AI_REVIEW_REQUIRED,
                DetailedDocumentStatus.AI_FLAGGED,
                DetailedDocumentStatus.AI_REJECTED,
                DetailedDocumentStatus.AI_FAILED,
                DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION,
                DetailedDocumentStatus.UNDER_OFFICER_REVIEW,
                DetailedDocumentStatus.ADMIN_VERIFIED,
                DetailedDocumentStatus.VERIFIED,
                DetailedDocumentStatus.REJECTED,
                DetailedDocumentStatus.CORRECTION_REQUIRED,
                DetailedDocumentStatus.REUPLOAD_REQUIRED,
                DetailedDocumentStatus.UPLOADED // Same state re-upload
        ));

        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.PROCESSING, Set.of(
                DetailedDocumentStatus.OCR_PROCESSING,
                DetailedDocumentStatus.OCR_COMPLETED,
                DetailedDocumentStatus.AI_VERIFIED,
                DetailedDocumentStatus.AI_PASSED,
                DetailedDocumentStatus.AI_REVIEW_REQUIRED,
                DetailedDocumentStatus.AI_FLAGGED,
                DetailedDocumentStatus.AI_REJECTED,
                DetailedDocumentStatus.AI_FAILED,
                DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION,
                DetailedDocumentStatus.UNDER_OFFICER_REVIEW,
                DetailedDocumentStatus.ADMIN_VERIFIED,
                DetailedDocumentStatus.VERIFIED,
                DetailedDocumentStatus.REJECTED,
                DetailedDocumentStatus.CORRECTION_REQUIRED,
                DetailedDocumentStatus.REUPLOAD_REQUIRED,
                DetailedDocumentStatus.UPLOADED
        ));

        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.OCR_PROCESSING, Set.of(
                DetailedDocumentStatus.OCR_COMPLETED,
                DetailedDocumentStatus.AI_VERIFIED,
                DetailedDocumentStatus.AI_PASSED,
                DetailedDocumentStatus.AI_REVIEW_REQUIRED,
                DetailedDocumentStatus.AI_FLAGGED,
                DetailedDocumentStatus.AI_REJECTED,
                DetailedDocumentStatus.AI_FAILED,
                DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION,
                DetailedDocumentStatus.UNDER_OFFICER_REVIEW,
                DetailedDocumentStatus.ADMIN_VERIFIED,
                DetailedDocumentStatus.VERIFIED,
                DetailedDocumentStatus.REJECTED,
                DetailedDocumentStatus.CORRECTION_REQUIRED,
                DetailedDocumentStatus.REUPLOAD_REQUIRED,
                DetailedDocumentStatus.UPLOADED
        ));

        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.OCR_COMPLETED, Set.of(
                DetailedDocumentStatus.AI_VERIFIED,
                DetailedDocumentStatus.AI_PASSED,
                DetailedDocumentStatus.AI_REVIEW_REQUIRED,
                DetailedDocumentStatus.AI_FLAGGED,
                DetailedDocumentStatus.AI_REJECTED,
                DetailedDocumentStatus.AI_FAILED,
                DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION,
                DetailedDocumentStatus.UNDER_OFFICER_REVIEW,
                DetailedDocumentStatus.ADMIN_VERIFIED,
                DetailedDocumentStatus.VERIFIED,
                DetailedDocumentStatus.REJECTED,
                DetailedDocumentStatus.CORRECTION_REQUIRED,
                DetailedDocumentStatus.REUPLOAD_REQUIRED,
                DetailedDocumentStatus.UPLOADED
        ));

        Set<DetailedDocumentStatus> aiPassedTransitions = Set.of(
                DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION,
                DetailedDocumentStatus.UNDER_OFFICER_REVIEW,
                DetailedDocumentStatus.ADMIN_VERIFIED,
                DetailedDocumentStatus.VERIFIED,
                DetailedDocumentStatus.CORRECTION_REQUIRED,
                DetailedDocumentStatus.REUPLOAD_REQUIRED,
                DetailedDocumentStatus.REJECTED,
                DetailedDocumentStatus.PROCESSING,
                DetailedDocumentStatus.OCR_PROCESSING,
                DetailedDocumentStatus.OCR_COMPLETED,
                DetailedDocumentStatus.UPLOADED
        );
        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.AI_VERIFIED, aiPassedTransitions);
        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.AI_PASSED, aiPassedTransitions);

        Set<DetailedDocumentStatus> aiReviewTransitions = Set.of(
                DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION,
                DetailedDocumentStatus.UNDER_OFFICER_REVIEW,
                DetailedDocumentStatus.ADMIN_VERIFIED,
                DetailedDocumentStatus.VERIFIED,
                DetailedDocumentStatus.CORRECTION_REQUIRED,
                DetailedDocumentStatus.REUPLOAD_REQUIRED,
                DetailedDocumentStatus.REJECTED,
                DetailedDocumentStatus.PROCESSING,
                DetailedDocumentStatus.OCR_PROCESSING,
                DetailedDocumentStatus.OCR_COMPLETED,
                DetailedDocumentStatus.UPLOADED
        );
        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.AI_REVIEW_REQUIRED, aiReviewTransitions);
        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.AI_FLAGGED, aiReviewTransitions);

        Set<DetailedDocumentStatus> aiRejectTransitions = Set.of(
                DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION,
                DetailedDocumentStatus.UNDER_OFFICER_REVIEW,
                DetailedDocumentStatus.ADMIN_VERIFIED,
                DetailedDocumentStatus.VERIFIED,
                DetailedDocumentStatus.CORRECTION_REQUIRED,
                DetailedDocumentStatus.REUPLOAD_REQUIRED,
                DetailedDocumentStatus.REJECTED,
                DetailedDocumentStatus.PROCESSING,
                DetailedDocumentStatus.OCR_PROCESSING,
                DetailedDocumentStatus.UPLOADED
        );
        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.AI_REJECTED, aiRejectTransitions);
        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.AI_FAILED, aiRejectTransitions);

        Set<DetailedDocumentStatus> officerReviewTransitions = Set.of(
                DetailedDocumentStatus.ADMIN_VERIFIED,
                DetailedDocumentStatus.VERIFIED,
                DetailedDocumentStatus.REJECTED,
                DetailedDocumentStatus.CORRECTION_REQUIRED,
                DetailedDocumentStatus.REUPLOAD_REQUIRED,
                DetailedDocumentStatus.OCR_PROCESSING,
                DetailedDocumentStatus.PROCESSING,
                DetailedDocumentStatus.UPLOADED
        );
        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION, officerReviewTransitions);
        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.UNDER_OFFICER_REVIEW, officerReviewTransitions);

        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.REJECTED, Set.of(
                DetailedDocumentStatus.CORRECTION_REQUIRED,
                DetailedDocumentStatus.REUPLOAD_REQUIRED,
                DetailedDocumentStatus.ADMIN_VERIFIED,
                DetailedDocumentStatus.VERIFIED,
                DetailedDocumentStatus.UPLOADED
        ));

        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.REUPLOAD_REQUIRED, Set.of(
                DetailedDocumentStatus.UPLOADED,
                DetailedDocumentStatus.CORRECTION_REQUIRED,
                DetailedDocumentStatus.ADMIN_VERIFIED,
                DetailedDocumentStatus.VERIFIED
        ));

        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.CORRECTION_REQUIRED, Set.of(
                DetailedDocumentStatus.UPLOADED,
                DetailedDocumentStatus.REUPLOAD_REQUIRED,
                DetailedDocumentStatus.ADMIN_VERIFIED,
                DetailedDocumentStatus.VERIFIED
        ));

        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.ADMIN_VERIFIED, Set.of(
                DetailedDocumentStatus.UPLOADED, // Replacement upload
                DetailedDocumentStatus.CORRECTION_REQUIRED,
                DetailedDocumentStatus.REJECTED
        ));

        LEGAL_TRANSITIONS.put(DetailedDocumentStatus.VERIFIED, Set.of(
                DetailedDocumentStatus.UPLOADED, // Replacement upload
                DetailedDocumentStatus.CORRECTION_REQUIRED,
                DetailedDocumentStatus.REJECTED
        ));
    }

    public boolean isValidTransition(DetailedDocumentStatus currentStatus, DetailedDocumentStatus targetStatus) {
        if (currentStatus == null) {
            return targetStatus == DetailedDocumentStatus.NOT_UPLOADED || targetStatus == DetailedDocumentStatus.UPLOADED || targetStatus == DetailedDocumentStatus.UPLOADING;
        }
        if (currentStatus == targetStatus) {
            return true;
        }
        Set<DetailedDocumentStatus> allowed = LEGAL_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }

    public void validateTransition(DetailedDocumentStatus currentStatus, DetailedDocumentStatus targetStatus) {
        if (!isValidTransition(currentStatus, targetStatus)) {
            log.error("Illegal document status transition attempted: {} -> {}", currentStatus, targetStatus);
            throw new IllegalStateException("Illegal document state transition from " + currentStatus + " to " + targetStatus + ".");
        }
    }

    @Transactional
    public ApplicationDocument transitionDocumentStatus(
            ApplicationDocument doc,
            DetailedDocumentStatus targetStatus,
            String actorId,
            String remarksOrReason
    ) {
        if (doc == null) {
            throw new IllegalArgumentException("ApplicationDocument must not be null.");
        }

        DetailedDocumentStatus current = doc.getDetailedStatus() != null ? doc.getDetailedStatus() :
                (doc.isUploaded() ?
                        (doc.getVerificationStatus() == DocumentVerificationStatus.VERIFIED ? DetailedDocumentStatus.VERIFIED :
                                (doc.getVerificationStatus() == DocumentVerificationStatus.REJECTED ? DetailedDocumentStatus.REJECTED : DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION))
                        : DetailedDocumentStatus.NOT_UPLOADED);

        validateTransition(current, targetStatus);

        // Rejection / Correction reason validation and assignment
        if (targetStatus == DetailedDocumentStatus.REJECTED) {
            if (!StringUtils.hasText(remarksOrReason)) {
                throw new IllegalArgumentException("Rejection reason is mandatory when transitioning to " + targetStatus + ".");
            }
            doc.setRejectionReason(remarksOrReason);
            doc.setRejectedAt(Instant.now());
            doc.setVerificationStatus(DocumentVerificationStatus.REJECTED);
            doc.setAdminVerificationResult("REJECTED");
            doc.setAdminReviewedBy(actorId);
            doc.setAdminReviewedAt(Instant.now());
        } else if (targetStatus == DetailedDocumentStatus.CORRECTION_REQUIRED || targetStatus == DetailedDocumentStatus.REUPLOAD_REQUIRED) {
            if (!StringUtils.hasText(remarksOrReason)) {
                throw new IllegalArgumentException("Correction reason is mandatory when transitioning to " + targetStatus + ".");
            }
            doc.setCorrectionReason(remarksOrReason);
            doc.setRejectionReason(remarksOrReason);
            doc.setRejectedAt(Instant.now());
            doc.setVerificationStatus(DocumentVerificationStatus.REJECTED);
            doc.setAdminVerificationResult("CORRECTION_REQUIRED");
            doc.setAdminReviewedBy(actorId);
            doc.setAdminReviewedAt(Instant.now());
        } else if (targetStatus == DetailedDocumentStatus.ADMIN_VERIFIED || targetStatus == DetailedDocumentStatus.VERIFIED) {
            doc.setVerifiedAt(Instant.now());
            doc.setVerifiedBy(actorId);
            doc.setAdminReviewedBy(actorId);
            doc.setAdminReviewedAt(Instant.now());
            doc.setAdminVerificationResult("ADMIN_VERIFIED");
            doc.setRejectionReason(null);
            doc.setCorrectionReason(null);
            doc.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
        } else if (targetStatus == DetailedDocumentStatus.AI_VERIFIED || targetStatus == DetailedDocumentStatus.AI_PASSED) {
            doc.setAiVerificationResult("AI_VERIFIED");
            doc.setVerificationStatus(DocumentVerificationStatus.PENDING);
        } else if (targetStatus == DetailedDocumentStatus.AI_REVIEW_REQUIRED || targetStatus == DetailedDocumentStatus.AI_FLAGGED) {
            doc.setAiVerificationResult("AI_REVIEW_REQUIRED");
            doc.setVerificationStatus(DocumentVerificationStatus.PENDING);
        } else if (targetStatus == DetailedDocumentStatus.AI_REJECTED || targetStatus == DetailedDocumentStatus.AI_FAILED) {
            doc.setAiVerificationResult("AI_REJECTED");
            doc.setVerificationStatus(DocumentVerificationStatus.REJECTED);
            if (StringUtils.hasText(remarksOrReason)) {
                doc.setRejectionReason(remarksOrReason);
            }
        } else if (targetStatus == DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION || targetStatus == DetailedDocumentStatus.UNDER_OFFICER_REVIEW) {
            doc.setVerificationStatus(DocumentVerificationStatus.PENDING);
        } else if (targetStatus == DetailedDocumentStatus.UPLOADED) {
            doc.setUploaded(true);
            doc.setUploadedAt(Instant.now());
            doc.setVerificationStatus(DocumentVerificationStatus.PENDING);
            doc.setRejectionReason(null);
            doc.setCorrectionReason(null);
            doc.setRejectedAt(null);
            doc.setVerifiedAt(null);
            doc.setVerifiedBy(null);
        }

        doc.setDetailedStatus(targetStatus);
        ApplicationDocument saved = applicationDocumentRepository.save(doc);

        // Record transition audit event for state transitions other than standard UPLOADED (which is recorded as DOCUMENT_UPLOADED)
        if (targetStatus != DetailedDocumentStatus.UPLOADED && applicationEventService != null) {
            applicationEventService.recordEvent(
                    doc.getApplicationId(),
                    actorId != null ? actorId : doc.getUserId(),
                    ApplicationEventType.DOCUMENT_STATUS_CHANGED,
                    null,
                    null,
                    "Document " + doc.getDocumentCode() + " transitioned from " + current + " to " + targetStatus +
                            (StringUtils.hasText(remarksOrReason) ? (" (Reason: " + remarksOrReason + ")") : ""),
                    Map.of(
                            "documentCode", doc.getDocumentCode() != null ? doc.getDocumentCode() : "",
                            "fromStatus", current.name(),
                            "toStatus", targetStatus.name(),
                            "version", doc.getVersion() != null ? doc.getVersion() : 1,
                            "actorId", actorId != null ? actorId : "SYSTEM"
                    )
            );
        }

        return saved;
    }
}
