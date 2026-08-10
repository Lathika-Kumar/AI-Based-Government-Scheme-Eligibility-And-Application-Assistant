package com.schemebridge.notificationservice.dto;

import com.schemebridge.notificationservice.enums.Channel;
import com.schemebridge.notificationservice.enums.NotificationType;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateResponse {

    private String id;
    private String templateId;
    private String templateName;
    private NotificationType notificationType;
    private Channel channel;
    private String subjectTemplate;
    private String titleTemplate;
    private String bodyTemplate;
    private String htmlTemplate;
    private Map<String, String> defaultVariables;
    private Instant createdAt;
    private Instant updatedAt;
}
