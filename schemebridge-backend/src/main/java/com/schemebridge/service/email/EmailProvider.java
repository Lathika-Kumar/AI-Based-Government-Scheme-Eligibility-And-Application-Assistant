package com.schemebridge.service.email;

public interface EmailProvider {
    void sendEmail(String recipient, String subject, String htmlBody, String textBody);
}
