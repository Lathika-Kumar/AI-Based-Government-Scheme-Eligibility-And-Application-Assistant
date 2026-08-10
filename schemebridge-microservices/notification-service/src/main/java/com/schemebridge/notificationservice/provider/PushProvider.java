package com.schemebridge.notificationservice.provider;

import java.util.Map;

public interface PushProvider {

    boolean sendPushNotification(String authUserId, String deviceToken, String title, String message, Map<String, String> dataPayload);

    String getProviderName();

    boolean isAvailable();
}
