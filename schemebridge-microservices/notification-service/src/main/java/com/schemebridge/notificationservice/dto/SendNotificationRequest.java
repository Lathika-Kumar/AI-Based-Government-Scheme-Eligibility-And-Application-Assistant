package com.schemebridge.notificationservice.dto;

import com.schemebridge.notificationservice.enums.Channel;
import com.schemebridge.notificationservice.enums.NotificationType;
import com.schemebridge.notificationservice.enums.Priority;
import com.schemebridge.notificationservice.enums.ReferenceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendNotificationRequest {

    @NotBlank(message = "authUserId is required")
    private String authUserId;

    @NotNull(message = "notificationType is required")
    private NotificationType notificationType;

    @NotNull(message = "channel is required")
    private Channel channel;

    private String subject;
    private String title;
    private String message;
    private String htmlContent;

    private String recipientEmail;
    private String recipientPhone;

    private String referenceId;
    private ReferenceType referenceType;

    @Builder.Default
    private Priority priority = Priority.NORMAL;

    private Map<String, String> templateVariables;
}
