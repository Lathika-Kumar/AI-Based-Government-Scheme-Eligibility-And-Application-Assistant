package com.schemebridge.notificationservice.controller;

import com.schemebridge.common.dto.ApiResponse;
import com.schemebridge.notificationservice.dto.*;
import com.schemebridge.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "Email, SMS, Push, In-App, Scheduling, Broadcast, and History APIs")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/email")
    @Operation(summary = "Send Email Notification", description = "Dispatches an email notification using Brevo Email API or configured provider")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendEmail(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "SYSTEM") String authUserId,
            @Valid @RequestBody EmailNotificationRequest request) {
        if (request.getAuthUserId() == null) request.setAuthUserId(authUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Email notification sent successfully", notificationService.sendEmail(request)));
    }

    @PostMapping("/sms")
    @Operation(summary = "Send SMS Notification", description = "Dispatches an SMS notification via Twilio / SMS Gateway provider")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendSms(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "SYSTEM") String authUserId,
            @Valid @RequestBody SmsNotificationRequest request) {
        if (request.getAuthUserId() == null) request.setAuthUserId(authUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("SMS notification sent successfully", notificationService.sendSms(request)));
    }

    @PostMapping("/push")
    @Operation(summary = "Send Push Notification", description = "Dispatches a mobile push notification via FCM provider")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendPush(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "SYSTEM") String authUserId,
            @Valid @RequestBody PushNotificationRequest request) {
        if (request.getAuthUserId() == null) request.setAuthUserId(authUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Push notification sent successfully", notificationService.sendPush(request)));
    }

    @PostMapping("/inapp")
    @Operation(summary = "Send In-App Notification", description = "Creates an in-app alert notification in the citizen inbox")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendInApp(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "SYSTEM") String authUserId,
            @Valid @RequestBody SendNotificationRequest request) {
        if (request.getAuthUserId() == null) request.setAuthUserId(authUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("In-App notification created successfully", notificationService.sendInApp(request)));
    }

    @PostMapping("/broadcast")
    @Operation(summary = "Broadcast Notification", description = "Sends a system-wide or role-targeted broadcast notification to multiple citizens")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> broadcast(
            @Valid @RequestBody BroadcastNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Broadcast notifications processed", notificationService.broadcast(request)));
    }

    @PostMapping("/schedule")
    @Operation(summary = "Schedule Future Notification", description = "Schedules a notification for future automated execution")
    public ResponseEntity<ApiResponse<String>> scheduleNotification(
            @Valid @RequestBody ScheduleNotificationRequest request) {
        String scheduleId = notificationService.scheduleNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification scheduled successfully with ID: " + scheduleId, scheduleId));
    }

    @GetMapping("/my")
    @Operation(summary = "Get My Notifications", description = "Retrieves paginated notifications for the authenticated citizen")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "citizen-001") String authUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notificationService.getMyNotifications(authUserId, page, size)));
    }

    @GetMapping("/unread")
    @Operation(summary = "Get Unread Notifications", description = "Retrieves unread notifications list for authenticated citizen")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "citizen-001") String authUserId) {
        return ResponseEntity.ok(ApiResponse.success("Unread notifications retrieved", notificationService.getUnreadNotifications(authUserId)));
    }

    @GetMapping("/count")
    @Operation(summary = "Get Unread Count", description = "Retrieves current count of unread notifications for badge rendering")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "citizen-001") String authUserId) {
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", notificationService.getUnreadCount(authUserId)));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark Notification as Read", description = "Updates a notification status to READ")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "citizen-001") String authUserId,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as READ", notificationService.markAsRead(authUserId, id)));
    }

    @PutMapping("/{id}/unread")
    @Operation(summary = "Mark Notification as Unread", description = "Updates a notification status back to UNREAD")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsUnread(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "citizen-001") String authUserId,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as UNREAD", notificationService.markAsUnread(authUserId, id)));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark All as Read", description = "Bulk updates all unread notifications for citizen to READ status")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "citizen-001") String authUserId) {
        notificationService.markAllAsRead(authUserId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as READ", "SUCCESS"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Notification", description = "Soft-deletes a notification item from inbox")
    public ResponseEntity<ApiResponse<String>> deleteNotification(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "citizen-001") String authUserId,
            @PathVariable String id) {
        notificationService.deleteNotification(authUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search Notifications", description = "Searches notifications by subject or message content")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> searchNotifications(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "citizen-001") String authUserId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved", notificationService.searchNotifications(authUserId, q, page, size)));
    }

    @GetMapping("/history")
    @Operation(summary = "Get Notification History", description = "Retrieves complete notification delivery history")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getHistory(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "citizen-001") String authUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Notification history retrieved", notificationService.getNotificationHistory(authUserId, page, size)));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get Notification Dashboard Summary", description = "Aggregates notification count breakdown by channel and status for UI dashboard")
    public ResponseEntity<ApiResponse<NotificationDashboardResponse>> getDashboardSummary(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "citizen-001") String authUserId) {
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics retrieved", notificationService.getDashboardSummary(authUserId)));
    }
}
