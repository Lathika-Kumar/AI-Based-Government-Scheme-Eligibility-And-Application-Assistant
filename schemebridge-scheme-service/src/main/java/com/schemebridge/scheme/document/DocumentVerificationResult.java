package com.schemebridge.scheme.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persisted AI-assisted and Officer document verification finding record.
 */
@Document(collection = "document_verification_results")
@CompoundIndexes({
    @CompoundIndex(name = "app_doc_ver_verify_idx", def = "{'applicationId': 1, 'documentCode': 1, 'version': 1}", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVerificationResult {

    @Id
    private String id;

    @Indexed
    private String applicationId;

    @Indexed
    private String documentId;

    @Indexed
    private String documentCode;

    private String documentType;

    @Indexed
    private String userId;

    private Integer version;

    @Indexed
    private String sha256;

    private Double overallScore;

    @Indexed
    private String aiStatus; // AI_VERIFIED, AI_REVIEW_REQUIRED, AI_REJECTED

    private String officerStatus; // PENDING, ADMIN_VERIFIED, CORRECTION_REQUIRED, REJECTED

    @Builder.Default
    private List<VerificationCheckDetail> checks = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> extractedFields = new HashMap<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Builder.Default
    private List<String> rejectionReasons = new ArrayList<>();

    private Instant processedAt;

    private Long processingDurationMs;

    private String reviewedBy;

    private Instant reviewedAt;

    private String remarks;

    private boolean duplicateDetected;
    private String duplicateReferenceId;
}
