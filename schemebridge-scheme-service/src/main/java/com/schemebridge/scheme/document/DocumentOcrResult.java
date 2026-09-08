package com.schemebridge.scheme.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Persisted OCR extraction result for a specific version of an application document.
 */
@Document(collection = "document_ocr_results")
@CompoundIndexes({
    @CompoundIndex(name = "app_doc_ver_idx", def = "{'applicationId': 1, 'documentCode': 1, 'version': 1}", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentOcrResult {

    @Id
    private String id;

    @Indexed
    private String applicationId;

    @Indexed
    private String documentId;

    @Indexed
    private String documentCode;

    @Indexed
    private String userId;

    private Integer version;

    private String detectedDocumentType;

    private String rawText;

    @Builder.Default
    private Map<String, ExtractedFieldDetail> extractedFields = new HashMap<>();

    private Double overallConfidence;

    private OcrExtractionStatus extractionStatus;

    private String provider;

    private String providerVersion;

    @Indexed
    private Instant processedAt;

    private Long processingDurationMs;
}
