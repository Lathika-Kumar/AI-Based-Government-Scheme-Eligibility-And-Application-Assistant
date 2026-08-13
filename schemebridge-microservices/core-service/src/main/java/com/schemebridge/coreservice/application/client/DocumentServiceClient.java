package com.schemebridge.coreservice.application.client;

import com.schemebridge.coreservice.document.dto.DocumentResponse;
import com.schemebridge.coreservice.document.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * DocumentServiceClient — In-process direct Java integration between Application Domain
 * and Document Domain within Core Service.
 */
@Component
@Slf4j
public class DocumentServiceClient {

    private final DocumentService documentService;

    public DocumentServiceClient(@Lazy DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Fetch document metadata list by application ID directly via in-process Java call.
     */
    public List<DocumentResponse> getDocumentsByApplicationId(String applicationId) {
        try {
            if (documentService != null) {
                return documentService.getDocumentsByApplicationId(applicationId);
            }
        } catch (Exception e) {
            log.warn("[DocumentServiceClient] Could not fetch documents for application {}: {}", applicationId, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Fetch document details by document ID directly.
     */
    public Optional<DocumentResponse> getDocumentById(String documentId) {
        try {
            if (documentService != null) {
                return Optional.ofNullable(documentService.getDocumentById(documentId));
            }
        } catch (Exception e) {
            log.warn("[DocumentServiceClient] Could not fetch document by ID {}: {}", documentId, e.getMessage());
        }
        return Optional.empty();
    }
}
