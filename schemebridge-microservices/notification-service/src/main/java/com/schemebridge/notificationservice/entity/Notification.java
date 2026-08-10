package com.schemebridge.notificationservice.entity;

import com.schemebridge.notificationservice.enums.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;

@Document(collection = "notifications")
@CompoundIndexes({
    @CompoundIndex(name = "idx_user_read", def = "{'authUserId': 1, 'readStatus': 1}"),
    @CompoundIndex(name = "idx_user_delivery", def = "{'authUserId': 1, 'deliveryStatus': 1}"),
    @CompoundIndex(name = "idx_priority_created", def = "{'priority': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_ref_type_id", def = "{'referenceType': 1, 'referenceId': 1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String notificationId;

    @Indexed
    private String authUserId;

    @Field(targetType = FieldType.STRING)
    private NotificationType notificationType;

    @Field(targetType = FieldType.STRING)
    private Channel channel;

    private String subject;
    private String title;
    private String message;
    private String htmlContent;

    private String recipientEmail;
    private String recipientPhone;

    private String referenceId;

    @Field(targetType = FieldType.STRING)
    private ReferenceType referenceType;

    @Builder.Default
    @Field(targetType = FieldType.STRING)
    private Priority priority = Priority.NORMAL;

    @Builder.Default
    @Field(targetType = FieldType.STRING)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @Builder.Default
    @Field(targetType = FieldType.STRING)
    private ReadStatus readStatus = ReadStatus.UNREAD;

    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private Instant expiresAt;

    private String failureReason;

    @Builder.Default
    private Integer retryCount = 0;
}
