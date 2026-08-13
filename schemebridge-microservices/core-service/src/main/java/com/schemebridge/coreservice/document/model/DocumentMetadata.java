package com.schemebridge.coreservice.document.model;

import com.schemebridge.coreservice.document.enums.DocumentType;
import com.schemebridge.coreservice.document.enums.StorageProvider;
import com.schemebridge.coreservice.document.enums.VerificationStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
    @CompoundIndex(name = "idx_auth_doctype", def = "{'authUserId': 1, 'documentType': 1}"),
    @CompoundIndex(name = "idx_app_status", def = "{'applicationId': 1, 'verificationStatus': 1}")
})
public class DocumentMetadata {

    @Id
    private String id;

    @Indexed
    private String authUserId;

    @Indexed
    private String applicationId;

    @Indexed
    private DocumentType documentType;

    private String documentName;
    private String originalFileName;
    private String storedFileName;
    private String mimeType;
    private String fileExtension;
    private long fileSize;

    private String storageLocation;
    private StorageProvider storageProvider;

    @Indexed
    private VerificationStatus verificationStatus;

    private String verifiedBy;
    private String verifiedByName;
    private LocalDateTime verifiedAt;
    private String remarks;
    private String rejectionReason;

    @Indexed
    private String checksumSHA256;

    @Builder.Default
    private int currentVersion = 1;

    @Builder.Default
    private List<DocumentVersionHistory> versionHistory = new ArrayList<>();

    @Builder.Default
    private List<DocumentAuditTrailItem> auditTrail = new ArrayList<>();

    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    private String uploadedBy;

    @Builder.Default
    private boolean active = true;
}
