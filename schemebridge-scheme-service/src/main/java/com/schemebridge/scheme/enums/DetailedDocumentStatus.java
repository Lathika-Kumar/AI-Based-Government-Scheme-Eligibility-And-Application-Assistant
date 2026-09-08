package com.schemebridge.scheme.enums;

/**
 * Fine-grained document lifecycle status for citizen and admin workflows.
 */
public enum DetailedDocumentStatus {
    NOT_UPLOADED,
    UPLOADED,
    OCR_PROCESSING,
    OCR_COMPLETED,
    PENDING_ADMIN_VERIFICATION,
    VERIFIED,
    REJECTED,
    REUPLOAD_REQUIRED
}
