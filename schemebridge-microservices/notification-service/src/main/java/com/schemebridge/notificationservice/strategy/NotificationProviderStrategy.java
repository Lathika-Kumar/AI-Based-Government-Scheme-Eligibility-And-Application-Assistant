package com.schemebridge.notificationservice.strategy;

import com.schemebridge.notificationservice.entity.Notification;
import com.schemebridge.notificationservice.enums.Channel;

/**
 * Strategy Pattern interface defining the provider dispatch contract for notification channels.
 */
public interface NotificationProviderStrategy {

    boolean supports(Channel channel);

    boolean send(Notification notification);

    String getProviderName();
}
