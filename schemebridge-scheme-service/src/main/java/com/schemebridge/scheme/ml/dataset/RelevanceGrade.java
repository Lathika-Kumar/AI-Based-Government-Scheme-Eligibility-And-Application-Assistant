package com.schemebridge.scheme.ml.dataset;

/**
 * Standardized Learning-to-Rank (LTR) multi-level relevance grading.
 * Terminal-state based: higher events represent deeper citizen intent/conversion.
 * Invariant: Never manufacture an outcome that was not legitimately observed.
 */
public enum RelevanceGrade {
    IMPRESSED_UNENGAGED(0, "RECOMMENDATION_SHOWN"),
    VIEWED(1, "SCHEME_VIEWED"),
    INTENT_HIGH(2, "APPLICATION_STARTED"),
    CONVERTED(3, "APPLICATION_COMPLETED");

    private final int grade;
    private final String canonicalEventType;

    RelevanceGrade(int grade, String canonicalEventType) {
        this.grade = grade;
        this.canonicalEventType = canonicalEventType;
    }

    public int getGrade() {
        return grade;
    }

    public String getCanonicalEventType() {
        return canonicalEventType;
    }

    /**
     * Resolves a raw interaction event type string into its baseline relevance grade.
     */
    public static RelevanceGrade fromEventType(String eventType) {
        if (eventType == null) return IMPRESSED_UNENGAGED;
        String normalized = eventType.trim().toUpperCase();
        return switch (normalized) {
            case "APPLICATION_COMPLETED", "SCHEME_APPLIED" -> CONVERTED;
            case "APPLICATION_STARTED", "SCHEME_SAVED" -> INTENT_HIGH;
            case "SCHEME_VIEWED", "SCHEME_EXPANDED" -> VIEWED;
            case "RECOMMENDATION_SHOWN" -> IMPRESSED_UNENGAGED;
            default -> IMPRESSED_UNENGAGED;
        };
    }

    /**
     * Resolves multiple observed grades in a session into the maximum legitimately observed terminal grade.
     */
    public static RelevanceGrade resolveTerminalGrade(RelevanceGrade current, RelevanceGrade next) {
        if (current == null) return next != null ? next : IMPRESSED_UNENGAGED;
        if (next == null) return current;
        return current.getGrade() >= next.getGrade() ? current : next;
    }
}
