package com.schemebridge.mapper;

import com.schemebridge.dto.ApplicationRequest;
import com.schemebridge.dto.ApplicationResponse;
import com.schemebridge.dto.ApplicationTimelineEntryResponse;
import com.schemebridge.entity.Application;
import com.schemebridge.entity.ApplicationTimelineEntry;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ApplicationMapper {

    public ApplicationResponse toApplicationResponse(Application application) {
        if (application == null) return null;

        return ApplicationResponse.builder()
                .id(application.getId())
                .userId(application.getUserId())
                .schemeId(application.getSchemeId())
                .schemeName(application.getSchemeName())
                .ministry(application.getMinistry())
                .referenceNumber(application.getReferenceNumber())
                .remarks(application.getRemarks())
                .documents(application.getDocuments())
                .timeline(toTimelineResponse(application.getTimeline()))
                .status(application.getStatus())
                .submittedAt(application.getSubmittedAt())
                .withdrawnAt(application.getWithdrawnAt())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }

    public Application toEntity(ApplicationRequest request, String userId) {
        if (request == null) return null;

        return Application.builder()
                .userId(userId)
                .schemeId(request.getSchemeId())
                .schemeName(request.getSchemeName())
                .ministry(request.getMinistry())
                .documents(request.getDocuments())
                .referenceNumber(generateReferenceNumber(request.getSchemeId()))
                .status(com.schemebridge.enums.ApplicationStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .timeline(List.of(ApplicationTimelineEntry.builder()
                        .stage(com.schemebridge.enums.ApplicationStatus.SUBMITTED)
                        .completedAt(Instant.now())
                        .notes("Application submitted successfully.")
                        .build()))
                .build();
    }

    private String generateReferenceNumber(String schemeId) {
        String prefix = schemeId != null ? schemeId.toUpperCase().replaceAll("[^A-Z0-9]", "").substring(0, Math.min(6, schemeId.length())) : "APP";
        return String.format("%s/%s/%s", prefix, java.time.LocalDate.now().getYear(), java.time.temporal.ChronoField.DAY_OF_YEAR.getFrom(java.time.LocalDate.now()));
    }

    private List<ApplicationTimelineEntryResponse> toTimelineResponse(List<ApplicationTimelineEntry> timeline) {
        if (timeline == null) return null;
        return timeline.stream()
                .map(entry -> ApplicationTimelineEntryResponse.builder()
                        .stage(entry.getStage())
                        .completedAt(entry.getCompletedAt())
                        .notes(entry.getNotes())
                        .build())
                .collect(Collectors.toList());
    }
}
