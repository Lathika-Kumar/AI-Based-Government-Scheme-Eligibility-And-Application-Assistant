package com.schemebridge.scheme.enums;

/**
 * Provenance classification for document requirements.
 */
public enum RequirementProvenance {
    /** Explicitly configured in official government scheme circular. */
    VERIFIED_OFFICIAL,

    /** Extracted and verified from authoritative government scheme sources/checklists. */
    VERIFIED_SOURCE,

    /** Derived deterministically by system rule engine from eligibility criteria. */
    SYSTEM_CONFIGURED,

    /** General informational requirement with no specific structured rule. */
    UNKNOWN_OR_UNSTRUCTURED
}
