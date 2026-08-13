package com.schemebridge.coreservice.document.enums;

public enum VerificationStatus {
    UPLOADED,       // Just uploaded, awaiting review
    UNDER_REVIEW,   // Officer has picked it up for review
    VERIFIED,       // Accepted and verified
    REJECTED,       // Rejected with reason
    EXPIRED         // Document expired (e.g., old certificate)
}
