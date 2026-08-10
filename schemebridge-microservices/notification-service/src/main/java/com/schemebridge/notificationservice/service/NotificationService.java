package com.schemebridge.notificationservice.service;

import com.schemebridge.notificationservice.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface NotificationService {

    NotificationResponse sendNotification(SendNotificationRequest request);

    NotificationResponse sendEmail(EmailNotificationRequest request);

    NotificationResponse sendSms(SmsNotificationRequest request);

    NotificationResponse sendPush(PushNotificationRequest request);

    NotificationResponse sendInApp(SendNotificationRequest request);

    List<NotificationResponse> broadcast(BroadcastNotificationRequest request);

    String scheduleNotification(ScheduleNotificationRequest request);

    Page<NotificationResponse> getMyNotifications(String authUserId, int page, int size);

    List<NotificationResponse> getUnreadNotifications(String authUserId);

    Long getUnreadCount(String authUserId);

    NotificationResponse markAsRead(String authUserId, String notificationId);

    NotificationResponse markAsUnread(String authUserId, String notificationId);

    void markAllAsRead(String authUserId);

    void deleteNotification(String authUserId, String notificationId);

    Page<NotificationResponse> searchNotifications(String authUserId, String query, int page, int size);

    Page<NotificationResponse> getNotificationHistory(String authUserId, int page, int size);

    NotificationDashboardResponse getDashboardSummary(String authUserId);

    void retryFailedNotifications();

    void executeScheduledNotifications();
}
