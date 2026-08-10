package com.schemebridge.notificationservice.provider;

public interface SmsProvider {

    boolean sendSms(String toPhone, String messageText);

    String getProviderName();

    boolean isAvailable();
}
