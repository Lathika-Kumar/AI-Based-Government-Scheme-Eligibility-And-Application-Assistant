package com.schemebridge.entity;

import com.schemebridge.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationTimelineEntry {

    private ApplicationStatus stage;
    private Instant completedAt;
    private String notes;
}
