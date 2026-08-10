package com.schemebridge.applicationservice.service;

import com.schemebridge.applicationservice.client.NotificationServiceClient;
import com.schemebridge.applicationservice.document.ApplicationDocument;
import com.schemebridge.applicationservice.document.EligibilitySnapshot;
import com.schemebridge.applicationservice.dto.*;
import com.schemebridge.applicationservice.enums.ApplicationStage;
import com.schemebridge.applicationservice.enums.ApplicationStatus;
import com.schemebridge.applicationservice.repository.ApplicationRepository;
import com.schemebridge.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationNumberGeneratorService numberGeneratorService;
    private final EligibilitySnapshotService eligibilitySnapshotService;
    private final ApplicationTimelineService timelineService;
    private final NotificationServiceClient notificationClient;

    // ─── Active statuses for duplicate check ───────────────────────────────────
    private static final List<ApplicationStatus> ACTIVE_STATUSES = List.of(
            ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED,
            ApplicationStatus.UNDER_REVIEW, ApplicationStatus.DOCUMENT_PENDING,
            ApplicationStatus.VERIFIED
    );

    // ────────────────────────────────────────────────────────────────────────────
    // CREATE
    // ────────────────────────────────────────────────────────────────────────────

    public ApplicationResponse createApplication(CreateApplicationRequest request) {
        // Duplicate check: reject if active application exists for same user+scheme
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

        // Add initial tracking entry
        timelineService.addTrackingEntry(app, ApplicationStatus.DRAFT,
                ApplicationStage.CITIZEN_SUBMISSION, "CITIZEN",
                request.getAuthUserId(), "Application created as DRAFT", "CREATED");

        ApplicationDocument saved = applicationRepository.save(app);
        return mapToResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // GET BY ID
    // ────────────────────────────────────────────────────────────────────────────

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

    // ────────────────────────────────────────────────────────────────────────────
    // MY APPLICATIONS
    // ────────────────────────────────────────────────────────────────────────────

    public Page<ApplicationSummaryResponse> getMyApplications(String authUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return applicationRepository.findByAuthUserIdOrderByCreatedAtDesc(authUserId, pageable)
                .map(this::mapToSummary);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // DASHBOARD
    // ────────────────────────────────────────────────────────────────────────────

    public ApplicationDashboardResponse getDashboard(String authUserId) {
        long total = applicationRepository.countByAuthUserId(authUserId);
        long approved = applicationRepository.countByAuthUserIdAndApplicationStatus(
                authUserId, ApplicationStatus.APPROVED);
        long rejected = applicationRepository.countByAuthUserIdAndApplicationStatus(
                authUserId, ApplicationStatus.REJECTED);
        long withdrawn = applicationRepository.countByAuthUserIdAndApplicationStatus(
                authUserId, ApplicationStatus.WITHDRAWN);
        long underReview = applicationRepository.countByAuthUserIdAndApplicationStatus(
                authUserId, ApplicationStatus.UNDER_REVIEW);
        long benefitReleased = applicationRepository.countByAuthUserIdAndApplicationStatus(
                authUserId, ApplicationStatus.BENEFIT_RELEASED);

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

    // ────────────────────────────────────────────────────────────────────────────
    // SUBMIT
    // ────────────────────────────────────────────────────────────────────────────

    public ApplicationResponse submitApplication(String id) {
        ApplicationDocument app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", id));

        if (app.getApplicationStatus() != ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT applications can be submitted. Current status: "
                    + app.getApplicationStatus());
        }

        timelineService.addTrackingEntry(app, ApplicationStatus.SUBMITTED,
                ApplicationStage.DOCUMENT_VERIFICATION, "CITIZEN",
                app.getAuthUserId(), "Application submitted by citizen", "SUBMITTED");

        app.setSubmittedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());

        ApplicationDocument saved = applicationRepository.save(app);

        // Notify (stub)
        notificationClient.notifyApplicationSubmitted(app.getAuthUserId(), app.getApplicationNumber());

        return mapToResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // WITHDRAW
    // ────────────────────────────────────────────────────────────────────────────

    public ApplicationResponse withdrawApplication(String id, WithdrawRequest request) {
        ApplicationDocument app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", id));

        if (app.getApplicationStatus().isTerminal()) {
            throw new IllegalStateException("Cannot withdraw a terminal application. Current status: "
                    + app.getApplicationStatus());
        }

        String reason = request.getReason() != null ? request.getReason() : request.getRemarks();
        timelineService.addTrackingEntry(app, ApplicationStatus.WITHDRAWN,
                app.getApplicationStage(), "CITIZEN",
                app.getAuthUserId(), reason, "WITHDRAWN");

        app.setRejectionReason(reason);
        app.setActive(false);
        app.setUpdatedAt(LocalDateTime.now());

        ApplicationDocument saved = applicationRepository.save(app);
        notificationClient.notifyApplicationWithdrawn(app.getAuthUserId(), app.getApplicationNumber());

        return mapToResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // SEARCH
    // ────────────────────────────────────────────────────────────────────────────

    public Page<ApplicationSummaryResponse> searchApplications(String authUserId, String keyword,
                                                               int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return applicationRepository.searchByUser(authUserId, keyword, pageable)
                .map(this::mapToSummary);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // UPDATE (PATCH)
    // ────────────────────────────────────────────────────────────────────────────

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

    // ────────────────────────────────────────────────────────────────────────────
    // MAPPERS
    // ────────────────────────────────────────────────────────────────────────────

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
