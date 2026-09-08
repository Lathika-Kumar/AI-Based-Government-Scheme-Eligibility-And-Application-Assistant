package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.AdminMetricsResponse;
import com.schemebridge.scheme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMetricsService {

    private final SchemeRepository schemeRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final GrievanceRepository grievanceRepository;
    private final NotificationRepository notificationRepository;
    private final MongoTemplate mongoTemplate;

    public AdminMetricsResponse getMetrics() {
        // 1. Schemes counts
        long totalSchemes = schemeRepository.count();
        long activeSchemes = schemeRepository.countByStatus(SchemeStatus.ACTIVE);
        long draftSchemes = schemeRepository.countByStatus(SchemeStatus.DRAFT);
        long inactiveSchemes = schemeRepository.countByStatus(SchemeStatus.INACTIVE) + schemeRepository.countByStatus(SchemeStatus.ARCHIVED);

        // 2. Application status counts
        long totalApplications = applicationRepository.count();
        long pendingApps = applicationRepository.countByStatus(ApplicationStatus.SUBMITTED)
                + applicationRepository.countByStatus(ApplicationStatus.DOCUMENTS_PENDING);
        long underReviewApps = applicationRepository.countByStatus(ApplicationStatus.UNDER_REVIEW);
        long correctionApps = applicationRepository.countByStatus(ApplicationStatus.CORRECTION_REQUIRED);
        long approvedApps = applicationRepository.countByStatus(ApplicationStatus.APPROVED);
        long rejectedApps = applicationRepository.countByStatus(ApplicationStatus.REJECTED);
        long cancelledApps = applicationRepository.countByStatus(ApplicationStatus.CANCELLED);

        // 3. Document counts
        long docsPending = applicationDocumentRepository.countByVerificationStatus(DocumentVerificationStatus.PENDING);
        long docsVerified = applicationDocumentRepository.countByVerificationStatus(DocumentVerificationStatus.VERIFIED);
        long docsRejected = applicationDocumentRepository.countByVerificationStatus(DocumentVerificationStatus.REJECTED);

        // 4. Grievance counts
        long totalGrievances = grievanceRepository.count();
        long openGrievances = grievanceRepository.countByStatus(GrievanceStatus.OPEN);
        long inProgressGrievances = grievanceRepository.countByStatus(GrievanceStatus.IN_PROGRESS)
                + grievanceRepository.countByStatus(GrievanceStatus.WAITING_FOR_CITIZEN);
        long resolvedGrievances = grievanceRepository.countByStatus(GrievanceStatus.RESOLVED)
                + grievanceRepository.countByStatus(GrievanceStatus.CLOSED);
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        long overdueGrievances = grievanceRepository.countByStatusInAndCreatedAtBefore(
                List.of(GrievanceStatus.OPEN, GrievanceStatus.IN_PROGRESS), sevenDaysAgo);

        // 5. Time-based application volume
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfWeek = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        long appsToday = countApplicationsSince(startOfDay);
        long appsWeek = countApplicationsSince(startOfWeek);
        long appsMonth = countApplicationsSince(startOfMonth);

        // 6. Unread Admin Notifications
        Query unreadNotifQuery = new Query(new Criteria().andOperator(
                new Criteria().orOperator(
                        Criteria.where("recipientRole").in("ROLE_ADMIN", "ROLE_SCHEME_MANAGER", "ROLE_VERIFICATION_OFFICER", "ADMIN"),
                        Criteria.where("recipientUserId").is("admin")
                ),
                Criteria.where("read").is(false)
        ));
        long unreadAdminNotifs = mongoTemplate.count(unreadNotifQuery, Notification.class);

        // 7. Applications by Status Map
        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("DOCUMENTS_PENDING", applicationRepository.countByStatus(ApplicationStatus.DOCUMENTS_PENDING));
        byStatus.put("SUBMITTED", applicationRepository.countByStatus(ApplicationStatus.SUBMITTED));
        byStatus.put("UNDER_REVIEW", underReviewApps);
        byStatus.put("CORRECTION_REQUIRED", correctionApps);
        byStatus.put("APPROVED", approvedApps);
        byStatus.put("REJECTED", rejectedApps);
        byStatus.put("CANCELLED", cancelledApps);

        // 8. Applications by Scheme (Aggregation)
        Map<String, Long> byScheme = new LinkedHashMap<>();
        try {
            Aggregation agg = Aggregation.newAggregation(
                    Aggregation.group("schemeCode").count().as("count"),
                    Aggregation.sort(org.springframework.data.domain.Sort.Direction.DESC, "count"),
                    Aggregation.limit(10)
            );
            AggregationResults<Document> results = mongoTemplate.aggregate(agg, "applications", Document.class);
            for (Document doc : results.getMappedResults()) {
                String code = doc.getString("_id");
                if (code != null) {
                    byScheme.put(code, ((Number) doc.get("count")).longValue());
                }
            }
        } catch (Exception e) {
            log.warn("Failed aggregating applications by scheme", e);
        }

        // 9. Monthly Trend (Past 6 months)
        Map<String, Long> monthlyTrend = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String monthName = monthDate.getMonth().name().substring(0, 3);
            Instant mStart = monthDate.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant mEnd = monthDate.plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            long count = mongoTemplate.count(new Query(Criteria.where("createdAt").gte(mStart).lt(mEnd)), Application.class);
            monthlyTrend.put(monthName, count);
        }

        return AdminMetricsResponse.builder()
                .totalSchemes(totalSchemes)
                .activeSchemes(activeSchemes)
                .draftSchemes(draftSchemes)
                .inactiveSchemes(inactiveSchemes)
                .totalApplications(totalApplications)
                .pendingApplications(pendingApps)
                .underReviewApplications(underReviewApps)
                .correctionRequiredApplications(correctionApps)
                .approvedApplications(approvedApps)
                .rejectedApplications(rejectedApps)
                .cancelledApplications(cancelledApps)
                .documentsPending(docsPending)
                .documentsVerified(docsVerified)
                .documentsRejected(docsRejected)
                .totalGrievances(totalGrievances)
                .openGrievances(openGrievances)
                .inProgressGrievances(inProgressGrievances)
                .resolvedGrievances(resolvedGrievances)
                .overdueGrievances(overdueGrievances)
                .applicationsToday(appsToday)
                .applicationsThisWeek(appsWeek)
                .applicationsThisMonth(appsMonth)
                .unreadAdminNotifications(unreadAdminNotifs)
                .applicationsByStatus(byStatus)
                .applicationsByScheme(byScheme)
                .applicationsByState(Map.of("All States", totalApplications))
                .monthlyTrend(monthlyTrend)
                .build();
    }

    private long countApplicationsSince(Instant timestamp) {
        return mongoTemplate.count(new Query(Criteria.where("createdAt").gte(timestamp)), Application.class);
    }
}
