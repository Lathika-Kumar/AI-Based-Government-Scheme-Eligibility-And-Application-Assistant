package com.schemebridge.notificationservice.strategy;

import com.schemebridge.notificationservice.entity.Notification;
import com.schemebridge.notificationservice.enums.Channel;
import com.schemebridge.notificationservice.provider.PushProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PushNotificationStrategy implements NotificationProviderStrategy {

    private final PushProvider pushProvider;

    @Override
    public boolean supports(Channel channel) {
        return Channel.PUSH.equals(channel);
    }

    @Override
    public boolean send(Notification notification) {
        return pushProvider.sendPushNotification(notification.getAuthUserId(), null, notification.getTitle(), notification.getMessage(), null);
    }

    @Override
    public String getProviderName() {
        return pushProvider.getProviderName();
    }
}
