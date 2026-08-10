package com.schemebridge.documentservice.service;

import com.schemebridge.documentservice.document.DocumentAuditTrailItem;
import com.schemebridge.documentservice.document.DocumentMetadata;
import com.schemebridge.documentservice.document.DocumentVersionHistory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DocumentHistoryService {

    public void addVersion(DocumentMetadata metadata) {
        DocumentVersionHistory version = DocumentVersionHistory.builder()
                .version(metadata.getCurrentVersion())
                .originalFileName(metadata.getOriginalFileName())
                .storedFileName(metadata.getStoredFileName())
                .mimeType(metadata.getMimeType())
                .fileExtension(metadata.getFileExtension())
                .fileSize(metadata.getFileSize())
                .storageLocation(metadata.getStorageLocation())
                .storageProvider(metadata.getStorageProvider())
                .checksumSHA256(metadata.getChecksumSHA256())
                .verificationStatus(metadata.getVerificationStatus())
                .remarks(metadata.getRemarks())
                .uploadedAt(metadata.getUploadedAt())
                .uploadedBy(metadata.getUploadedBy())
                .build();

        metadata.getVersionHistory().add(version);
        metadata.setCurrentVersion(metadata.getCurrentVersion() + 1);
    }

    public void addAuditEntry(DocumentMetadata metadata, String action, String performedBy,
                              String performedByName, String ipAddress, String remarks) {
        DocumentAuditTrailItem audit = DocumentAuditTrailItem.builder()
                .action(action)
                .performedBy(performedBy)
                .performedByName(performedByName)
                .ipAddress(ipAddress)
                .remarks(remarks)
                .timestamp(LocalDateTime.now())
                .build();

        metadata.getAuditTrail().add(audit);
    }
}
