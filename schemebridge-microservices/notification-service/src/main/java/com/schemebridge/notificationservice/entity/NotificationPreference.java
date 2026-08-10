package com.schemebridge.notificationservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference extends BaseEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String authUserId;

    @Builder.Default
    private Boolean emailEnabled = true;

    @Builder.Default
    private Boolean smsEnabled = true;

    @Builder.Default
    private Boolean pushEnabled = true;

    @Builder.Default
    private Boolean inAppEnabled = true;

    @Builder.Default
    private Boolean marketingNotifications = false;

    @Builder.Default
    private Boolean schemeAlerts = true;

    @Builder.Default
    private Boolean applicationUpdates = true;

    @Builder.Default
    private Boolean documentReminders = true;

    @Builder.Default
    private Boolean otpMessages = true;

    @Builder.Default
    private Boolean emergencyNotifications = true;

    @Builder.Default
    private String languagePreference = "en";
}
