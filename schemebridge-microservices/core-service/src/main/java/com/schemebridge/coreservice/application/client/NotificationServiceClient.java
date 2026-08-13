package com.schemebridge.coreservice.application.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * NotificationServiceClient — placeholder for notification-service (port 8086) REST integration.
 *
 * CURRENT STATE: Stub/log-only implementation.
 * FUTURE STATE: Will publish events to notification-service via:
 *   POST http://localhost:8086/api/v1/notifications/publish
 *   OR via message queue (Kafka/RabbitMQ) for event-driven architecture.
 *
 * Events published:
 *   - APPLICATION_SUBMITTED    → SMS + Email to citizen
 *   - DOCUMENT_PENDING         → SMS reminder with list of missing docs
 *   - APPLICATION_APPROVED     → SMS + Email congratulations
 *   - APPLICATION_REJECTED     → SMS + Email with rejection reason
 *   - BENEFIT_RELEASED         → SMS + Email with benefit details
 *   - APPLICATION_WITHDRAWN    → Confirmation notification
 */
@Component("applicationNotificationServiceClient")
@Slf4j
public class NotificationServiceClient {

    @Value("${integration.notification-service.url}")
    private String notificationServiceUrl;

    public void notifyApplicationSubmitted(String authUserId, String applicationNumber) {
        log.info("[NotificationServiceClient] STUB – APPLICATION_SUBMITTED: user={}, app={}",
                authUserId, applicationNumber);
        // TODO: POST to notification-service when Module 9 is ready
    }

    public void notifyApplicationApproved(String authUserId, String applicationNumber, String benefitDetails) {
        log.info("[NotificationServiceClient] STUB – APPLICATION_APPROVED: user={}, app={}",
                authUserId, applicationNumber);
    }

    public void notifyApplicationRejected(String authUserId, String applicationNumber, String reason) {
        log.info("[NotificationServiceClient] STUB – APPLICATION_REJECTED: user={}, app={}, reason={}",
                authUserId, applicationNumber, reason);
    }

    public void notifyDocumentPending(String authUserId, String applicationNumber) {
        log.info("[NotificationServiceClient] STUB – DOCUMENT_PENDING: user={}, app={}",
                authUserId, applicationNumber);
    }

    public void notifyBenefitReleased(String authUserId, String applicationNumber) {
        log.info("[NotificationServiceClient] STUB – BENEFIT_RELEASED: user={}, app={}",
                authUserId, applicationNumber);
    }

    public void notifyApplicationWithdrawn(String authUserId, String applicationNumber) {
        log.info("[NotificationServiceClient] STUB – APPLICATION_WITHDRAWN: user={}, app={}",
                authUserId, applicationNumber);
    }
}
