package com.schemebridge.notificationservice.event;

import com.schemebridge.notificationservice.enums.DeliveryStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationEvent extends ApplicationEvent {

    private final String notificationId;
    private final String authUserId;
    private final String channel;
    private final DeliveryStatus status;

    public NotificationEvent(Object source, String notificationId, String authUserId, String channel, DeliveryStatus status) {
        super(source);
        this.notificationId = notificationId;
        this.authUserId = authUserId;
        this.channel = channel;
        this.status = status;
    }
}
