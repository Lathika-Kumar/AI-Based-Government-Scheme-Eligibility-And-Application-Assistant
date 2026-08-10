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
public class SmsNotificationRequest {

    private String authUserId;

    @NotBlank(message = "Recipient phone number is required")
    private String recipientPhone;

    @NotBlank(message = "SMS message text is required")
    private String message;

    @Builder.Default
    private NotificationType notificationType = NotificationType.GENERAL;

    @Builder.Default
    private Priority priority = Priority.NORMAL;

    private String referenceId;
    private Map<String, String> templateVariables;
}
