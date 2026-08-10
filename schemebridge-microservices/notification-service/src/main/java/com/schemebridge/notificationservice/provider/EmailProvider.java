package com.schemebridge.notificationservice.provider;

public interface EmailProvider {

    boolean sendEmail(String toEmail, String subject, String textContent, String htmlContent);

    String getProviderName();

    boolean isAvailable();
}
