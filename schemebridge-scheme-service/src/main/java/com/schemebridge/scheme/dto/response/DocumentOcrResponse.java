package com.schemebridge.scheme.dto.response;

import com.schemebridge.scheme.document.ExtractedFieldDetail;
import com.schemebridge.scheme.document.OcrExtractionStatus;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentOcrResponse {
    private String id;
    private String applicationId;
    private String documentId;
    private String documentCode;
    private String userId;
    private Integer version;
    private String detectedDocumentType;
    private String rawText;
    private Map<String, ExtractedFieldDetail> extractedFields;
    private Double overallConfidence;
    private OcrExtractionStatus extractionStatus;
    private String provider;
    private String providerVersion;
    private Instant processedAt;
    private Long processingDurationMs;
}
