package com.schemebridge.service.impl;

import com.schemebridge.dto.AdminDashboardResponse;
import com.schemebridge.entity.Application;
import com.schemebridge.entity.Document;
import com.schemebridge.entity.Scheme;
import com.schemebridge.entity.User;
import com.schemebridge.enums.AccountStatus;
import com.schemebridge.enums.ApplicationStatus;
import com.schemebridge.enums.DocumentStatus;
import com.schemebridge.enums.RoleEnum;
import com.schemebridge.enums.SchemeStatus;
import com.schemebridge.repository.ApplicationRepository;
import com.schemebridge.repository.DocumentRepository;
import com.schemebridge.repository.SchemeRepository;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final SchemeRepository schemeRepository;
    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;

    @Override
    public AdminDashboardResponse getDashboardMetrics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream().filter(user -> user.getStatus() == AccountStatus.ACTIVE).count();
        long verifiedUsers = userRepository.findAll().stream().filter(user -> Boolean.TRUE.equals(user.getEmailVerified()) && Boolean.TRUE.equals(user.getPhoneVerified())).count();
        long pendingUsers = userRepository.findAll().stream().filter(user -> user.getStatus() == AccountStatus.PENDING_VERIFICATION).count();

        long totalSchemes = schemeRepository.count();
        long publishedSchemes = schemeRepository.findByStatusAndDeletedFalse(SchemeStatus.PUBLISHED).size();
        long draftSchemes = schemeRepository.findByStatusAndDeletedFalse(SchemeStatus.DRAFT).size();
        long archivedSchemes = schemeRepository.findByStatusAndDeletedFalse(SchemeStatus.ARCHIVED).size();

        long applicationsSubmitted = applicationRepository.count();
        long applicationsApproved = applicationRepository.findAll().stream().filter(app -> app.getStatus() == ApplicationStatus.APPROVED).count();
        long applicationsRejected = applicationRepository.findAll().stream().filter(app -> app.getStatus() == ApplicationStatus.REJECTED).count();
        long applicationsPending = applicationRepository.findAll().stream().filter(app -> app.getStatus() == ApplicationStatus.SUBMITTED || app.getStatus() == ApplicationStatus.UNDER_REVIEW).count();

        long documentsUploaded = documentRepository.count();
        long verifiedDocuments = documentRepository.findAll().stream().filter(doc -> doc.getStatus() == DocumentStatus.VERIFIED).count();
        long pendingDocuments = documentRepository.findAll().stream().filter(doc -> doc.getStatus() == DocumentStatus.PENDING_REVIEW || doc.getStatus() == DocumentStatus.UPLOADED).count();

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .verifiedUsers(verifiedUsers)
                .pendingUsers(pendingUsers)
                .totalSchemes(totalSchemes)
                .publishedSchemes(publishedSchemes)
                .draftSchemes(draftSchemes)
                .archivedSchemes(archivedSchemes)
                .applicationsSubmitted(applicationsSubmitted)
                .applicationsApproved(applicationsApproved)
                .applicationsRejected(applicationsRejected)
                .applicationsPending(applicationsPending)
                .documentsUploaded(documentsUploaded)
                .verifiedDocuments(verifiedDocuments)
                .pendingDocuments(pendingDocuments)
                .build();
    }
}
