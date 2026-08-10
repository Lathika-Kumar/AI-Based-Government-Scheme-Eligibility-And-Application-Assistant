package com.schemebridge.notificationservice.entity;

import com.schemebridge.notificationservice.enums.Channel;
import com.schemebridge.notificationservice.enums.NotificationType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Entity implementing the Transactional Outbox Pattern for eventual consistency
 * and future event streaming via Kafka/RabbitMQ.
 */
@Document(collection = "notification_outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationOutbox extends BaseEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String outboxId;

    @Indexed
    private String notificationId;

    @Indexed
    private String authUserId;

    private NotificationType notificationType;
    private Channel channel;
    private String payloadJson;

    @Indexed
    @Builder.Default
    private Boolean processed = false;

    private Instant processedAt;
}
