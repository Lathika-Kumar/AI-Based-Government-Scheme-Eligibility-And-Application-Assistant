package com.schemebridge.notificationservice.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDashboardResponse {

    private Long totalNotifications;
    private Long unreadCount;
    private Long readCount;
    private Long pendingDeliveryCount;
    private Long failedDeliveryCount;

    private Map<String, Long> countByChannel;
    private Map<String, Long> countByType;
}
