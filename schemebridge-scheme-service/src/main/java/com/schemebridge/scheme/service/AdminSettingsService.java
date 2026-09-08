package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.AdminSettings;
import com.schemebridge.scheme.dto.request.UpdateAdminSettingsRequest;
import com.schemebridge.scheme.dto.response.AdminSettingsResponse;
import com.schemebridge.scheme.repository.AdminSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSettingsService {

    private final AdminSettingsRepository adminSettingsRepository;
    private final AdminAuditService adminAuditService;

    @Transactional(readOnly = true)
    public AdminSettingsResponse getSettings() {
        AdminSettings settings = adminSettingsRepository.findById("DEFAULT_SETTINGS")
                .orElseGet(() -> adminSettingsRepository.save(AdminSettings.builder().id("DEFAULT_SETTINGS").build()));
        return toResponse(settings);
    }

    @Transactional
    public AdminSettingsResponse updateSettings(UpdateAdminSettingsRequest request, String actorId, String actorRole) {
        AdminSettings settings = adminSettingsRepository.findById("DEFAULT_SETTINGS")
                .orElseGet(() -> AdminSettings.builder().id("DEFAULT_SETTINGS").build());

        AdminSettings before = AdminSettings.builder()
                .emailNotificationsEnabled(settings.isEmailNotificationsEnabled())
                .applicationSlaDays(settings.getApplicationSlaDays())
                .maxUploadSizeBytes(settings.getMaxUploadSizeBytes())
                .allowedMimeTypes(settings.getAllowedMimeTypes())
                .enableSseAlerts(settings.isEnableSseAlerts())
                .maintenanceMode(settings.isMaintenanceMode())
                .build();

        if (request.getEmailNotificationsEnabled() != null) {
            settings.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        }
        if (request.getApplicationSlaDays() != null && request.getApplicationSlaDays() > 0) {
            settings.setApplicationSlaDays(request.getApplicationSlaDays());
        }
        if (request.getMaxUploadSizeBytes() != null && request.getMaxUploadSizeBytes() > 0) {
            settings.setMaxUploadSizeBytes(request.getMaxUploadSizeBytes());
        }
        if (request.getAllowedMimeTypes() != null && !request.getAllowedMimeTypes().isEmpty()) {
            settings.setAllowedMimeTypes(request.getAllowedMimeTypes());
        }
        if (request.getEnableSseAlerts() != null) {
            settings.setEnableSseAlerts(request.getEnableSseAlerts());
        }
        if (request.getMaintenanceMode() != null) {
            settings.setMaintenanceMode(request.getMaintenanceMode());
        }

        settings.setUpdatedBy(actorId);
        settings.setUpdatedAt(Instant.now());
        AdminSettings saved = adminSettingsRepository.save(settings);

        adminAuditService.recordAction(actorId, actorRole, "SETTINGS_UPDATED", "ADMIN_SETTINGS", "DEFAULT_SETTINGS",
                before, saved, null, null, Map.of("updatedBy", actorId));

        log.info("Updated administrative platform settings by actor={}", actorId);
        return toResponse(saved);
    }

    private AdminSettingsResponse toResponse(AdminSettings s) {
        return AdminSettingsResponse.builder()
                .id(s.getId())
                .emailNotificationsEnabled(s.isEmailNotificationsEnabled())
                .applicationSlaDays(s.getApplicationSlaDays())
                .maxUploadSizeBytes(s.getMaxUploadSizeBytes())
                .allowedMimeTypes(s.getAllowedMimeTypes())
                .enableSseAlerts(s.isEnableSseAlerts())
                .maintenanceMode(s.isMaintenanceMode())
                .updatedBy(s.getUpdatedBy())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
