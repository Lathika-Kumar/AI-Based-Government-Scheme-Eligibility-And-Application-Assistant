package com.schemebridge.coreservice.application.dto;

import com.schemebridge.coreservice.application.model.TrackingHistoryItem;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationTimelineResponse {
    private String applicationId;
    private String applicationNumber;
    private String currentStatus;
    private String currentStage;
    private List<TrackingHistoryItem> timeline;
    private int totalEvents;
}
