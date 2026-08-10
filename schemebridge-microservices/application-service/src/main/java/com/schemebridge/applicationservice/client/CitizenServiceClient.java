package com.schemebridge.applicationservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * CitizenServiceClient — placeholder for citizen-service REST integration.
 *
 * CURRENT STATE: Stub implementation (returns empty/mock data).
 * FUTURE STATE: Will call http://localhost:8082/api/v1/citizen/{authUserId}/profile
 *               using RestTemplate or WebClient.
 *
 * Integration Trigger: When Document Service validates a document, it calls back
 * the Application Service. Similarly, when an application is created,
 * this client fetches the citizen's profile and builds an EligibilitySnapshot.
 */
@Component
@Slf4j
public class CitizenServiceClient {

    @Value("${integration.citizen-service.url}")
    private String citizenServiceUrl;

    /**
     * Fetch citizen profile summary for EligibilitySnapshot capture.
     * Returns a map of profile fields. Empty if citizen-service is unavailable.
     *
     * @param authUserId the auth user ID (JWT subject)
     * @return Optional map of citizen profile fields, or empty if unavailable
     */
    public Optional<Map<String, Object>> getCitizenProfile(String authUserId) {
        log.info("[CitizenServiceClient] STUB – would call GET {}/api/v1/citizen/{}/profile",
                citizenServiceUrl, authUserId);
        // TODO: Implement with RestTemplate/WebClient
        // String url = citizenServiceUrl + "/api/v1/citizen/" + authUserId + "/profile";
        // ResponseEntity<ApiResponse<CitizenProfileResponse>> response =
        //     restTemplate.exchange(url, HttpMethod.GET, null, ...);
        return Optional.empty();
    }

    /**
     * Fetch citizen's saved schemes and preferences.
     */
    public Optional<Map<String, Object>> getCitizenPreferences(String authUserId) {
        log.info("[CitizenServiceClient] STUB – would call GET {}/api/v1/citizen/{}/preferences",
                citizenServiceUrl, authUserId);
        return Optional.empty();
    }
}
