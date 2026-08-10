package com.schemebridge.notificationservice.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class FirebasePushProviderPlaceholder implements PushProvider {

    @Override
    public boolean sendPushNotification(String authUserId, String deviceToken, String title, String message, Map<String, String> dataPayload) {
        log.info("[SIMULATED FCM PUSH] Sending Push Notification to User: {}, Token: {}, Title: '{}', Message: '{}'",
                authUserId, deviceToken != null ? deviceToken : "ALL_DEVICES", title, message);
        // Firebase Cloud Messaging placeholder ready for production SDK injection
        return true;
    }

    @Override
    public String getProviderName() {
        return "FirebasePushProviderPlaceholder";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
