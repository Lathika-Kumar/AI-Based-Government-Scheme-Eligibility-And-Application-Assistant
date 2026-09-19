package com.schemebridge.scheme.dto.response;

import com.schemebridge.scheme.document.VerificationCheckDetail;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVerificationResponse {
    private String id;
    private String applicationId;
    private String documentId;
    private String documentCode;
    private String documentType;
    private Integer version;
    private String sha256;
    private Double overallScore;
    private String aiStatus;
    private String officerStatus;

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
