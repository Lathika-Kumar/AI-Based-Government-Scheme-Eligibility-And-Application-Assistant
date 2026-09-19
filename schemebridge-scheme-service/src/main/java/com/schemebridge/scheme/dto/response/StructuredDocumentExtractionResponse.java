package com.schemebridge.scheme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructuredDocumentExtractionResponse {
    private String documentType;
    private String documentName;
    private Map<String, DocumentFieldExtraction> fields;
    private Double overallConfidence;
    private String extractionStatus; // "SUCCESS", "PARTIAL", "UNCERTAIN", "FAILED"
    private String rawText;
    private IdentityVerificationResult identityVerification;
    private Long processingDurationMs;
}
