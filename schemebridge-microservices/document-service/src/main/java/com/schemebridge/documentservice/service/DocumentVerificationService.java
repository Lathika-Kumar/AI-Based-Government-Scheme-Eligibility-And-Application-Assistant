package com.schemebridge.documentservice.service;

import com.schemebridge.documentservice.client.ApplicationServiceClient;
import com.schemebridge.documentservice.client.NotificationServiceClient;
import com.schemebridge.documentservice.document.DocumentMetadata;
import com.schemebridge.documentservice.dto.DocumentVerifyRequest;
import com.schemebridge.documentservice.enums.VerificationStatus;
import com.schemebridge.documentservice.repository.DocumentRepository;
import com.schemebridge.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DocumentVerificationService {

    private final DocumentRepository documentRepository;
    private final DocumentHistoryService historyService;
    private final NotificationServiceClient notificationClient;
    private final ApplicationServiceClient applicationClient;

    public DocumentMetadata verifyDocument(String id, DocumentVerifyRequest request) {
        DocumentMetadata metadata = documentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        metadata.setVerificationStatus(VerificationStatus.VERIFIED);
        metadata.setVerifiedBy(request.getVerifiedBy());
        metadata.setVerifiedByName(request.getVerifiedByName());
        metadata.setVerifiedAt(LocalDateTime.now());
        metadata.setRemarks(request.getRemarks());
        metadata.setUpdatedAt(LocalDateTime.now());

        historyService.addAuditEntry(metadata, "VERIFY", request.getVerifiedBy(),
                request.getVerifiedByName(), "127.0.0.1", request.getRemarks());

        DocumentMetadata saved = documentRepository.save(metadata);

        // Call placeholders
        notificationClient.notifyDocumentVerified(saved.getAuthUserId(), saved.getId(), saved.getDocumentType().name());
        if (saved.getApplicationId() != null) {
            applicationClient.updateApplicationDocumentStatus(saved.getApplicationId(), saved.getId(), "VERIFIED", request.getRemarks());
        }

        return saved;
    }

    public DocumentMetadata rejectDocument(String id, DocumentVerifyRequest request) {
        DocumentMetadata metadata = documentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        metadata.setVerificationStatus(VerificationStatus.REJECTED);
        metadata.setVerifiedBy(request.getVerifiedBy());
        metadata.setVerifiedByName(request.getVerifiedByName());
        metadata.setVerifiedAt(LocalDateTime.now());
        metadata.setRejectionReason(request.getRejectionReason() != null ? request.getRejectionReason() : request.getRemarks());
        metadata.setRemarks(request.getRemarks());
        metadata.setUpdatedAt(LocalDateTime.now());

        historyService.addAuditEntry(metadata, "REJECT", request.getVerifiedBy(),
                request.getVerifiedByName(), "127.0.0.1", request.getRejectionReason());

        DocumentMetadata saved = documentRepository.save(metadata);

        // Call placeholders
        notificationClient.notifyDocumentRejected(saved.getAuthUserId(), saved.getId(), saved.getDocumentType().name(), saved.getRejectionReason());
        if (saved.getApplicationId() != null) {
            applicationClient.updateApplicationDocumentStatus(saved.getApplicationId(), saved.getId(), "REJECTED", saved.getRejectionReason());
        }

        return saved;
    }
}
