package com.schemebridge.applicationservice.service;

import com.schemebridge.applicationservice.document.ApplicationDocument;
import com.schemebridge.applicationservice.document.TrackingHistoryItem;
import com.schemebridge.applicationservice.dto.ApplicationTimelineResponse;
import com.schemebridge.applicationservice.enums.ApplicationStage;
import com.schemebridge.applicationservice.enums.ApplicationStatus;
import com.schemebridge.applicationservice.repository.ApplicationRepository;
import com.schemebridge.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * ApplicationTimelineService manages the tracking history of an application.
 * Every status change appends an immutable TrackingHistoryItem to the application document.
 * The tracking history is the complete audit trail of the application lifecycle.
 */
@Service
@RequiredArgsConstructor
public class ApplicationTimelineService {

    private final ApplicationRepository applicationRepository;

    /**
     * Records a status/stage transition in the application's tracking history.
     */
    public void addTrackingEntry(ApplicationDocument application, ApplicationStatus newStatus,
                                 ApplicationStage newStage, String performedBy,
                                 String performedByName, String remarks, String action) {
        TrackingHistoryItem entry = TrackingHistoryItem.builder()
                .status(newStatus)
                .stage(newStage)
                .timestamp(LocalDateTime.now())
                .performedBy(performedBy)
                .performedByName(performedByName)
                .remarks(remarks)
                .action(action)
                .build();

        application.getTrackingHistory().add(entry);
        application.setApplicationStatus(newStatus);
        application.setApplicationStage(newStage);
        application.setLastUpdatedAt(LocalDateTime.now());
    }

    /**
     * Retrieves the full application timeline for the GET /timeline endpoint.
     */
    public ApplicationTimelineResponse getTimeline(String applicationId) {
        ApplicationDocument app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        return ApplicationTimelineResponse.builder()
                .applicationId(app.getId())
                .applicationNumber(app.getApplicationNumber())
                .currentStatus(app.getApplicationStatus().name())
                .currentStage(app.getApplicationStage() != null ? app.getApplicationStage().name() : null)
                .timeline(app.getTrackingHistory())
                .totalEvents(app.getTrackingHistory().size())
                .build();
    }
}
