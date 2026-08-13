package com.schemebridge.coreservice.document.service;

import com.schemebridge.common.event.BusinessEvents;
import com.schemebridge.common.event.DomainEventPublisher;
import com.schemebridge.coreservice.document.client.ApplicationServiceClient;
import com.schemebridge.coreservice.document.client.NotificationServiceClient;
import com.schemebridge.coreservice.document.model.DocumentMetadata;
import com.schemebridge.coreservice.document.dto.DocumentVerifyRequest;
import com.schemebridge.coreservice.document.enums.VerificationStatus;
import com.schemebridge.coreservice.document.repository.DocumentRepository;
import com.schemebridge.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentVerificationService {

    private final DocumentRepository documentRepository;
    private final DocumentHistoryService historyService;
    private final NotificationServiceClient notificationClient;
    private final ApplicationServiceClient applicationClient;
    private final DomainEventPublisher eventPublisher;

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

        // Publish DocumentVerifiedEvent
        try {
            String docName = saved.getDocumentName() != null ? saved.getDocumentName() :
                    (saved.getOriginalFileName() != null ? saved.getOriginalFileName() : "Document");
            String docType = saved.getDocumentType() != null ? saved.getDocumentType().name() : "IDENTITY";
            eventPublisher.publishEvent(BusinessEvents.createDocumentVerifiedEvent(
                    saved.getAuthUserId(), saved.getId(), docName, docType, saved.getApplicationId()));
        } catch (Exception e) {
            log.warn("[DocumentVerificationService] Failed to publish DocumentVerifiedEvent: {}", e.getMessage());
        }

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

        // Publish DocumentRejectedEvent
        try {
            String docName = saved.getDocumentName() != null ? saved.getDocumentName() :
                    (saved.getOriginalFileName() != null ? saved.getOriginalFileName() : "Document");
            String docType = saved.getDocumentType() != null ? saved.getDocumentType().name() : "IDENTITY";
            eventPublisher.publishEvent(BusinessEvents.createDocumentRejectedEvent(
                    saved.getAuthUserId(), saved.getId(), docName, docType, saved.getRejectionReason(), saved.getApplicationId()));
        } catch (Exception e) {
            log.warn("[DocumentVerificationService] Failed to publish DocumentRejectedEvent: {}", e.getMessage());
        }

        notificationClient.notifyDocumentRejected(saved.getAuthUserId(), saved.getId(), saved.getDocumentType().name(), saved.getRejectionReason());
        if (saved.getApplicationId() != null) {
            applicationClient.updateApplicationDocumentStatus(saved.getApplicationId(), saved.getId(), "REJECTED", saved.getRejectionReason());
        }

        return saved;
    }
}
