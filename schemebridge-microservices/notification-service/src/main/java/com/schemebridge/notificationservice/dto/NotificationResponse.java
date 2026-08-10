package com.schemebridge.notificationservice.dto;

import com.schemebridge.notificationservice.enums.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private String id;
    private String notificationId;
    private String authUserId;
    private NotificationType notificationType;
    private Channel channel;
    private String subject;
    private String title;
    private String message;
    private String htmlContent;
    private String recipientEmail;
    private String recipientPhone;
    private String referenceId;
    private ReferenceType referenceType;
    private Priority priority;
    private DeliveryStatus deliveryStatus;
    private ReadStatus readStatus;
    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private Instant expiresAt;
    private String failureReason;
    private Integer retryCount;
    private Instant createdAt;
    private Instant updatedAt;
}
