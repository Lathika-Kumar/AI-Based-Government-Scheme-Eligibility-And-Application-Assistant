package com.schemebridge.scheme.dto.response;

import com.schemebridge.scheme.document.ApplicationDocumentVersion;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDocumentResponse {
    private String id;
    private String applicationId;
    private String documentCode;
    private String documentName;
    private boolean mandatory;
    private boolean uploaded;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private Instant uploadedAt;
    private Instant verifiedAt;
    private Instant rejectedAt;
    private String schemeCode;
    private Integer version;
    private String downloadUrl;
    private String rejectionReason;
    private String verificationStatus;
    private DetailedDocumentStatus status;
    private List<ApplicationDocumentVersion> versionHistory;
}
