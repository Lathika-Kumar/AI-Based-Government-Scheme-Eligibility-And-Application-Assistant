package com.schemebridge.coreservice.document.client;

import com.schemebridge.coreservice.citizen.dto.CitizenProfileResponse;
import com.schemebridge.coreservice.citizen.service.CitizenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * CitizenServiceClient — In-process direct Java integration between Document Domain
 * and Citizen Domain within Core Service.
 */
@Component("documentCitizenServiceClient")
@Slf4j
public class CitizenServiceClient {

    private final CitizenService citizenService;

    public CitizenServiceClient(@Lazy CitizenService citizenService) {
        this.citizenService = citizenService;
    }

    public Optional<Map<String, Object>> getCitizenProfile(String authUserId) {
        try {
            if (citizenService != null) {
                CitizenProfileResponse profile = citizenService.getProfileByAuthUserId(authUserId);
                if (profile != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", profile.getId());
                    map.put("authUserId", profile.getAuthUserId());
                    return Optional.of(map);
                }
            }
        } catch (Exception e) {
            log.warn("[Document.CitizenServiceClient] Could not fetch profile for authUserId {}: {}", authUserId, e.getMessage());
        }
        return Optional.empty();
    }
}
