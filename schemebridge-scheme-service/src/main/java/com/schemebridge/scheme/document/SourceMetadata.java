package com.schemebridge.scheme.document;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceMetadata {
    private String sourceType; // OFFICIAL_CENTRAL_GOVERNMENT, OFFICIAL_STATE_GOVERNMENT, OFFICIAL_UT_GOVERNMENT, OFFICIAL_MINISTRY, OFFICIAL_DEPARTMENT, OTHER_OFFICIAL
    private String sourceUrl;
    private String sourceTitle;
    
    private Instant lastVerified;
    private String verificationStatus; // VERIFIED, PENDING_VERIFICATION, ARCHIVED
    private String notes;

    // Government Circular & AI Traceability
    private String sourceDocumentId;
    private String sourceFileName;
    private String uploadedBy;
    private Instant uploadedAt;
    private Instant extractedAt;
    private String modelUsed;
    private String extractionStatus;
}
