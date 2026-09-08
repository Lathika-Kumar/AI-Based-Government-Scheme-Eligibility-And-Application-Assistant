package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.request.SendNotificationRequest;
import com.schemebridge.scheme.dto.response.NotificationResponse;
import com.schemebridge.scheme.dto.response.PagedNotificationResponse;
import com.schemebridge.scheme.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "Endpoints for managing notifications and streaming real-time alerts")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private String getRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .filter(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_SCHEME_MANAGER") || r.equals("ROLE_VERIFICATION_OFFICER"))
                    .findFirst()
                    .orElse("ROLE_USER");
        }
        return "ROLE_USER";
    }

    private boolean isPrivileged() {
        String role = getRole();
        return "ROLE_ADMIN".equals(role) || "ROLE_SCHEME_MANAGER".equals(role) || "ROLE_VERIFICATION_OFFICER".equals(role);
    }

    @GetMapping("/api/notifications")
    @Operation(summary = "Get current user's notifications (paginated)")
    public ResponseEntity<PagedNotificationResponse> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String userId = getUserId();
        PagedNotificationResponse response = isPrivileged()
                ? notificationService.getAdminNotifications(page, size)
                : notificationService.getUserNotifications(userId, page, size);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/api/notifications/unread-count")
    @Operation(summary = "Get unread notifications count for current user")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        String userId = getUserId();
        long count = isPrivileged()
                ? notificationService.getAdminUnreadCount()
                : notificationService.getUnreadCount(userId);
        return new ResponseEntity<>(Map.of("unreadCount", count), HttpStatus.OK);
    }

    @PostMapping("/api/notifications/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable String id) {
        String userId = getUserId();
        NotificationResponse response = notificationService.markAsRead(id, userId, isPrivileged());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/api/notifications/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        String userId = getUserId();
        notificationService.markAllAsRead(userId, isPrivileged());
        return new ResponseEntity<>(Map.of("message", "All notifications marked as read"), HttpStatus.OK);
    }

    @PostMapping("/api/admin/notifications/send")
    @Operation(summary = "Send a manual notification broadcast or targeted alert (Admin only)")
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody SendNotificationRequest request) {
        String createdBy = getUserId();
        var notif = notificationService.sendAdminNotification(request, createdBy);
        return new ResponseEntity<>(notificationService.toResponse(notif), HttpStatus.CREATED);
    }

    @GetMapping(value = "/api/admin/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Establish Server-Sent Events (SSE) stream for real-time admin operational alerts")
    public SseEmitter streamAdminNotifications() {
        String userId = getUserId();
        String role = getRole();
        return notificationService.registerSseEmitter(userId, role);
    }
}
