package com.schemebridge.notificationservice.event;

import com.schemebridge.notificationservice.enums.DeliveryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publisher component emitting notification lifecycle events. Enables zero-code-change
 * future bridging to Kafka, RabbitMQ, or Redis Pub/Sub.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishNotificationEvent(String notificationId, String authUserId, String channel, DeliveryStatus status) {
        log.debug("Publishing Notification Lifecycle Event: id={}, user={}, status={}", notificationId, authUserId, status);
        NotificationEvent event = new NotificationEvent(this, notificationId, authUserId, channel, status);
        applicationEventPublisher.publishEvent(event);
    }
}
