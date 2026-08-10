package com.schemebridge.notificationservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleNotificationRequest {

    @NotNull(message = "Notification request details are required")
    @Valid
    private SendNotificationRequest notificationRequest;

    @NotNull(message = "Scheduled execution time is required")
    @Future(message = "Scheduled time must be in the future")
    private Instant scheduledTime;
}
