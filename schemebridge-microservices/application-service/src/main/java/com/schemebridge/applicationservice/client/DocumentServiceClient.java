package com.schemebridge.applicationservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * DocumentServiceClient — placeholder for document-service (port 8085) REST integration.
 *
 * CURRENT STATE: Stub implementation.
 * FUTURE STATE: Will call:
 *   POST http://localhost:8085/api/v1/documents/verify
 *   GET  http://localhost:8085/api/v1/documents/{documentId}/status
 *
 * Used to trigger document verification when a citizen uploads documents
 * and when the officer needs to review verification status.
 */
@Component
@Slf4j
public class DocumentServiceClient {

    @Value("${integration.document-service.url}")
    private String documentServiceUrl;

    public Optional<String> triggerVerification(String documentId, String applicationId) {
        log.info("[DocumentServiceClient] STUB – would POST {}/api/v1/documents/verify for doc={}, app={}",
                documentServiceUrl, documentId, applicationId);
        // TODO: Implement when Document Service (Module 8) is ready
        return Optional.empty();
    }

    public Optional<String> getVerificationStatus(String documentId) {
        log.info("[DocumentServiceClient] STUB – would call GET {}/api/v1/documents/{}/status",
                documentServiceUrl, documentId);
        return Optional.empty();
    }
}
