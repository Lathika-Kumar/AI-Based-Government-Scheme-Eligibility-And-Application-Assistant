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

    /** Existing SchemeBridge curated dataset provenance. */
    EXISTING_SCHEMEBRIDGE,

    /** Official MyScheme API extraction. */
    MYSCHEME_OFFICIAL_API,

    /** General informational requirement with no specific structured rule. */
    UNKNOWN_OR_UNSTRUCTURED
}
