package com.schemebridge.notificationservice.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenceResponse {

    private String id;
    private String authUserId;

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
    private Instant createdAt;
    private Instant updatedAt;
}
