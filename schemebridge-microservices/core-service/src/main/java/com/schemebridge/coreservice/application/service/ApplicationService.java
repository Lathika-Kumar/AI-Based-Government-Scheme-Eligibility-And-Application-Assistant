package com.schemebridge.coreservice.application.service;

import com.schemebridge.common.event.BusinessEvents;
import com.schemebridge.common.event.DomainEventPublisher;
import com.schemebridge.coreservice.application.client.NotificationServiceClient;
import com.schemebridge.coreservice.application.model.ApplicationDocument;
import com.schemebridge.coreservice.application.model.EligibilitySnapshot;
import com.schemebridge.coreservice.application.dto.*;
import com.schemebridge.coreservice.application.enums.ApplicationStage;
import com.schemebridge.coreservice.application.enums.ApplicationStatus;
import com.schemebridge.coreservice.application.repository.ApplicationRepository;
import com.schemebridge.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationNumberGeneratorService numberGeneratorService;
    private final EligibilitySnapshotService eligibilitySnapshotService;
    private final ApplicationTimelineService timelineService;
    private final NotificationServiceClient notificationClient;
    private final DomainEventPublisher eventPublisher;

    private static final List<ApplicationStatus> ACTIVE_STATUSES = List.of(
            ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED,
            ApplicationStatus.UNDER_REVIEW, ApplicationStatus.DOCUMENT_PENDING,
            ApplicationStatus.VERIFIED
    );

    public ApplicationResponse createApplication(CreateApplicationRequest request) {
        boolean hasDuplicate = applicationRepository.existsByAuthUserIdAndSchemeIdAndApplicationStatusIn(
                request.getAuthUserId(), request.getSchemeId(), ACTIVE_STATUSES);
        if (hasDuplicate) {
            throw new IllegalStateException(
                    "An active application already exists for this scheme. " +
                    "Previous application must be REJECTED or WITHDRAWN before reapplying.");
        }

        EligibilitySnapshot snapshot = eligibilitySnapshotService.captureSnapshot(request);
        String appNumber = numberGeneratorService.generateApplicationNumber();

        ApplicationDocument app = ApplicationDocument.builder()
                .applicationNumber(appNumber)
                .authUserId(request.getAuthUserId())
                .citizenProfileId(request.getCitizenProfileId())
                .schemeId(request.getSchemeId())
                .schemeCode(request.getSchemeCode())
                .schemeName(request.getSchemeName())
                .departmentId(request.getDepartmentId())
                .departmentName(request.getDepartmentName())
                .applicationStatus(ApplicationStatus.DRAFT)
                .applicationStage(ApplicationStage.CITIZEN_SUBMISSION)
                .eligibilitySnapshot(snapshot)
                .remarks(request.getRemarks())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastUpdatedAt(LocalDateTime.now())
                .build();

        timelineService.addTrackingEntry(app, ApplicationStatus.DRAFT,
                ApplicationStage.CITIZEN_SUBMISSION, "CITIZEN",
                request.getAuthUserId(), "Application created as DRAFT", "CREATED");

        ApplicationDocument saved = applicationRepository.save(app);
        return mapToResponse(saved);
    }

    public ApplicationResponse getApplicationById(String id) {
        ApplicationDocument app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", id));
        return mapToResponse(app);
    }

    public ApplicationResponse getApplicationByNumber(String applicationNumber) {
        ApplicationDocument app = applicationRepository.findByApplicationNumber(applicationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "applicationNumber", applicationNumber));
        return mapToResponse(app);
    }

    public Page<ApplicationSummaryResponse> getMyApplications(String authUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return applicationRepository.findByAuthUserIdOrderByCreatedAtDesc(authUserId, pageable)
                .map(this::mapToSummary);
    }

    public ApplicationDashboardResponse getDashboard(String authUserId) {
        long total = applicationRepository.countByAuthUserId(authUserId);
        long approved = applicationRepository.countByAuthUserIdAndApplicationStatus(authUserId, ApplicationStatus.APPROVED);
        long rejected = applicationRepository.countByAuthUserIdAndApplicationStatus(authUserId, ApplicationStatus.REJECTED);
        long withdrawn = applicationRepository.countByAuthUserIdAndApplicationStatus(authUserId, ApplicationStatus.WITHDRAWN);
        long underReview = applicationRepository.countByAuthUserIdAndApplicationStatus(authUserId, ApplicationStatus.UNDER_REVIEW);
        long benefitReleased = applicationRepository.countByAuthUserIdAndApplicationStatus(authUserId, ApplicationStatus.BENEFIT_RELEASED);

        long pending = total - approved - rejected - withdrawn - benefitReleased;

        List<ApplicationSummaryResponse> recent = applicationRepository
                .findByAuthUserIdOrderByCreatedAtDesc(authUserId)
                .stream().limit(5).map(this::mapToSummary).collect(Collectors.toList());

        return ApplicationDashboardResponse.builder()
                .totalApplications(total)
                .approvedCount(approved)
                .pendingCount(Math.max(0, pending))
                .rejectedCount(rejected)
                .withdrawnCount(withdrawn)
                .underReviewCount(underReview)
                .benefitReleasedCount(benefitReleased)
                .recentApplications(recent)
                .build();
    }

    public ApplicationResponse submitApplication(String id) {
        ApplicationDocument app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", id));

        if (app.getApplicationStatus() != ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT applications can be submitted. Current status: " + app.getApplicationStatus());
        }

        timelineService.addTrackingEntry(app, ApplicationStatus.SUBMITTED,
                ApplicationStage.DOCUMENT_VERIFICATION, "CITIZEN",
                app.getAuthUserId(), "Application submitted by citizen", "SUBMITTED");

        app.setApplicationStatus(ApplicationStatus.SUBMITTED);
        app.setSubmittedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());

        ApplicationDocument saved = applicationRepository.save(app);

        // Publish ApplicationSubmittedEvent
        try {
            eventPublisher.publishEvent(BusinessEvents.createApplicationSubmittedEvent(
                    saved.getAuthUserId(), saved.getId(), saved.getApplicationNumber(), saved.getSchemeName()));
        } catch (Exception e) {
            log.warn("[ApplicationService] Failed to publish ApplicationSubmittedEvent: {}", e.getMessage());
        }

        notificationClient.notifyApplicationSubmitted(app.getAuthUserId(), app.getApplicationNumber());
        return mapToResponse(saved);
    }

    public ApplicationResponse updateApplicationStatus(String id, ApplicationStatusUpdateRequest request) {
        ApplicationDocument app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", id));

        ApplicationStatus newStatus = request.getStatus() != null ? request.getStatus() : ApplicationStatus.UNDER_REVIEW;
        app.setApplicationStatus(newStatus);
        if (request.getRemarks() != null) app.setRemarks(request.getRemarks());
        if (request.getRejectionReason() != null) app.setRejectionReason(request.getRejectionReason());
        if (request.getBenefitDetails() != null) {
            app.setBenefitType(request.getBenefitDetails());
            try {
                String numeric = request.getBenefitDetails().replaceAll("[^0-9.]", "");
                if (!numeric.isEmpty()) {
                    app.setBenefitAmount(new BigDecimal(numeric));
                }
            } catch (Exception ignored) {}
        }

        if (newStatus == ApplicationStatus.APPROVED) app.setApprovedAt(LocalDateTime.now());
        if (newStatus == ApplicationStatus.REJECTED) app.setRejectedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());
        app.setLastUpdatedAt(LocalDateTime.now());

        timelineService.addTrackingEntry(app, newStatus, app.getApplicationStage(), "ADMIN", "OFFICER",
                request.getRemarks() != null ? request.getRemarks() : "Status updated to " + newStatus, newStatus.name());

        ApplicationDocument saved = applicationRepository.save(app);

        // Publish business domain event based on status
        try {
            if (newStatus == ApplicationStatus.APPROVED) {
                eventPublisher.publishEvent(BusinessEvents.createApplicationApprovedEvent(
                        saved.getAuthUserId(), saved.getId(), saved.getApplicationNumber(),
                        saved.getSchemeName(), request.getBenefitDetails() != null ? request.getBenefitDetails() : "Benefit Direct Transfer"));
            } else if (newStatus == ApplicationStatus.REJECTED) {
                eventPublisher.publishEvent(BusinessEvents.createApplicationRejectedEvent(
                        saved.getAuthUserId(), saved.getId(), saved.getApplicationNumber(),
                        saved.getSchemeName(), request.getRejectionReason() != null ? request.getRejectionReason() : "Eligibility criteria not fulfilled"));
            } else {
                eventPublisher.publishEvent(BusinessEvents.createApplicationStatusChangedEvent(
                        saved.getAuthUserId(), saved.getId(), saved.getApplicationNumber(),
                        saved.getSchemeName(), newStatus.name(), request.getRemarks() != null ? request.getRemarks() : "Application under review"));
            }
        } catch (Exception e) {
            log.warn("[ApplicationService] Failed to publish Application Status Event: {}", e.getMessage());
        }

        return mapToResponse(saved);
    }

    public ApplicationResponse withdrawApplication(String id, WithdrawRequest request) {
        ApplicationDocument app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", id));

        if (app.getApplicationStatus().isTerminal()) {
            throw new IllegalStateException("Cannot withdraw a terminal application. Current status: " + app.getApplicationStatus());
        }

        String reason = request.getReason() != null ? request.getReason() : request.getRemarks();
        timelineService.addTrackingEntry(app, ApplicationStatus.WITHDRAWN,
                app.getApplicationStage(), "CITIZEN",
                app.getAuthUserId(), reason, "WITHDRAWN");

        app.setApplicationStatus(ApplicationStatus.WITHDRAWN);
        app.setRejectionReason(reason);
        app.setActive(false);
        app.setUpdatedAt(LocalDateTime.now());

        ApplicationDocument saved = applicationRepository.save(app);
        notificationClient.notifyApplicationWithdrawn(app.getAuthUserId(), app.getApplicationNumber());

        return mapToResponse(saved);
    }

    public Page<ApplicationSummaryResponse> searchApplications(String authUserId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return applicationRepository.searchByUser(authUserId, keyword, pageable).map(this::mapToSummary);
    }

    public ApplicationResponse updateApplication(String id, CreateApplicationRequest request) {
        ApplicationDocument app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", id));

        if (app.getApplicationStatus() != ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT applications can be updated.");
        }

        if (request.getRemarks() != null) app.setRemarks(request.getRemarks());
        if (request.getSchemeName() != null) app.setSchemeName(request.getSchemeName());
        if (request.getDepartmentName() != null) app.setDepartmentName(request.getDepartmentName());
        app.setUpdatedAt(LocalDateTime.now());
        app.setLastUpdatedAt(LocalDateTime.now());

        return mapToResponse(applicationRepository.save(app));
    }

    public ApplicationResponse mapToResponse(ApplicationDocument doc) {
        return ApplicationResponse.builder()
                .id(doc.getId())
                .applicationNumber(doc.getApplicationNumber())
                .authUserId(doc.getAuthUserId())
                .citizenProfileId(doc.getCitizenProfileId())
                .schemeId(doc.getSchemeId())
                .schemeCode(doc.getSchemeCode())
                .schemeName(doc.getSchemeName())
                .departmentId(doc.getDepartmentId())
                .departmentName(doc.getDepartmentName())
                .applicationStatus(doc.getApplicationStatus())
                .applicationStage(doc.getApplicationStage())
                .submittedAt(doc.getSubmittedAt())
                .lastUpdatedAt(doc.getLastUpdatedAt())
                .approvedAt(doc.getApprovedAt())
                .rejectedAt(doc.getRejectedAt())
                .benefitAmount(doc.getBenefitAmount())
                .benefitType(doc.getBenefitType())
                .remarks(doc.getRemarks())
                .rejectionReason(doc.getRejectionReason())
                .assignedOfficerId(doc.getAssignedOfficerId())
                .assignedOfficerName(doc.getAssignedOfficerName())
                .trackingHistory(doc.getTrackingHistory())
                .requiredDocuments(doc.getRequiredDocuments())
                .uploadedDocuments(doc.getUploadedDocuments())
                .eligibilitySnapshot(doc.getEligibilitySnapshot())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    private ApplicationSummaryResponse mapToSummary(ApplicationDocument doc) {
        return ApplicationSummaryResponse.builder()
                .id(doc.getId())
                .applicationNumber(doc.getApplicationNumber())
                .schemeCode(doc.getSchemeCode())
                .schemeName(doc.getSchemeName())
                .departmentName(doc.getDepartmentName())
                .applicationStatus(doc.getApplicationStatus())
                .applicationStage(doc.getApplicationStage())
                .submittedAt(doc.getSubmittedAt())
                .lastUpdatedAt(doc.getLastUpdatedAt())
                .build();
    }
}
