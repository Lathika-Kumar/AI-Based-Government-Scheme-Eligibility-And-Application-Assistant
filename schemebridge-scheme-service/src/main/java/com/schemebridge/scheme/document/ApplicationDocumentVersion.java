package com.schemebridge.scheme.document;

import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDocumentVersion {
    private Integer version;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String storageReference;
    private String gridFsFileId;
    private DetailedDocumentStatus status;
    private DocumentVerificationStatus verificationStatus;
    private Instant uploadedAt;
    private Instant rejectedAt;
    private String rejectionReason;
    private Instant verifiedAt;
    private String verifiedBy;
}
