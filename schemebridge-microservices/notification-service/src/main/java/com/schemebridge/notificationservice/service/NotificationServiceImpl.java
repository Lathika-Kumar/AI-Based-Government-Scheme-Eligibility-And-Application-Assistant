package com.schemebridge.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.common.exception.ResourceNotFoundException;
import com.schemebridge.notificationservice.constants.NotificationConstants;
import com.schemebridge.notificationservice.dto.*;
import com.schemebridge.notificationservice.entity.*;
import com.schemebridge.notificationservice.enums.*;
import com.schemebridge.notificationservice.event.NotificationEventPublisher;
import com.schemebridge.notificationservice.factory.NotificationFactory;
import com.schemebridge.notificationservice.repository.*;
import com.schemebridge.notificationservice.strategy.NotificationProviderStrategy;
import com.schemebridge.notificationservice.strategy.NotificationProviderStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Clean Architecture Implementation of NotificationService leveraging Strategy, Factory,
 * Outbox, Dead Letter Queue (DLQ), and Event Lifecycle Publisher patterns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final ScheduledNotificationRepository scheduledRepository;
    private final NotificationOutboxRepository outboxRepository;
    private final NotificationDeadLetterRepository deadLetterRepository;

    private final PreferenceService preferenceService;
    private final NotificationTemplateEngine templateEngine;
    private final NotificationFactory notificationFactory;
    private final NotificationProviderStrategyFactory strategyFactory;
    private final NotificationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        log.info("Processing notification dispatch request for user: {}, Channel: {}, Type: {}",
                request.getAuthUserId(), request.getChannel(), request.getNotificationType());

        // Check user preferences
        if (!preferenceService.isChannelEnabled(request.getAuthUserId(), request.getChannel().name(), request.getNotificationType().name())) {
            log.info("Notification suppressed by user preferences for user: {}, Channel: {}", request.getAuthUserId(), request.getChannel());
            Notification suppressed = notificationFactory.createSuppressedNotification(request, "Suppressed by user preferences");
            return mapToResponse(notificationRepository.save(suppressed));
        }

        // Render template if text is not provided or templateVariables present
        String finalSubject = request.getSubject();
        String finalTitle = request.getTitle();
        String finalMessage = request.getMessage();
        String finalHtml = request.getHtmlContent();

        Optional<NotificationTemplate> templateOpt = templateRepository.findByNotificationTypeAndChannelAndActiveTrue(
                request.getNotificationType(), request.getChannel());

        if (templateOpt.isPresent()) {
            NotificationTemplate tmpl = templateOpt.get();
            Map<String, String> vars = request.getTemplateVariables() != null ? request.getTemplateVariables() : new HashMap<>();
            if (tmpl.getDefaultVariables() != null) {
                tmpl.getDefaultVariables().forEach(vars::putIfAbsent);
            }

            if (finalSubject == null || finalSubject.isEmpty()) finalSubject = templateEngine.render(tmpl.getSubjectTemplate(), vars);
            if (finalTitle == null || finalTitle.isEmpty()) finalTitle = templateEngine.render(tmpl.getTitleTemplate(), vars);
            if (finalMessage == null || finalMessage.isEmpty()) finalMessage = templateEngine.render(tmpl.getBodyTemplate(), vars);
            if (finalHtml == null || finalHtml.isEmpty()) finalHtml = templateEngine.render(tmpl.getHtmlTemplate(), vars);
        }

        Notification notification = notificationFactory.createNotification(request, finalSubject, finalTitle, finalMessage, finalHtml);
        Notification saved = notificationRepository.save(notification);

        // Transactional Outbox Entry (Eventual Consistency & Kafka / RabbitMQ readiness)
        recordOutboxEntry(saved);

        // Strategy Pattern for provider dispatch
        NotificationProviderStrategy strategy = strategyFactory.getStrategy(saved.getChannel());
        boolean success = strategy.send(saved);

        if (success) {
            saved.setDeliveryStatus(DeliveryStatus.DELIVERED);
            saved.setDeliveredAt(Instant.now());
            saved.setSentAt(Instant.now());
        } else {
            saved.setDeliveryStatus(DeliveryStatus.FAILED);
            saved.setFailureReason("Provider delivery failed on initial attempt");
        }

        Notification finalSaved = notificationRepository.save(saved);
        recordNotificationLog(finalSaved, strategy.getProviderName(), success ? DeliveryStatus.DELIVERED : DeliveryStatus.FAILED, success ? 200 : 500, null, saved.getFailureReason(), 1);

        // Emit Spring Event
        eventPublisher.publishNotificationEvent(finalSaved.getNotificationId(), finalSaved.getAuthUserId(), finalSaved.getChannel().name(), finalSaved.getDeliveryStatus());

        return mapToResponse(finalSaved);
    }

    @Override
    public NotificationResponse sendEmail(EmailNotificationRequest request) {
        SendNotificationRequest sendReq = SendNotificationRequest.builder()
                .authUserId(request.getAuthUserId())
                .recipientEmail(request.getRecipientEmail())
                .subject(request.getSubject())
                .message(request.getMessage())
                .htmlContent(request.getHtmlContent())
                .notificationType(request.getNotificationType())
                .channel(Channel.EMAIL)
                .priority(request.getPriority())
                .referenceId(request.getReferenceId())
                .templateVariables(request.getTemplateVariables())
                .build();
        return sendNotification(sendReq);
    }

    @Override
    public NotificationResponse sendSms(SmsNotificationRequest request) {
        SendNotificationRequest sendReq = SendNotificationRequest.builder()
                .authUserId(request.getAuthUserId())
                .recipientPhone(request.getRecipientPhone())
                .message(request.getMessage())
                .notificationType(request.getNotificationType())
                .channel(Channel.SMS)
                .priority(request.getPriority())
                .referenceId(request.getReferenceId())
                .templateVariables(request.getTemplateVariables())
                .build();
        return sendNotification(sendReq);
    }

    @Override
    public NotificationResponse sendPush(PushNotificationRequest request) {
        SendNotificationRequest sendReq = SendNotificationRequest.builder()
                .authUserId(request.getAuthUserId())
                .title(request.getTitle())
                .message(request.getMessage())
                .notificationType(request.getNotificationType())
                .channel(Channel.PUSH)
                .priority(request.getPriority())
                .templateVariables(request.getDataPayload())
                .build();
        return sendNotification(sendReq);
    }

    @Override
    public NotificationResponse sendInApp(SendNotificationRequest request) {
        request.setChannel(Channel.IN_APP);
        return sendNotification(request);
    }

    @Override
    public List<NotificationResponse> broadcast(BroadcastNotificationRequest request) {
        log.info("Processing Broadcast Notification to target users: {}", request.getTargetUserIds() != null ? request.getTargetUserIds().size() : "ALL");
        List<NotificationResponse> responses = new ArrayList<>();

        List<String> userIds = request.getTargetUserIds();
        if (userIds == null || userIds.isEmpty()) {
            userIds = List.of("BROADCAST_ALL_CITIZENS");
        }

        for (String uid : userIds) {
            SendNotificationRequest sendReq = SendNotificationRequest.builder()
                    .authUserId(uid)
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .htmlContent(request.getHtmlContent())
                    .notificationType(request.getNotificationType())
                    .channel(request.getChannel())
                    .priority(request.getPriority())
                    .build();
            responses.add(sendNotification(sendReq));
        }
        return responses;
    }

    @Override
    public String scheduleNotification(ScheduleNotificationRequest request) {
        log.info("Scheduling notification for execution at: {}", request.getScheduledTime());
        String schedId = "SCHED-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
        try {
            ScheduledNotification sched = ScheduledNotification.builder()
                    .notificationId(schedId)
                    .authUserId(request.getNotificationRequest().getAuthUserId())
                    .scheduledTime(request.getScheduledTime())
                    .status(DeliveryStatus.PENDING)
                    .targetChannel(request.getNotificationRequest().getChannel())
                    .requestPayload(objectMapper.writeValueAsString(request.getNotificationRequest()))
                    .build();
            scheduledRepository.save(sched);
            return schedId;
        } catch (Exception ex) {
            log.error("Failed to serialize scheduled notification payload", ex);
            throw new RuntimeException("Error scheduling notification: " + ex.getMessage());
        }
    }

    @Override
    public Page<NotificationResponse> getMyNotifications(String authUserId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, NotificationConstants.DEFAULT_SORT_BY));
        return notificationRepository.findByAuthUserIdAndActiveTrue(authUserId, pageable).map(this::mapToResponse);
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(String authUserId) {
        return notificationRepository.findByAuthUserIdAndReadStatusAndActiveTrue(authUserId, ReadStatus.UNREAD)
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    public Long getUnreadCount(String authUserId) {
        return notificationRepository.countByAuthUserIdAndReadStatusAndActiveTrue(authUserId, ReadStatus.UNREAD);
    }

    @Override
    public NotificationResponse markAsRead(String authUserId, String notificationId) {
        Notification notif = findNotification(notificationId);
        notif.setReadStatus(ReadStatus.READ);
        notif.setReadAt(Instant.now());
        return mapToResponse(notificationRepository.save(notif));
    }

    @Override
    public NotificationResponse markAsUnread(String authUserId, String notificationId) {
        Notification notif = findNotification(notificationId);
        notif.setReadStatus(ReadStatus.UNREAD);
        notif.setReadAt(null);
        return mapToResponse(notificationRepository.save(notif));
    }

    @Override
    public void markAllAsRead(String authUserId) {
        List<Notification> unreadList = notificationRepository.findByAuthUserIdAndReadStatusAndActiveTrue(authUserId, ReadStatus.UNREAD);
        Instant now = Instant.now();
        unreadList.forEach(n -> {
            n.setReadStatus(ReadStatus.READ);
            n.setReadAt(now);
        });
        notificationRepository.saveAll(unreadList);
        log.info("Marked {} unread notifications as READ for user {}", unreadList.size(), authUserId);
    }

    @Override
    public void deleteNotification(String authUserId, String notificationId) {
        Notification notif = findNotification(notificationId);
        notif.setActive(false);
        notificationRepository.save(notif);
        log.info("Soft-deleted notification {} for user {}", notificationId, authUserId);
    }

    @Override
    public Page<NotificationResponse> searchNotifications(String authUserId, String query, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, NotificationConstants.DEFAULT_SORT_BY));
        return notificationRepository.findByAuthUserIdAndSubjectContainingIgnoreCaseOrMessageContainingIgnoreCaseAndActiveTrue(
                authUserId, query, query, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<NotificationResponse> getNotificationHistory(String authUserId, int page, int size) {
        return getMyNotifications(authUserId, page, size);
    }

    @Override
    public NotificationDashboardResponse getDashboardSummary(String authUserId) {
        List<Notification> userNotifs = notificationRepository.findByAuthUserIdAndActiveTrue(authUserId, PageRequest.of(0, 1000)).getContent();

        long unread = userNotifs.stream().filter(n -> ReadStatus.UNREAD.equals(n.getReadStatus())).count();
        long read = userNotifs.stream().filter(n -> ReadStatus.READ.equals(n.getReadStatus())).count();
        long pending = userNotifs.stream().filter(n -> DeliveryStatus.PENDING.equals(n.getDeliveryStatus())).count();
        long failed = userNotifs.stream().filter(n -> DeliveryStatus.FAILED.equals(n.getDeliveryStatus())).count();

        Map<String, Long> countByChannel = new HashMap<>();
        Map<String, Long> countByType = new HashMap<>();

        for (Notification n : userNotifs) {
            if (n.getChannel() != null) {
                countByChannel.put(n.getChannel().name(), countByChannel.getOrDefault(n.getChannel().name(), 0L) + 1);
            }
            if (n.getNotificationType() != null) {
                countByType.put(n.getNotificationType().name(), countByType.getOrDefault(n.getNotificationType().name(), 0L) + 1);
            }
        }

        return NotificationDashboardResponse.builder()
                .totalNotifications((long) userNotifs.size())
                .unreadCount(unread)
                .readCount(read)
                .pendingDeliveryCount(pending)
                .failedDeliveryCount(failed)
                .countByChannel(countByChannel)
                .countByType(countByType)
                .build();
    }

    @Override
    public void retryFailedNotifications() {
        List<Notification> failedList = notificationRepository.findByDeliveryStatusAndRetryCountLessThanAndActiveTrue(
                DeliveryStatus.FAILED, NotificationConstants.MAX_RETRY_ATTEMPTS);
        log.info("Executing Notification Retry Job. Found {} failed notifications to retry.", failedList.size());

        for (Notification notif : failedList) {
            notif.setRetryCount(notif.getRetryCount() + 1);
            NotificationProviderStrategy strategy = strategyFactory.getStrategy(notif.getChannel());
            boolean success = strategy.send(notif);

            if (success) {
                notif.setDeliveryStatus(DeliveryStatus.DELIVERED);
                notif.setDeliveredAt(Instant.now());
                notif.setFailureReason(null);
            } else if (notif.getRetryCount() >= NotificationConstants.MAX_RETRY_ATTEMPTS) {
                notif.setFailureReason("Max retry limit (" + NotificationConstants.MAX_RETRY_ATTEMPTS + ") exceeded.");
                // Dead Letter Queue (DLQ) Entry creation for admin investigation
                recordDeadLetterEntry(notif);
            }
            notificationRepository.save(notif);
            recordNotificationLog(notif, strategy.getProviderName(), success ? DeliveryStatus.DELIVERED : DeliveryStatus.FAILED,
                    success ? 200 : 500, null, notif.getFailureReason(), notif.getRetryCount() + 1);
        }
    }

    @Override
    public void executeScheduledNotifications() {
        Instant now = Instant.now();
        List<ScheduledNotification> dueList = scheduledRepository.findByStatusAndScheduledTimeBeforeAndActiveTrue(DeliveryStatus.PENDING, now);
        log.info("Executing Scheduled Notifications Job. Found {} due notifications.", dueList.size());

        for (ScheduledNotification sched : dueList) {
            try {
                SendNotificationRequest sendReq = objectMapper.readValue(sched.getRequestPayload(), SendNotificationRequest.class);
                sendNotification(sendReq);
                sched.setStatus(DeliveryStatus.DELIVERED);
            } catch (Exception ex) {
                log.error("Failed to execute scheduled notification {}", sched.getNotificationId(), ex);
                sched.setStatus(DeliveryStatus.FAILED);
            }
            scheduledRepository.save(sched);
        }
    }

    private Notification findNotification(String notificationId) {
        return notificationRepository.findByNotificationId(notificationId)
                .or(() -> notificationRepository.findById(notificationId))
                .orElseThrow(() -> new ResourceNotFoundException(NotificationConstants.ERR_NOTIF_NOT_FOUND + notificationId));
    }

    private void recordOutboxEntry(Notification n) {
        try {
            NotificationOutbox outbox = NotificationOutbox.builder()
                    .outboxId(NotificationConstants.OUTBOX_ID_PREFIX + UUID.randomUUID().toString().substring(0, 13).toUpperCase())
                    .notificationId(n.getNotificationId())
                    .authUserId(n.getAuthUserId())
                    .notificationType(n.getNotificationType())
                    .channel(n.getChannel())
                    .payloadJson(objectMapper.writeValueAsString(n))
                    .processed(false)
                    .build();
            outboxRepository.save(outbox);
        } catch (Exception ex) {
            log.error("Failed to record outbox entry for notification {}", n.getNotificationId(), ex);
        }
    }

    private void recordDeadLetterEntry(Notification n) {
        try {
            NotificationDeadLetter dlq = NotificationDeadLetter.builder()
                    .dlqId(NotificationConstants.DLQ_ID_PREFIX + UUID.randomUUID().toString().substring(0, 13).toUpperCase())
                    .notificationId(n.getNotificationId())
                    .authUserId(n.getAuthUserId())
                    .notificationType(n.getNotificationType())
                    .channel(n.getChannel())
                    .failureReason(n.getFailureReason())
                    .totalAttempts(n.getRetryCount())
                    .originalPayloadJson(objectMapper.writeValueAsString(n))
                    .failedAt(Instant.now())
                    .replayed(false)
                    .build();
            deadLetterRepository.save(dlq);
            log.warn("Notification {} sent to Dead Letter Queue (DLQ)", n.getNotificationId());
        } catch (Exception ex) {
            log.error("Failed to record DLQ entry for notification {}", n.getNotificationId(), ex);
        }
    }

    private void recordNotificationLog(Notification n, String providerName, DeliveryStatus status, int responseCode, String payload, String reason, int attempt) {
        NotificationLog logEntry = NotificationLog.builder()
                .notificationId(n.getNotificationId())
                .authUserId(n.getAuthUserId())
                .channel(n.getChannel())
                .providerName(providerName)
                .status(status)
                .responseCode(responseCode)
                .responsePayload(payload)
                .failureReason(reason)
                .attemptNumber(attempt)
                .timestamp(Instant.now())
                .build();
        logRepository.save(logEntry);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .notificationId(n.getNotificationId())
                .authUserId(n.getAuthUserId())
                .notificationType(n.getNotificationType())
                .channel(n.getChannel())
                .subject(n.getSubject())
                .title(n.getTitle())
                .message(n.getMessage())
                .htmlContent(n.getHtmlContent())
                .recipientEmail(n.getRecipientEmail())
                .recipientPhone(n.getRecipientPhone())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .priority(n.getPriority())
                .deliveryStatus(n.getDeliveryStatus())
                .readStatus(n.getReadStatus())
                .sentAt(n.getSentAt())
                .deliveredAt(n.getDeliveredAt())
                .readAt(n.getReadAt())
                .expiresAt(n.getExpiresAt())
                .failureReason(n.getFailureReason())
                .retryCount(n.getRetryCount())
                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())
                .build();
    }
}
