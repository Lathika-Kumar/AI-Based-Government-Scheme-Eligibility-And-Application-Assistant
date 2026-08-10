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

@Document(collection = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog extends BaseEntity {

    @Id
    private String id;

    @Indexed
    private String notificationId;

    @Indexed
    private String authUserId;

    @Field(targetType = FieldType.STRING)
    private Channel channel;

    private String providerName;

    @Field(targetType = FieldType.STRING)
    private DeliveryStatus status;

    private Integer responseCode;
    private String responsePayload;
    private String failureReason;
    private Integer attemptNumber;
    private Instant timestamp;
}
