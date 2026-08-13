package com.schemebridge.coreservice.document.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * NotificationServiceClient — placeholder for notification-service (port 8086) REST integration.
 *
 * Triggered when a document status changes to VERIFIED or REJECTED.
 */
@Component("documentNotificationServiceClient")
@Slf4j
public class NotificationServiceClient {

    @Value("${integration.notification-service.url:http://localhost:8086}")
    private String notificationServiceUrl;

    public void notifyDocumentVerified(String authUserId, String documentId, String documentType) {
        log.info("[NotificationServiceClient] STUB – DOCUMENT_VERIFIED: user={}, doc={}, type={}",
                authUserId, documentId, documentType);
    }

    public void notifyDocumentRejected(String authUserId, String documentId, String documentType, String reason) {
        log.info("[NotificationServiceClient] STUB – DOCUMENT_REJECTED: user={}, doc={}, type={}, reason={}",
                authUserId, documentId, documentType, reason);
    }
}
