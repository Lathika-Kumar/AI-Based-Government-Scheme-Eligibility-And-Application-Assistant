package com.schemebridge.applicationservice.dto;

import com.schemebridge.applicationservice.document.TrackingHistoryItem;
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
