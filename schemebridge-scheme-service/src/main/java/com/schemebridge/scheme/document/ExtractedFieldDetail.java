package com.schemebridge.scheme.document;

import lombok.*;

/**
 * Individual field extracted via OCR with confidence and provenance metadata.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedFieldDetail {
    private String fieldName;
    private Object value;
    private Double confidence;

    @Builder.Default
    private String source = "OCR_EXTRACTED";

    @Builder.Default
    private boolean verified = false;
}
