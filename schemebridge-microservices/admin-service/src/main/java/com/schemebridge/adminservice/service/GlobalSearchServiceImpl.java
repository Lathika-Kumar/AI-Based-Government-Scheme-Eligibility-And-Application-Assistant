package com.schemebridge.adminservice.service;

import com.schemebridge.adminservice.dto.GlobalSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalSearchServiceImpl implements GlobalSearchService {

    private final OfficerService officerService;
    private final AnnouncementService announcementService;
    private final FeedbackService feedbackService;
    private final AuditLogService auditLogService;

    @Override
    public GlobalSearchResponse searchAll(String query) {
        log.info("Executing Global Search for query: '{}'", query);

        return GlobalSearchResponse.builder()
                .query(query)
                .officers(officerService.searchOfficers(query, 0, 5).getContent())
                .announcements(announcementService.getAllAnnouncements(0, 5).getContent())
                .feedback(feedbackService.searchFeedback(query, 0, 5).getContent())
                .activityLogs(auditLogService.getRecentLogs(0, 5).getContent())
                .build();
    }
}
