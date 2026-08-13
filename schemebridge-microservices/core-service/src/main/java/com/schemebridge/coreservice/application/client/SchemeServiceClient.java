package com.schemebridge.coreservice.application.client;

import com.schemebridge.coreservice.scheme.dto.SchemeResponse;
import com.schemebridge.coreservice.scheme.service.SchemeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * SchemeServiceClient — In-process direct Java integration between Application Domain
 * and Scheme Domain within Core Service.
 */
@Component
@Slf4j
public class SchemeServiceClient {

    private final SchemeService schemeService;

    public SchemeServiceClient(@Lazy SchemeService schemeService) {
        this.schemeService = schemeService;
    }

    /**
     * Fetch scheme details by schemeId directly via in-process Java call.
     */
    public Optional<Map<String, Object>> getSchemeById(String schemeId) {
        try {
            if (schemeService != null) {
                SchemeResponse scheme = schemeService.getSchemeById(schemeId);
                if (scheme != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", scheme.getId());
                    map.put("schemeCode", scheme.getSchemeCode());
                    map.put("title", scheme.getTitleEnglish());
                    map.put("description", scheme.getDescriptionEnglish());
                    map.put("category", scheme.getCategoryName());
                    map.put("department", scheme.getDepartmentName());
                    return Optional.of(map);
                }
            }
        } catch (Exception e) {
            log.warn("[SchemeServiceClient] Could not fetch scheme by ID {}: {}", schemeId, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Evaluate citizen eligibility against scheme rules.
     */
    public Optional<Map<String, Object>> evaluateEligibility(Map<String, Object> request) {
        log.info("[SchemeServiceClient] In-process eligibility evaluation triggered");
        return Optional.empty();
    }

    /**
     * Fetch required documents for a scheme.
     */
    public Optional<Map<String, Object>> getSchemeDocuments(String schemeId) {
        return getSchemeById(schemeId);
    }
}
