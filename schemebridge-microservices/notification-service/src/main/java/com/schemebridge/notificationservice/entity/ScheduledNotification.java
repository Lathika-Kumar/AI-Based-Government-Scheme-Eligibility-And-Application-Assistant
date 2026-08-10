package com.schemebridge.notificationservice.entity;

import com.schemebridge.notificationservice.enums.Channel;
import com.schemebridge.notificationservice.enums.DeliveryStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;

@Document(collection = "scheduled_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledNotification extends BaseEntity {

    @Id
    private String id;

    @Indexed
    private String notificationId;

    @Indexed
    private String authUserId;

    @Indexed
    private Instant scheduledTime;

    @Field(targetType = FieldType.STRING)
    private DeliveryStatus status;

    @Field(targetType = FieldType.STRING)
    private Channel targetChannel;

    private String requestPayload;
}
