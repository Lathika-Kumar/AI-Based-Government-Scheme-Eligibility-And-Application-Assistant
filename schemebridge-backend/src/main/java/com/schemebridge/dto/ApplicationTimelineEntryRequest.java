package com.schemebridge.dto;

import com.schemebridge.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationTimelineEntryRequest {

    private ApplicationStatus stage;
    private Instant completedAt;
    private String notes;
}
