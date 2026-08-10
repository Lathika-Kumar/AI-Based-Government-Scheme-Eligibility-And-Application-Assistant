package com.schemebridge.notificationservice.dto;

import com.schemebridge.notificationservice.enums.Channel;
import com.schemebridge.notificationservice.enums.NotificationType;
import com.schemebridge.notificationservice.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastNotificationRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message content is required")
    private String message;

    private String htmlContent;

    @NotNull(message = "NotificationType is required")
    private NotificationType notificationType;

    @Builder.Default
    private Channel channel = Channel.IN_APP;

    @Builder.Default
    private Priority priority = Priority.NORMAL;

    private List<String> targetUserIds;
    private String targetRole;
}
