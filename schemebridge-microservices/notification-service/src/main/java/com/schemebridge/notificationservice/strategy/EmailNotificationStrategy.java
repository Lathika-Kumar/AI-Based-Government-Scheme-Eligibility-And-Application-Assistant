package com.schemebridge.notificationservice.strategy;

import com.schemebridge.notificationservice.entity.Notification;
import com.schemebridge.notificationservice.enums.Channel;
import com.schemebridge.notificationservice.provider.EmailProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationStrategy implements NotificationProviderStrategy {

    private final EmailProvider emailProvider;

    @Override
    public boolean supports(Channel channel) {
        return Channel.EMAIL.equals(channel);
    }

    @Override
    public boolean send(Notification notification) {
        String recipient = notification.getRecipientEmail() != null ? notification.getRecipientEmail() : "citizen@schemebridge.gov.in";
        return emailProvider.sendEmail(recipient, notification.getSubject(), notification.getMessage(), notification.getHtmlContent());
    }

    @Override
    public String getProviderName() {
        return emailProvider.getProviderName();
    }
}
