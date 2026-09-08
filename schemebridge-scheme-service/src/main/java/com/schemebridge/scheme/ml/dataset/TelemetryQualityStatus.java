package com.schemebridge.scheme.ml.dataset;

/**
 * Deterministic classification for telemetry events in Phase 29.
 * No records are silently discarded.
 */
public enum TelemetryQualityStatus {
    VALID,
    DUPLICATE,
    SYNTHETIC,
    MALFORMED,
    PII_VIOLATION,
    ORPHAN,
    INVALID_SEQUENCE
}
