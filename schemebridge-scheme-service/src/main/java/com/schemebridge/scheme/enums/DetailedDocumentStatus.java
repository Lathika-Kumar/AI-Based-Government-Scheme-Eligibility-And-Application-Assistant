package com.schemebridge.scheme.enums;

/**
 * Fine-grained document lifecycle status for citizen and admin workflows.
 */
public enum DetailedDocumentStatus {
    NOT_UPLOADED,
    UPLOADING,
    UPLOADED,
    PROCESSING,
    OCR_PROCESSING,
    OCR_COMPLETED,
    AI_VERIFIED,
    AI_PASSED,
    AI_REVIEW_REQUIRED,
    AI_FLAGGED,
    AI_REJECTED,
    AI_FAILED,
    PENDING_ADMIN_VERIFICATION,
    UNDER_OFFICER_REVIEW,
    ADMIN_VERIFIED,
    VERIFIED,
    REJECTED,
    CORRECTION_REQUIRED,
    REUPLOAD_REQUIRED;

    public boolean isOfficerVerified() {
        return this == ADMIN_VERIFIED || this == VERIFIED;
    }

    public boolean isAiVerified() {
        return this == AI_VERIFIED || this == AI_PASSED;
    }

    public boolean isAiFlagged() {
        return this == AI_REVIEW_REQUIRED || this == AI_FLAGGED;
    }

    public boolean isAiFailed() {
        return this == AI_REJECTED || this == AI_FAILED;
    }

    public boolean isCorrectionRequired() {
        return this == CORRECTION_REQUIRED || this == REUPLOAD_REQUIRED;
    }

    public boolean isProcessing() {
        return this == UPLOADING || this == PROCESSING || this == OCR_PROCESSING;
    }
}
