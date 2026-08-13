package com.schemebridge.coreservice.application.client;

import com.schemebridge.coreservice.citizen.dto.CitizenProfileResponse;
import com.schemebridge.coreservice.citizen.service.CitizenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * CitizenServiceClient — In-process direct Java integration between Application Domain
 * and Citizen Domain within Core Service.
 */
@Component("applicationCitizenServiceClient")
@Slf4j
public class CitizenServiceClient {

    private final CitizenService citizenService;

    public CitizenServiceClient(@Lazy CitizenService citizenService) {
        this.citizenService = citizenService;
    }

    /**
     * Fetch citizen profile summary directly via in-process Java service call.
     */
    public Optional<Map<String, Object>> getCitizenProfile(String authUserId) {
        try {
            if (citizenService != null) {
                CitizenProfileResponse profile = citizenService.getProfileByAuthUserId(authUserId);
                if (profile != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", profile.getId());
                    map.put("authUserId", profile.getAuthUserId());
                    map.put("personalDetails", profile.getPersonalDetails());
                    map.put("incomeDetails", profile.getIncomeDetails());
                    map.put("addressDetails", profile.getAddressDetails());
                    return Optional.of(map);
                }
            }
        } catch (Exception e) {
            log.warn("[CitizenServiceClient] Could not fetch profile for authUserId {}: {}", authUserId, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Fetch citizen's saved schemes and preferences.
     */
    public Optional<Map<String, Object>> getCitizenPreferences(String authUserId) {
        try {
            if (citizenService != null) {
                CitizenProfileResponse profile = citizenService.getProfileByAuthUserId(authUserId);
                if (profile != null && profile.getPreferences() != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("preferences", profile.getPreferences());
                    map.put("savedSchemes", profile.getSavedSchemes());
                    return Optional.of(map);
                }
            }
        } catch (Exception e) {
            log.warn("[CitizenServiceClient] Could not fetch preferences for authUserId {}: {}", authUserId, e.getMessage());
        }
        return Optional.empty();
    }
}
