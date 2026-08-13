package com.schemebridge.adminservice.service;

import com.schemebridge.adminservice.constants.AdminConstants;
import com.schemebridge.adminservice.dto.AnnouncementRequest;
import com.schemebridge.adminservice.dto.AnnouncementResponse;
import com.schemebridge.adminservice.entity.SystemAnnouncement;
import com.schemebridge.adminservice.enums.AdminActionType;
import com.schemebridge.adminservice.enums.AnnouncementAudience;
import com.schemebridge.adminservice.enums.AnnouncementPriority;
import com.schemebridge.adminservice.repository.SystemAnnouncementRepository;
import com.schemebridge.common.event.BusinessEvents;
import com.schemebridge.common.event.DomainEventPublisher;
import com.schemebridge.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementServiceImpl implements AnnouncementService {

    private final SystemAnnouncementRepository announcementRepository;
    private final AuditLogService auditLogService;
    private final DomainEventPublisher eventPublisher;

    @Override
    @Transactional
    public AnnouncementResponse createAnnouncement(String actorEmail, AnnouncementRequest request) {
        log.info("Creating announcement '{}' by {}", request.getTitle(), actorEmail);

        String annId = AdminConstants.ANNOUNCEMENT_ID_PREFIX + UUID.randomUUID().toString().substring(0, 13).toUpperCase();

        SystemAnnouncement announcement = SystemAnnouncement.builder()
                .announcementId(annId)
                .title(request.getTitle())
                .content(request.getContent())
                .priority(request.getPriority() != null ? request.getPriority() : AnnouncementPriority.NORMAL)
                .targetAudience(request.getTargetAudience() != null ? request.getTargetAudience() : AnnouncementAudience.ALL_USERS)
                .scheduledAt(request.getScheduledAt())
                .expiresAt(request.getExpiresAt())
                .broadcasted(false)
                .build();

        SystemAnnouncement saved = announcementRepository.save(announcement);

        auditLogService.logActivity(actorEmail, AdminActionType.ANNOUNCEMENT_CREATED, "Announcement", saved.getAnnouncementId(),
                "Created announcement: " + saved.getTitle());

        // Publish AnnouncementPublishedEvent
        try {
            eventPublisher.publishEvent(BusinessEvents.createAnnouncementPublishedEvent(
                    saved.getAnnouncementId(), saved.getTitle(), saved.getContent(),
                    saved.getTargetAudience().name(), List.of("citizen-001")));
        } catch (Exception e) {
            log.warn("[AnnouncementService] Failed to publish AnnouncementPublishedEvent: {}", e.getMessage());
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public AnnouncementResponse updateAnnouncement(String actorEmail, String announcementId, AnnouncementRequest request) {
        SystemAnnouncement ann = findEntity(announcementId);

        ann.setTitle(request.getTitle());
        ann.setContent(request.getContent());
        if (request.getPriority() != null) ann.setPriority(request.getPriority());
        if (request.getTargetAudience() != null) ann.setTargetAudience(request.getTargetAudience());
        ann.setScheduledAt(request.getScheduledAt());
        ann.setExpiresAt(request.getExpiresAt());

        SystemAnnouncement updated = announcementRepository.save(ann);

        auditLogService.logActivity(actorEmail, AdminActionType.ANNOUNCEMENT_UPDATED, "Announcement", updated.getAnnouncementId(),
                "Updated announcement: " + updated.getTitle());

        return mapToResponse(updated);
    }

    @Override
    public AnnouncementResponse getAnnouncementById(String announcementId) {
        return mapToResponse(findEntity(announcementId));
    }

    @Override
    public Page<AnnouncementResponse> getAllAnnouncements(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AdminConstants.DEFAULT_SORT_BY));
        return announcementRepository.findByActiveTrue(pageable).map(this::mapToResponse);
    }

    @Override
    public List<AnnouncementResponse> getAnnouncementsForAudience(AnnouncementAudience audience) {
        return announcementRepository.findByTargetAudienceAndActiveTrue(audience).stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public void broadcastAnnouncement(String actorEmail, String announcementId) {
        SystemAnnouncement ann = findEntity(announcementId);
        ann.setBroadcasted(true);
        SystemAnnouncement saved = announcementRepository.save(ann);

        auditLogService.logActivity(actorEmail, AdminActionType.ANNOUNCEMENT_UPDATED, "Announcement", ann.getAnnouncementId(),
                "Broadcasted announcement: " + ann.getTitle());

        try {
            eventPublisher.publishEvent(BusinessEvents.createAnnouncementPublishedEvent(
                    saved.getAnnouncementId(), saved.getTitle(), saved.getContent(),
                    saved.getTargetAudience().name(), List.of("citizen-001")));
        } catch (Exception e) {
            log.warn("[AnnouncementService] Failed to publish broadcast event: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteAnnouncement(String actorEmail, String announcementId) {
        SystemAnnouncement ann = findEntity(announcementId);
        ann.setActive(false);
        announcementRepository.save(ann);
    }

    @Override
    @Transactional
    public void cleanupExpiredAnnouncements() {
        log.info("Cleaning up expired announcements...");
        Instant now = Instant.now();
        List<SystemAnnouncement> expired = announcementRepository.findByExpiresAtBeforeAndActiveTrue(now);
        for (SystemAnnouncement ann : expired) {
            ann.setActive(false);
            announcementRepository.save(ann);
        }
    }

    private SystemAnnouncement findEntity(String announcementId) {
        return announcementRepository.findByAnnouncementId(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException(AdminConstants.ERR_ANNOUNCEMENT_NOT_FOUND + announcementId));
    }

    private AnnouncementResponse mapToResponse(SystemAnnouncement a) {
        return AnnouncementResponse.builder()
                .id(a.getId())
                .announcementId(a.getAnnouncementId())
                .title(a.getTitle())
                .content(a.getContent())
                .priority(a.getPriority())
                .targetAudience(a.getTargetAudience())
                .scheduledAt(a.getScheduledAt())
                .expiresAt(a.getExpiresAt())
                .broadcasted(a.getBroadcasted())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
