package com.schemebridge.adminservice.service;

import com.schemebridge.adminservice.constants.AdminConstants;
import com.schemebridge.adminservice.dto.FeedbackResolveRequest;
import com.schemebridge.adminservice.dto.FeedbackResponse;
import com.schemebridge.adminservice.entity.SystemFeedback;
import com.schemebridge.adminservice.enums.AdminActionType;
import com.schemebridge.adminservice.enums.FeedbackStatus;
import com.schemebridge.adminservice.repository.SystemFeedbackRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackServiceImpl implements FeedbackService {

    private final SystemFeedbackRepository feedbackRepository;
    private final AuditLogService auditLogService;
    private final DomainEventPublisher eventPublisher;

    @Override
    @Transactional
    public FeedbackResponse resolveFeedback(String actorEmail, String feedbackId, FeedbackResolveRequest request) {
        SystemFeedback fb = findEntity(feedbackId);

        fb.setStatus(request.getStatus());
        fb.setResolutionNotes(request.getResolutionNotes());
        if (request.getAssignedOfficerId() != null) fb.setAssignedOfficerId(request.getAssignedOfficerId());

        if (FeedbackStatus.RESOLVED.equals(request.getStatus())) {
            fb.setResolvedAt(Instant.now());
        }

        SystemFeedback saved = feedbackRepository.save(fb);

        auditLogService.logActivity(actorEmail, AdminActionType.FEEDBACK_RESOLVED, "Feedback", feedbackId,
                "Resolved feedback with status: " + request.getStatus());

        // Publish GrievanceStatusUpdatedEvent
        try {
            String uid = saved.getUserId() != null ? saved.getUserId() : "citizen-001";
            String resNotes = saved.getResolutionNotes() != null ? saved.getResolutionNotes() : "Resolved by Officer";
            eventPublisher.publishEvent(BusinessEvents.createGrievanceStatusUpdatedEvent(
                    uid, saved.getFeedbackId(), saved.getStatus().name(), resNotes));
        } catch (Exception e) {
            log.warn("[FeedbackService] Failed to publish GrievanceStatusUpdatedEvent: {}", e.getMessage());
        }

        return mapToResponse(saved);
    }

    @Override
    public FeedbackResponse getFeedbackById(String feedbackId) {
        return mapToResponse(findEntity(feedbackId));
    }

    @Override
    public Page<FeedbackResponse> getAllFeedback(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AdminConstants.DEFAULT_SORT_BY));
        return feedbackRepository.findByActiveTrue(pageable).map(this::mapToResponse);
    }

    @Override
    public Page<FeedbackResponse> getFeedbackByStatus(FeedbackStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AdminConstants.DEFAULT_SORT_BY));
        return feedbackRepository.findByStatusAndActiveTrue(status, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<FeedbackResponse> searchFeedback(String query, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AdminConstants.DEFAULT_SORT_BY));
        return feedbackRepository.findBySubjectContainingIgnoreCaseOrMessageContainingIgnoreCaseAndActiveTrue(query, query, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void deleteFeedback(String actorEmail, String feedbackId) {
        SystemFeedback fb = findEntity(feedbackId);
        fb.setActive(false);
        feedbackRepository.save(fb);
    }

    private SystemFeedback findEntity(String feedbackId) {
        return feedbackRepository.findByFeedbackId(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException(AdminConstants.ERR_FEEDBACK_NOT_FOUND + feedbackId));
    }

    private FeedbackResponse mapToResponse(SystemFeedback f) {
        return FeedbackResponse.builder()
                .id(f.getId())
                .feedbackId(f.getFeedbackId())
                .userId(f.getUserId())
                .userEmail(f.getUserEmail())
                .subject(f.getSubject())
                .message(f.getMessage())
                .rating(f.getRating())
                .status(f.getStatus())
                .assignedOfficerId(f.getAssignedOfficerId())
                .resolutionNotes(f.getResolutionNotes())
                .resolvedAt(f.getResolvedAt())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}
