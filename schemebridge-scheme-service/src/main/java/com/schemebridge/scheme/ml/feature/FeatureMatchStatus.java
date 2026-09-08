package com.schemebridge.scheme.ml.feature;

/**
 * Tri-state feature match status for Phase 26 criteria evaluation.
 * Invariant: Every comparison field evaluates to exactly one of MATCH, MISMATCH, or UNKNOWN.
 * Missing/unspecified criteria must evaluate to UNKNOWN, never to MATCH.
 */
public enum FeatureMatchStatus {
    MATCH,
    MISMATCH,
    UNKNOWN
}
