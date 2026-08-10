package com.schemebridge.notificationservice.dto;

import com.schemebridge.notificationservice.enums.NotificationType;
import com.schemebridge.notificationservice.enums.Priority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotificationRequest {

    private String authUserId;

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Valid email address is required")
    private String recipientEmail;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String message;
    private String htmlContent;

    @Builder.Default
    private NotificationType notificationType = NotificationType.GENERAL;

    @Builder.Default
    private Priority priority = Priority.NORMAL;

    private String referenceId;
    private Map<String, String> templateVariables;
}
