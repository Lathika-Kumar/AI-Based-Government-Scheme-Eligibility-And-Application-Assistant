package com.schemebridge.documentservice.dto;

import com.schemebridge.documentservice.document.DocumentAuditTrailItem;
import com.schemebridge.documentservice.document.DocumentVersionHistory;
import com.schemebridge.documentservice.enums.DocumentType;
import com.schemebridge.documentservice.enums.StorageProvider;
import com.schemebridge.documentservice.enums.VerificationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {
    private String id;
    private String authUserId;
    private String applicationId;
    private DocumentType documentType;
    private String documentName;
    private String originalFileName;
    private String storedFileName;
    private String mimeType;
    private String fileExtension;
    private long fileSize;
    private StorageProvider storageProvider;
    private VerificationStatus verificationStatus;
    private String verifiedBy;
    private String verifiedByName;
    private LocalDateTime verifiedAt;
    private String remarks;
    private String rejectionReason;
    private String checksumSHA256;
    private int currentVersion;
    private String downloadUrl;
    private List<DocumentVersionHistory> versionHistory;
    private List<DocumentAuditTrailItem> auditTrail;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
}
