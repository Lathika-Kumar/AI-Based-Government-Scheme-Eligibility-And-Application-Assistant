package com.schemebridge.scheme.document;

import java.util.EnumSet;
import java.util.Set;

public enum ApplicationStatus {
    DRAFT,
    DOCUMENTS_PENDING,
    READY_FOR_SUBMISSION,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    CANCELLED,
    CORRECTION_REQUIRED;

    /**
     * Statuses that represent an in-progress application.
     * An active application blocks creation of a new application for the same user + scheme.
     */
    public static final Set<ApplicationStatus> ACTIVE_STATUSES = EnumSet.of(
            DRAFT,
            DOCUMENTS_PENDING,
            READY_FOR_SUBMISSION,
            SUBMITTED,
            UNDER_REVIEW,
            CORRECTION_REQUIRED
    );

    /**
     * Statuses that represent a concluded application.
     * A terminal application allows reapplication for the same scheme.
     */
    public static final Set<ApplicationStatus> TERMINAL_STATUSES = EnumSet.of(
            REJECTED,
            CANCELLED,
            APPROVED
    );

    public boolean isActive() {
        return ACTIVE_STATUSES.contains(this);
    }

    public boolean isTerminal() {
        return TERMINAL_STATUSES.contains(this);
    }
}
