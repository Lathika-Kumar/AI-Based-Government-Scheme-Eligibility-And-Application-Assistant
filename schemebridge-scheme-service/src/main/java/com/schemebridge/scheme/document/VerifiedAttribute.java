package com.schemebridge.scheme.document;

import lombok.*;
import java.time.Instant;

/**
 * Encapsulates the value, verification status, and provenance of a citizen attribute.
 *
 * Source states:
 *   - SELF_DECLARED: Manually entered by citizen.
 *   - OCR_EXTRACTED: Extracted from uploaded document via AI OCR (unconfirmed).
 *   - DOCUMENT_VERIFIED: Confirmed by admin officer during application review.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifiedAttribute<T> {
    private T value;

    @Builder.Default
    private String source = "SELF_DECLARED";

    @Builder.Default
    private Boolean verified = false;

    private String documentId;
    private String documentCode;
    private Double confidenceScore;
    private Instant verifiedAt;

    public static <T> VerifiedAttribute<T> selfDeclared(T value) {
        return VerifiedAttribute.<T>builder()
                .value(value)
                .source("SELF_DECLARED")
                .verified(false)
                .build();
    }

    public static <T> VerifiedAttribute<T> verified(T value, String source, String documentId, String documentCode, Double confidenceScore) {
        return VerifiedAttribute.<T>builder()
                .value(value)
                .source(source)
                .verified(true)
                .documentId(documentId)
                .documentCode(documentCode)
                .confidenceScore(confidenceScore)
                .verifiedAt(Instant.now())
                .build();
    }
}
