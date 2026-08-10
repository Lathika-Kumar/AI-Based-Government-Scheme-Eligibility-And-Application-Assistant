package com.schemebridge.applicationservice.enums;

public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    DOCUMENT_PENDING,
    VERIFIED,
    APPROVED,
    REJECTED,
    WITHDRAWN,
    BENEFIT_RELEASED;

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == WITHDRAWN || this == BENEFIT_RELEASED;
    }

    public boolean isActive() {
        return this == DRAFT || this == SUBMITTED || this == UNDER_REVIEW
                || this == DOCUMENT_PENDING || this == VERIFIED;
    }
}
