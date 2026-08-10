package com.schemebridge.notificationservice.service;

import com.schemebridge.notificationservice.dto.PreferenceResponse;
import com.schemebridge.notificationservice.dto.PreferenceUpdateRequest;
import com.schemebridge.notificationservice.entity.NotificationPreference;
import com.schemebridge.notificationservice.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreferenceServiceImpl implements PreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Override
    public PreferenceResponse getPreferences(String authUserId) {
        NotificationPreference pref = preferenceRepository.findByAuthUserId(authUserId)
                .orElseGet(() -> createDefaultPreferences(authUserId));
        return mapToResponse(pref);
    }

    @Override
    public PreferenceResponse updatePreferences(String authUserId, PreferenceUpdateRequest request) {
        NotificationPreference pref = preferenceRepository.findByAuthUserId(authUserId)
                .orElseGet(() -> createDefaultPreferences(authUserId));

        if (request.getEmailEnabled() != null) pref.setEmailEnabled(request.getEmailEnabled());
        if (request.getSmsEnabled() != null) pref.setSmsEnabled(request.getSmsEnabled());
        if (request.getPushEnabled() != null) pref.setPushEnabled(request.getPushEnabled());
        if (request.getInAppEnabled() != null) pref.setInAppEnabled(request.getInAppEnabled());

        if (request.getMarketingNotifications() != null) pref.setMarketingNotifications(request.getMarketingNotifications());
        if (request.getSchemeAlerts() != null) pref.setSchemeAlerts(request.getSchemeAlerts());
        if (request.getApplicationUpdates() != null) pref.setApplicationUpdates(request.getApplicationUpdates());
        if (request.getDocumentReminders() != null) pref.setDocumentReminders(request.getDocumentReminders());
        if (request.getOtpMessages() != null) pref.setOtpMessages(request.getOtpMessages());
        if (request.getEmergencyNotifications() != null) pref.setEmergencyNotifications(request.getEmergencyNotifications());
        if (request.getLanguagePreference() != null) pref.setLanguagePreference(request.getLanguagePreference());

        NotificationPreference saved = preferenceRepository.save(pref);
        log.info("Updated notification preferences for user {}", authUserId);
        return mapToResponse(saved);
    }

    @Override
    public boolean isChannelEnabled(String authUserId, String channelName, String notificationType) {
        if (authUserId == null || authUserId.trim().isEmpty()) {
            return true;
        }
        NotificationPreference pref = preferenceRepository.findByAuthUserId(authUserId)
                .orElseGet(() -> createDefaultPreferences(authUserId));

        switch (channelName.toUpperCase()) {
            case "EMAIL": if (!Boolean.TRUE.equals(pref.getEmailEnabled())) return false; break;
            case "SMS": if (!Boolean.TRUE.equals(pref.getSmsEnabled())) return false; break;
            case "PUSH": if (!Boolean.TRUE.equals(pref.getPushEnabled())) return false; break;
            case "IN_APP": if (!Boolean.TRUE.equals(pref.getInAppEnabled())) return false; break;
        }

        if ("OTP".equalsIgnoreCase(notificationType) || "PASSWORD_RESET".equalsIgnoreCase(notificationType)) {
            return Boolean.TRUE.equals(pref.getOtpMessages());
        }
        if ("APPLICATION_STATUS".equalsIgnoreCase(notificationType)) {
            return Boolean.TRUE.equals(pref.getApplicationUpdates());
        }
        if ("NEW_SCHEME".equalsIgnoreCase(notificationType) || "SCHEME_MATCHED".equalsIgnoreCase(notificationType)) {
            return Boolean.TRUE.equals(pref.getSchemeAlerts());
        }

        return true;
    }

    private NotificationPreference createDefaultPreferences(String authUserId) {
        NotificationPreference defaultPref = NotificationPreference.builder()
                .authUserId(authUserId)
                .emailEnabled(true)
                .smsEnabled(true)
                .pushEnabled(true)
                .inAppEnabled(true)
                .marketingNotifications(false)
                .schemeAlerts(true)
                .applicationUpdates(true)
                .documentReminders(true)
                .otpMessages(true)
                .emergencyNotifications(true)
                .languagePreference("en")
                .build();
        return preferenceRepository.save(defaultPref);
    }

    private PreferenceResponse mapToResponse(NotificationPreference pref) {
        return PreferenceResponse.builder()
                .id(pref.getId())
                .authUserId(pref.getAuthUserId())
                .emailEnabled(pref.getEmailEnabled())
                .smsEnabled(pref.getSmsEnabled())
                .pushEnabled(pref.getPushEnabled())
                .inAppEnabled(pref.getInAppEnabled())
                .marketingNotifications(pref.getMarketingNotifications())
                .schemeAlerts(pref.getSchemeAlerts())
                .applicationUpdates(pref.getApplicationUpdates())
                .documentReminders(pref.getDocumentReminders())
                .otpMessages(pref.getOtpMessages())
                .emergencyNotifications(pref.getEmergencyNotifications())
                .languagePreference(pref.getLanguagePreference())
                .createdAt(pref.getCreatedAt())
                .updatedAt(pref.getUpdatedAt())
                .build();
    }
}
