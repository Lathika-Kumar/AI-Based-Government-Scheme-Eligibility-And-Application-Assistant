package com.schemebridge.notificationservice.strategy;

import com.schemebridge.notificationservice.constants.NotificationConstants;
import com.schemebridge.notificationservice.entity.Notification;
import com.schemebridge.notificationservice.enums.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InAppNotificationStrategy implements NotificationProviderStrategy {

    @Override
    public boolean supports(Channel channel) {
        return Channel.IN_APP.equals(channel);
    }

    @Override
    public boolean send(Notification notification) {
        log.info("[IN-APP STRATEGY] Notification {} saved to inbox for user {}", notification.getNotificationId(), notification.getAuthUserId());
        return true;
    }

    @Override
    public String getProviderName() {
        return NotificationConstants.PROVIDER_IN_APP;
    }
}
