package com.schemebridge.documentservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * CitizenServiceClient — placeholder for citizen-service (port 8082) REST integration.
 *
 * Triggered to update citizen profile document readiness state.
 */
@Component
@Slf4j
public class CitizenServiceClient {

    @Value("${integration.citizen-service.url:http://localhost:8082}")
    private String citizenServiceUrl;

    public void updateDocumentReadiness(String authUserId, boolean ready, double score) {
        log.info("[CitizenServiceClient] STUB – update doc readiness: user={}, ready={}, score={}",
                authUserId, ready, score);
    }
}
