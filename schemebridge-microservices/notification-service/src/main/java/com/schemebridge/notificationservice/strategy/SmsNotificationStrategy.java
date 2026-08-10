package com.schemebridge.notificationservice.strategy;

import com.schemebridge.notificationservice.entity.Notification;
import com.schemebridge.notificationservice.enums.Channel;
import com.schemebridge.notificationservice.provider.SmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationStrategy implements NotificationProviderStrategy {

    private final SmsProvider smsProvider;

    @Override
    public boolean supports(Channel channel) {
        return Channel.SMS.equals(channel);
    }

    @Override
    public boolean send(Notification notification) {
        String phone = notification.getRecipientPhone() != null ? notification.getRecipientPhone() : "+919876543210";
        return smsProvider.sendSms(phone, notification.getMessage());
    }

    @Override
    public String getProviderName() {
        return smsProvider.getProviderName();
    }
}
