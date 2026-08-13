package com.schemebridge.coreservice.document.model;

import com.schemebridge.coreservice.document.enums.StorageProvider;
import com.schemebridge.coreservice.document.enums.VerificationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVersionHistory {
    private int version;
    private String originalFileName;
    private String storedFileName;
    private String mimeType;
    private String fileExtension;
    private long fileSize;
    private String storageLocation;
    private StorageProvider storageProvider;
    private String checksumSHA256;
    private VerificationStatus verificationStatus;
    private String remarks;
    private LocalDateTime uploadedAt;
    private String uploadedBy;
}
