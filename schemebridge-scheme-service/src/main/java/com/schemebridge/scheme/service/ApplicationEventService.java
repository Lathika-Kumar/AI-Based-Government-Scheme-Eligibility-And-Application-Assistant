package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.Application;
import com.schemebridge.scheme.document.ApplicationEvent;
import com.schemebridge.scheme.document.ApplicationEventType;
import com.schemebridge.scheme.document.ApplicationStatus;
import com.schemebridge.scheme.dto.response.ApplicationTimelineEventResponse;
import com.schemebridge.scheme.dto.response.ApplicationTimelineResponse;
import com.schemebridge.scheme.repository.ApplicationEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationEventService {

    private final ApplicationEventRepository applicationEventRepository;

    public void recordEvent(
            String applicationId,
            String userId,
            ApplicationEventType eventType,
            ApplicationStatus fromStatus,
            ApplicationStatus toStatus,
            String message,
            Map<String, Object> metadata
    ) {
        log.info("Recording application event: type={}, app={}, user={}", eventType, applicationId, userId);

        ApplicationEvent event = ApplicationEvent.builder()
                .applicationId(applicationId)
                .userId(userId)
                .eventType(eventType)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .message(message)
                .createdAt(Instant.now())
                .metadata(metadata)
                .build();

        applicationEventRepository.save(event);
    }

    public ApplicationTimelineResponse getTimeline(Application app) {
        List<ApplicationEvent> events = applicationEventRepository.findAllByApplicationIdOrderByCreatedAtAsc(app.getId());

        List<ApplicationTimelineEventResponse> eventResponses = events.stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());

        return ApplicationTimelineResponse.builder()
                .applicationId(app.getId())
                .applicationNumber(app.getApplicationNumber())
                .currentStatus(app.getStatus().name())
                .events(eventResponses)
                .build();
    }

    private ApplicationTimelineEventResponse mapToEventResponse(ApplicationEvent event) {
        return ApplicationTimelineEventResponse.builder()
                .eventType(event.getEventType().name())
                .fromStatus(event.getFromStatus() != null ? event.getFromStatus().name() : null)
                .toStatus(event.getToStatus() != null ? event.getToStatus().name() : null)
                .message(event.getMessage())
                .createdAt(event.getCreatedAt())
                .metadata(event.getMetadata())
                .build();
    }
}
