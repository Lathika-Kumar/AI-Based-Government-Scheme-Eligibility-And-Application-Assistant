package com.schemebridge.scheme.ml.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Normalized interaction event telemetry model for offline LTR dataset attribution.
 * Invariant: Completely PII-free. Explicit fields and metadata keys are sanitized.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInteractionEvent {

    private String eventId;
    private String sessionId;
    private String schemeCode;
    private String eventType;
    private long timestamp;
    private boolean isSyntheticFixture;

    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();

    private static final Set<String> FORBIDDEN_PII_KEYWORDS = Set.of(
            "aadhaar", "uid", "pan", "phone", "mobile", "email",
            "name", "firstname", "lastname", "fullname", "address",
            "street", "ip", "ipaddress", "location", "lat", "lon"
    );

    /**
     * Validates and sanitizes metadata to guarantee zero-PII telemetry.
     */
    public static Map<String, Object> sanitizeMetadata(Map<String, Object> rawMetadata) {
        if (rawMetadata == null || rawMetadata.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> clean = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawMetadata.entrySet()) {
            String key = entry.getKey();
            if (key == null) continue;
            String lowerKey = key.trim().toLowerCase();
            boolean isPii = FORBIDDEN_PII_KEYWORDS.stream().anyMatch(lowerKey::contains);
            if (!isPii) {
                clean.put(key, entry.getValue());
            }
        }
        return Collections.unmodifiableMap(clean);
    }
}
