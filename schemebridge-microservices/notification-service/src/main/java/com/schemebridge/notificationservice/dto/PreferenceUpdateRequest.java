package com.schemebridge.notificationservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenceUpdateRequest {

    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean pushEnabled;
    private Boolean inAppEnabled;

    private Boolean marketingNotifications;
    private Boolean schemeAlerts;
    private Boolean applicationUpdates;
    private Boolean documentReminders;
    private Boolean otpMessages;
    private Boolean emergencyNotifications;

    private String languagePreference;
}
