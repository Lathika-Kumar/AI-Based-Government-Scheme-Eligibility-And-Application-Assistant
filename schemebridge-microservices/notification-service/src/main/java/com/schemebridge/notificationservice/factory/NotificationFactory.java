package com.schemebridge.notificationservice.factory;

import com.schemebridge.notificationservice.constants.NotificationConstants;
import com.schemebridge.notificationservice.dto.SendNotificationRequest;
import com.schemebridge.notificationservice.entity.Notification;
import com.schemebridge.notificationservice.enums.DeliveryStatus;
import com.schemebridge.notificationservice.enums.Priority;
import com.schemebridge.notificationservice.enums.ReadStatus;
import com.schemebridge.notificationservice.enums.ReferenceType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Factory Pattern implementation for constructing Notification domain entities
 * with standardized IDs, default states, and fallback values.
 */
@Component
public class NotificationFactory {

    public Notification createNotification(SendNotificationRequest request, String subject, String title, String message, String htmlContent) {
        return Notification.builder()
                .notificationId(generateNotificationId())
                .authUserId(request.getAuthUserId())
                .notificationType(request.getNotificationType())
                .channel(request.getChannel())
                .subject(subject != null ? subject : "Notification Alert")
                .title(title != null ? title : "Notification Alert")
                .message(message != null ? message : "")
                .htmlContent(htmlContent)
                .recipientEmail(request.getRecipientEmail())
                .recipientPhone(request.getRecipientPhone())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType() != null ? request.getReferenceType() : ReferenceType.SYSTEM)
                .priority(request.getPriority() != null ? request.getPriority() : Priority.NORMAL)
                .deliveryStatus(DeliveryStatus.PENDING)
                .readStatus(ReadStatus.UNREAD)
                .retryCount(0)
                .build();
    }

    public Notification createSuppressedNotification(SendNotificationRequest request, String reason) {
        return Notification.builder()
                .notificationId(generateNotificationId())
                .authUserId(request.getAuthUserId())
                .notificationType(request.getNotificationType())
                .channel(request.getChannel())
                .subject(request.getSubject() != null ? request.getSubject() : "Suppressed Notification")
                .title(request.getTitle() != null ? request.getTitle() : "Suppressed Notification")
                .message(request.getMessage() != null ? request.getMessage() : "")
                .htmlContent(request.getHtmlContent())
                .recipientEmail(request.getRecipientEmail())
                .recipientPhone(request.getRecipientPhone())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType() != null ? request.getReferenceType() : ReferenceType.SYSTEM)
                .priority(request.getPriority() != null ? request.getPriority() : Priority.NORMAL)
                .deliveryStatus(DeliveryStatus.FAILED)
                .readStatus(ReadStatus.UNREAD)
                .failureReason(reason)
                .build();
    }

    private String generateNotificationId() {
        return NotificationConstants.NOTIFICATION_ID_PREFIX + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
    }
}
