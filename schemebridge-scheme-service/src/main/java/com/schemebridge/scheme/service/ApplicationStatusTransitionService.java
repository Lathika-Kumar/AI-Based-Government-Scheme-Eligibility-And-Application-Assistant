package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.ApplicationStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ApplicationStatusTransitionService {
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(ApplicationStatus.class);

    static {
        // DRAFT transitions (fallback support)
        ALLOWED_TRANSITIONS.put(ApplicationStatus.DRAFT, Set.of(
                ApplicationStatus.DOCUMENTS_PENDING,
                ApplicationStatus.READY_FOR_SUBMISSION,
                ApplicationStatus.CANCELLED
        ));

        // DOCUMENTS_PENDING transitions
        ALLOWED_TRANSITIONS.put(ApplicationStatus.DOCUMENTS_PENDING, Set.of(
                ApplicationStatus.READY_FOR_SUBMISSION,
                ApplicationStatus.CANCELLED
        ));

        // READY_FOR_SUBMISSION transitions
        ALLOWED_TRANSITIONS.put(ApplicationStatus.READY_FOR_SUBMISSION, Set.of(
                ApplicationStatus.DOCUMENTS_PENDING,
                ApplicationStatus.SUBMITTED,
                ApplicationStatus.CANCELLED
        ));

        // SUBMITTED transitions
        ALLOWED_TRANSITIONS.put(ApplicationStatus.SUBMITTED, Set.of(
                ApplicationStatus.UNDER_REVIEW,
                ApplicationStatus.CANCELLED
        ));

        // UNDER_REVIEW transitions
        ALLOWED_TRANSITIONS.put(ApplicationStatus.UNDER_REVIEW, Set.of(
                ApplicationStatus.APPROVED,
                ApplicationStatus.REJECTED,
                ApplicationStatus.CORRECTION_REQUIRED,
                ApplicationStatus.CANCELLED
        ));

        // CORRECTION_REQUIRED transitions
        ALLOWED_TRANSITIONS.put(ApplicationStatus.CORRECTION_REQUIRED, Set.of(
                ApplicationStatus.READY_FOR_SUBMISSION,
                ApplicationStatus.DOCUMENTS_PENDING,
                ApplicationStatus.CANCELLED
        ));

        // Terminal states have no outbound transitions
        ALLOWED_TRANSITIONS.put(ApplicationStatus.APPROVED, Set.of());
        ALLOWED_TRANSITIONS.put(ApplicationStatus.REJECTED, Set.of());
        ALLOWED_TRANSITIONS.put(ApplicationStatus.CANCELLED, Set.of());
    }

    public boolean isValidTransition(ApplicationStatus from, ApplicationStatus to) {
        Set<ApplicationStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }
}
