package com.schemebridge.scheme.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationTimelineResponse {
    private String applicationId;
    private String applicationNumber;
    private String currentStatus;
    private List<ApplicationTimelineEventResponse> events;
}
