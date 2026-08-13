package com.schemebridge.coreservice.document.client;

import com.schemebridge.coreservice.application.dto.ApplicationResponse;
import com.schemebridge.coreservice.application.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * ApplicationServiceClient — In-process direct Java integration between Document Domain
 * and Application Domain within Core Service.
 */
@Component
@Slf4j
public class ApplicationServiceClient {

    private final ApplicationService applicationService;

    public ApplicationServiceClient(@Lazy ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public Optional<Map<String, Object>> getApplicationById(String applicationId) {
        try {
            if (applicationService != null) {
                ApplicationResponse app = applicationService.getApplicationById(applicationId);
                if (app != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", app.getId());
                    map.put("applicationNumber", app.getApplicationNumber());
                    map.put("status", app.getApplicationStatus());
                    return Optional.of(map);
                }
            }
        } catch (Exception e) {
            log.warn("[Document.ApplicationServiceClient] Could not fetch application for ID {}: {}", applicationId, e.getMessage());
        }
        return Optional.empty();
    }

    public void updateApplicationDocumentStatus(String applicationId, String documentId, String status, String remarks) {
        log.info("[Document.ApplicationServiceClient] In-process document status update for application {}: docId={}, status={}, remarks={}",
                applicationId, documentId, status, remarks);
    }
}
