package com.schemebridge.notificationservice.service;

import com.schemebridge.common.event.BusinessEvents;
import com.schemebridge.common.event.DomainEvent;
import com.schemebridge.notificationservice.dto.NotificationResponse;
import com.schemebridge.notificationservice.enums.DeliveryStatus;
import com.schemebridge.notificationservice.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventIngestionServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private PreferenceService preferenceService;

    @InjectMocks
    private NotificationEventIngestionService eventIngestionService;

    @BeforeEach
    void setUp() {
        lenient().when(preferenceService.isChannelEnabled(anyString(), anyString(), anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("Process SchemeCreatedEvent - Should trigger notifications across enabled channels")
    void testProcessSchemeCreatedEvent() {
        DomainEvent event = BusinessEvents.createSchemeCreatedEvent(
                "SCH-100", "SCH001", "Farmers Welfare Grant", "Agriculture", List.of("citizen-001"));

        NotificationResponse mockResp = NotificationResponse.builder()
                .notificationId("NOTIF-1001")
                .authUserId("citizen-001")
                .notificationType(NotificationType.SCHEME_CREATED)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .build();

        when(notificationService.sendNotification(any())).thenReturn(mockResp);

        List<NotificationResponse> responses = eventIngestionService.processDomainEvent(event);

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertEquals("NOTIF-1001", responses.get(0).getNotificationId());
        verify(notificationService, atLeastOnce()).sendNotification(any());
    }

    @Test
    @DisplayName("Process ApplicationApprovedEvent - Should dispatch approved notification")
    void testProcessApplicationApprovedEvent() {
        DomainEvent event = BusinessEvents.createApplicationApprovedEvent(
                "citizen-001", "APP-500", "APP-2026-0001", "PM-KISAN", "₹6,000/year");

        NotificationResponse mockResp = NotificationResponse.builder()
                .notificationId("NOTIF-1002")
                .authUserId("citizen-001")
                .notificationType(NotificationType.APPLICATION_APPROVED)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .build();

        when(notificationService.sendNotification(any())).thenReturn(mockResp);

        List<NotificationResponse> responses = eventIngestionService.processDomainEvent(event);

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        verify(notificationService, atLeastOnce()).sendNotification(any());
    }

    @Test
    @DisplayName("Process ApplicationRejectedEvent - Should dispatch rejected notification with reason")
    void testProcessApplicationRejectedEvent() {
        DomainEvent event = BusinessEvents.createApplicationRejectedEvent(
                "citizen-001", "APP-501", "APP-2026-0002", "PM-KISAN", "Income criteria exceeded");

        NotificationResponse mockResp = NotificationResponse.builder()
                .notificationId("NOTIF-1003")
                .authUserId("citizen-001")
                .notificationType(NotificationType.APPLICATION_REJECTED)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .build();

        when(notificationService.sendNotification(any())).thenReturn(mockResp);

        List<NotificationResponse> responses = eventIngestionService.processDomainEvent(event);

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        verify(notificationService, atLeastOnce()).sendNotification(any());
    }

    @Test
    @DisplayName("Process DocumentRejectedEvent - Should dispatch document rejected notification")
    void testProcessDocumentRejectedEvent() {
        DomainEvent event = BusinessEvents.createDocumentRejectedEvent(
                "citizen-001", "DOC-900", "Income Certificate", "INCOME_PROOF", "Illegible text", "APP-501");

        NotificationResponse mockResp = NotificationResponse.builder()
                .notificationId("NOTIF-1004")
                .authUserId("citizen-001")
                .notificationType(NotificationType.DOCUMENT_REJECTED)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .build();

        when(notificationService.sendNotification(any())).thenReturn(mockResp);

        List<NotificationResponse> responses = eventIngestionService.processDomainEvent(event);

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        verify(notificationService, atLeastOnce()).sendNotification(any());
    }
}
