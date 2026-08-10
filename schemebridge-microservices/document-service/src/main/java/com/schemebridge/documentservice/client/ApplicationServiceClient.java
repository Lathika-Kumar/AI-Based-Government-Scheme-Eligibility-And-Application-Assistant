package com.schemebridge.documentservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ApplicationServiceClient — placeholder for application-service (port 8084) REST integration.
 *
 * Triggered to update document verification status in an application's uploadedDocuments list.
 */
@Component
@Slf4j
public class ApplicationServiceClient {

    @Value("${integration.application-service.url:http://localhost:8084}")
    private String applicationServiceUrl;

    public void updateApplicationDocumentStatus(String applicationId, String documentId, String status, String remarks) {
        log.info("[ApplicationServiceClient] STUB – update app doc status: app={}, doc={}, status={}",
                applicationId, documentId, status);
    }
}
