package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.Notification;
import com.schemebridge.scheme.document.NotificationType;
import com.schemebridge.scheme.dto.request.SendNotificationRequest;
import com.schemebridge.scheme.dto.response.NotificationResponse;
import com.schemebridge.scheme.dto.response.PagedNotificationResponse;
import com.schemebridge.scheme.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MongoTemplate mongoTemplate;

    // Thread-safe SSE emitters list
    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();

    public SseEmitter registerSseEmitter(String userId, String userRole) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 min timeout
        sseEmitters.add(emitter);

        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(() -> sseEmitters.remove(emitter));
        emitter.onError(e -> sseEmitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected to SchemeBridge Real-Time Notification Stream"));
        } catch (IOException e) {
            sseEmitters.remove(emitter);
        }

        return emitter;
    }

    public Notification sendNotification(String recipientUserId, String recipientRole, NotificationType type,
                                         String title, String message, String channel,
                                         String relatedEntityType, String relatedEntityId,
                                         String createdBy, Map<String, Object> metadata) {
        Notification notification = Notification.builder()
                .recipientUserId(recipientUserId)
                .recipientRole(recipientRole)
                .type(type)
                .title(title)
                .message(message)
                .channel(channel != null ? channel : "IN_APP")
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .createdBy(createdBy != null ? createdBy : "SYSTEM")
                .metadata(metadata != null ? metadata : new HashMap<>())
                .read(false)
                .createdAt(Instant.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification persisted: id={}, type={}, recipientUser={}, recipientRole={}",
                saved.getId(), type, recipientUserId, recipientRole);

        // Dispatch SSE to active admin emitters
        dispatchSse(saved);

        return saved;
    }

    public Notification sendAdminNotification(SendNotificationRequest request, String createdBy) {
        return sendNotification(
                request.getRecipientUserId(),
                request.getRecipientRole(),
                request.getType(),
                request.getTitle(),
                request.getMessage(),
                request.getChannel(),
                request.getRelatedEntityType(),
                request.getRelatedEntityId(),
                createdBy,
                request.getMetadata()
        );
    }

    private void dispatchSse(Notification notification) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        NotificationResponse response = toResponse(notification);

        for (SseEmitter emitter : sseEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .id(notification.getId())
                        .data(response));
                if (!"notification".equalsIgnoreCase(notification.getType().name())) {
                    emitter.send(SseEmitter.event()
                            .name(notification.getType().name())
                            .id(notification.getId())
                            .data(response));
                }
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        sseEmitters.removeAll(deadEmitters);
    }

    public PagedNotificationResponse getUserNotifications(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifPage = notificationRepository.findAllByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
        long unreadCount = notificationRepository.countByRecipientUserIdAndReadFalse(userId);

        List<NotificationResponse> content = notifPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PagedNotificationResponse.builder()
                .content(content)
                .page(notifPage.getNumber())
                .size(notifPage.getSize())
                .totalElements(notifPage.getTotalElements())
                .totalPages(notifPage.getTotalPages())
                .unreadCount(unreadCount)
                .build();
    }

    public PagedNotificationResponse getAdminNotifications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Query query = new Query();
        Criteria roleCriteria = new Criteria().orOperator(
                Criteria.where("recipientRole").in("ROLE_ADMIN", "ROLE_SCHEME_MANAGER", "ROLE_VERIFICATION_OFFICER", "ADMIN"),
                Criteria.where("recipientUserId").is("admin")
        );
        query.addCriteria(roleCriteria);

        long totalElements = mongoTemplate.count(query, Notification.class);
        query.with(pageable);
        List<Notification> notifs = mongoTemplate.find(query, Notification.class);

        Query unreadQuery = new Query(new Criteria().andOperator(
                roleCriteria,
                Criteria.where("read").ne(true)
        ));
        long unreadCount = mongoTemplate.count(unreadQuery, Notification.class);

        List<NotificationResponse> content = notifs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        return PagedNotificationResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .unreadCount(unreadCount)
                .build();
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientUserIdAndReadFalse(userId);
    }

    public long getAdminUnreadCount() {
        Query unreadQuery = new Query(new Criteria().andOperator(
                new Criteria().orOperator(
                        Criteria.where("recipientRole").in("ROLE_ADMIN", "ROLE_SCHEME_MANAGER", "ROLE_VERIFICATION_OFFICER", "ADMIN"),
                        Criteria.where("recipientUserId").is("admin")
                ),
                Criteria.where("read").ne(true)
        ));
        return mongoTemplate.count(unreadQuery, Notification.class);
    }

    @Transactional
    public NotificationResponse markAsRead(String id, String userId, boolean isPrivileged) {
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + id));

        if (!isPrivileged) {
            if (notif.getRecipientUserId() == null || !notif.getRecipientUserId().equals(userId)) {
                throw new SecurityException("Unauthorized to update this notification");
            }
        } else {
            boolean isTargetedToAdmin = notif.getRecipientRole() != null &&
                    List.of("ROLE_ADMIN", "ADMIN", "ROLE_SCHEME_MANAGER", "SCHEME_MANAGER", "ROLE_VERIFICATION_OFFICER", "VERIFICATION_OFFICER")
                            .contains(notif.getRecipientRole().toUpperCase());
            boolean isForThisUser = notif.getRecipientUserId() != null &&
                    (notif.getRecipientUserId().equals(userId) || "admin".equalsIgnoreCase(notif.getRecipientUserId()));

            if (!isTargetedToAdmin && !isForThisUser) {
                throw new SecurityException("Unauthorized to update this notification");
            }
        }

        if (!notif.isRead()) {
            notif.setRead(true);
            notif.setReadAt(Instant.now());
            notif = notificationRepository.save(notif);
        }
        return toResponse(notif);
    }

    @Transactional
    public void markAllAsRead(String userId, boolean isPrivileged) {
        if (isPrivileged) {
            Query query = new Query(new Criteria().andOperator(
                    new Criteria().orOperator(
                            Criteria.where("recipientRole").in("ROLE_ADMIN", "ROLE_SCHEME_MANAGER", "ROLE_VERIFICATION_OFFICER", "ADMIN"),
                            Criteria.where("recipientUserId").is(userId),
                            Criteria.where("recipientUserId").is("admin")
                    ),
                    Criteria.where("read").ne(true)
            ));
            List<Notification> notifs = mongoTemplate.find(query, Notification.class);
            Instant now = Instant.now();
            notifs.forEach(n -> {
                n.setRead(true);
                n.setReadAt(now);
            });
            notificationRepository.saveAll(notifs);
        } else {
            List<Notification> notifs = notificationRepository.findAllByRecipientUserIdAndReadFalse(userId);
            Instant now = Instant.now();
            notifs.forEach(n -> {
                n.setRead(true);
                n.setReadAt(now);
            });
            notificationRepository.saveAll(notifs);
        }
    }

    public NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .recipientUserId(n.getRecipientUserId())
                .recipientRole(n.getRecipientRole())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .channel(n.getChannel())
                .relatedEntityType(n.getRelatedEntityType())
                .relatedEntityId(n.getRelatedEntityId())
                .read(n.isRead())
                .readAt(n.getReadAt())
                .createdBy(n.getCreatedBy())
                .metadata(n.getMetadata())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
