package com.schemebridge.notificationservice.entity;

import com.schemebridge.notificationservice.enums.Channel;
import com.schemebridge.notificationservice.enums.NotificationType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Entity implementing the Dead Letter Queue (DLQ) pattern for failed notifications
 * that exceeded max retries. Supports admin replay and diagnostic investigation.
 */
@Document(collection = "notification_dlq")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDeadLetter extends BaseEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String dlqId;

    @Indexed
    private String notificationId;

    @Indexed
    private String authUserId;

    private NotificationType notificationType;
    private Channel channel;
    private String failureReason;
    private Integer totalAttempts;

    private String originalPayloadJson;
    private Instant failedAt;

    @Builder.Default
    private Boolean replayed = false;

    private Instant replayedAt;
}
