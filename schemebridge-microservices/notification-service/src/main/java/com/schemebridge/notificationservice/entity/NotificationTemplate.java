package com.schemebridge.notificationservice.entity;

import com.schemebridge.notificationservice.enums.Channel;
import com.schemebridge.notificationservice.enums.NotificationType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.util.HashMap;
import java.util.Map;

@Document(collection = "notification_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate extends BaseEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String templateId;

    private String templateName;

    @Indexed
    @Field(targetType = FieldType.STRING)
    private NotificationType notificationType;

    @Field(targetType = FieldType.STRING)
    private Channel channel;

    private String subjectTemplate;
    private String titleTemplate;
    private String bodyTemplate;
    private String htmlTemplate;

    @Builder.Default
    private Map<String, String> defaultVariables = new HashMap<>();
}
