package com.schemebridge.notificationservice.dto;

import com.schemebridge.notificationservice.enums.NotificationType;
import com.schemebridge.notificationservice.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushNotificationRequest {

    @NotBlank(message = "authUserId is required")
    private String authUserId;

    private String deviceToken;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message body is required")
    private String message;

    @Builder.Default
    private NotificationType notificationType = NotificationType.GENERAL;

    @Builder.Default
    private Priority priority = Priority.NORMAL;

    private Map<String, String> dataPayload;
}
