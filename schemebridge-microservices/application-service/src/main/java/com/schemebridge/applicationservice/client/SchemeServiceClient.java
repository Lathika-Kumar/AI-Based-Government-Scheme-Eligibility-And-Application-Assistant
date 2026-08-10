package com.schemebridge.applicationservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * SchemeServiceClient — placeholder for scheme-service REST integration.
 *
 * CURRENT STATE: Stub implementation.
 * FUTURE STATE: Will call:
 *   GET  http://localhost:8083/api/v1/schemes/{schemeId}
 *   POST http://localhost:8083/api/v1/schemes/evaluate-eligibility
 *
 * This client is used to:
 * 1. Fetch scheme details (name, department, documents required)
 * 2. Evaluate citizen eligibility at application creation time
 */
@Component
@Slf4j
public class SchemeServiceClient {

    @Value("${integration.scheme-service.url}")
    private String schemeServiceUrl;

    /**
     * Fetch scheme details by schemeId.
     */
    public Optional<Map<String, Object>> getSchemeById(String schemeId) {
        log.info("[SchemeServiceClient] STUB – would call GET {}/api/v1/schemes/{}",
                schemeServiceUrl, schemeId);
        // TODO: Implement with RestTemplate
        // String url = schemeServiceUrl + "/api/v1/schemes/" + schemeId;
        return Optional.empty();
    }

    /**
     * Evaluate citizen eligibility against scheme rules.
     */
    public Optional<Map<String, Object>> evaluateEligibility(Map<String, Object> request) {
        log.info("[SchemeServiceClient] STUB – would call POST {}/api/v1/schemes/evaluate-eligibility",
                schemeServiceUrl);
        // TODO: POST to scheme-service EligibilityEvaluationRequest
        return Optional.empty();
    }

    /**
     * Fetch required documents for a scheme.
     */
    public Optional<Map<String, Object>> getSchemeDocuments(String schemeId) {
        log.info("[SchemeServiceClient] STUB – would call GET {}/api/v1/schemes/{}/documents",
                schemeServiceUrl, schemeId);
        return Optional.empty();
    }
}
