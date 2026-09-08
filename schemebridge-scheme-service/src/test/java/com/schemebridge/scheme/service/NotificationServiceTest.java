package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.Notification;
import com.schemebridge.scheme.document.NotificationType;
import com.schemebridge.scheme.dto.response.NotificationResponse;
import com.schemebridge.scheme.dto.response.PagedNotificationResponse;
import com.schemebridge.scheme.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id("notif-100")
                .recipientUserId("citizen-1")
                .type(NotificationType.APPLICATION_APPROVED)
                .title("Application Approved")
                .message("Your application has been approved.")
                .channel("IN_APP")
                .relatedEntityType("APPLICATION")
                .relatedEntityId("app-100")
                .read(false)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Fresh citizen with no notifications receives empty response and zero unread count")
    void testGetUserNotifications_FreshUser_ReturnsEmptyAndZeroUnread() {
        when(notificationRepository.findAllByRecipientUserIdOrderByCreatedAtDesc(eq("fresh-citizen"), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(notificationRepository.countByRecipientUserIdAndReadFalse("fresh-citizen"))
                .thenReturn(0L);

        PagedNotificationResponse response = notificationService.getUserNotifications("fresh-citizen", 0, 20);

        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getUnreadCount());
    }

    @Test
    @DisplayName("User notification query is strictly scoped to authenticated user ID")
    void testGetUserNotifications_ScopedToAuthenticatedUserId() {
        Page<Notification> page = new PageImpl<>(List.of(testNotification));
        when(notificationRepository.findAllByRecipientUserIdOrderByCreatedAtDesc(eq("citizen-1"), any(Pageable.class)))
                .thenReturn(page);
        when(notificationRepository.countByRecipientUserIdAndReadFalse("citizen-1"))
                .thenReturn(1L);

        PagedNotificationResponse response = notificationService.getUserNotifications("citizen-1", 0, 20);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("citizen-1", response.getContent().get(0).getRecipientUserId());
        assertEquals(1, response.getUnreadCount());
    }

    @Test
    @DisplayName("Notification creation explicitly sets recipient user ID")
    void testSendNotification_Success() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId("notif-new");
            return n;
        });

        Notification sent = notificationService.sendNotification(
                "citizen-1", null, NotificationType.APPLICATION_SUBMITTED,
                "Submitted", "Your application is submitted", "IN_APP",
                "APPLICATION", "app-100", "SYSTEM", Map.of()
        );

        assertNotNull(sent);
        assertEquals("notif-new", sent.getId());
        assertEquals("citizen-1", sent.getRecipientUserId());
    }

    @Test
    @DisplayName("Owner can successfully mark their notification as read")
    void testMarkAsRead_Success() {
        when(notificationRepository.findById("notif-100")).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        NotificationResponse response = notificationService.markAsRead("notif-100", "citizen-1", false);
        assertNotNull(response);
        assertTrue(response.isRead());
    }

    @Test
    @DisplayName("Citizen A cannot mark Citizen B's notification as read")
    void testMarkAsRead_Unauthorized() {
        when(notificationRepository.findById("notif-100")).thenReturn(Optional.of(testNotification));

        assertThrows(SecurityException.class, () ->
                notificationService.markAsRead("notif-100", "other-citizen", false));
    }

    @Test
    @DisplayName("Non-privileged citizen cannot mark admin notification (null recipient) as read")
    void testMarkAsRead_NullRecipient_NonPrivilegedThrowsSecurityException() {
        Notification adminNotif = Notification.builder()
                .id("notif-admin")
                .recipientRole("ROLE_ADMIN")
                .recipientUserId(null)
                .read(false)
                .build();

        when(notificationRepository.findById("notif-admin")).thenReturn(Optional.of(adminNotif));

        assertThrows(SecurityException.class, () ->
                notificationService.markAsRead("notif-admin", "citizen-1", false));
    }

    @Test
    @DisplayName("Privileged officer can mark admin notification as read")
    void testMarkAsRead_Admin_PrivilegedSuccess() {
        Notification adminNotif = Notification.builder()
                .id("notif-admin")
                .recipientRole("ROLE_ADMIN")
                .recipientUserId(null)
                .read(false)
                .build();

        when(notificationRepository.findById("notif-admin")).thenReturn(Optional.of(adminNotif));
        when(notificationRepository.save(any(Notification.class))).thenReturn(adminNotif);

        NotificationResponse response = notificationService.markAsRead("notif-admin", "admin-1", true);
        assertNotNull(response);
        assertTrue(response.isRead());
    }

    @Test
    @DisplayName("Citizen markAllAsRead is scoped exclusively to authenticated citizen's unread notifications")
    void testMarkAllAsRead_CitizenScoped() {
        when(notificationRepository.findAllByRecipientUserIdAndReadFalse("citizen-1"))
                .thenReturn(List.of(testNotification));

        notificationService.markAllAsRead("citizen-1", false);

        verify(notificationRepository, times(1)).saveAll(argThat(iterable -> {
            List<Notification> list = (List<Notification>) iterable;
            return list.size() == 1 && list.get(0).isRead() && "citizen-1".equals(list.get(0).getRecipientUserId());
        }));
    }

    @Test
    @DisplayName("Unread count is strictly scoped to authenticated citizen")
    void testGetUnreadCount_UserScoped() {
        when(notificationRepository.countByRecipientUserIdAndReadFalse("citizen-1")).thenReturn(3L);

        long unread = notificationService.getUnreadCount("citizen-1");
        assertEquals(3L, unread);
    }

    @Test
    void testRegisterSseEmitter() {
        SseEmitter emitter = notificationService.registerSseEmitter("admin-1", "ROLE_ADMIN");
        assertNotNull(emitter);
    }
}
