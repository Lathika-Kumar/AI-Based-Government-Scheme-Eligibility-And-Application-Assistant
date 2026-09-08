package com.schemebridge.scheme.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceTimelineEntry {
    private String id;
    private String authorId;
    private String authorRole;
    private String action;
    private String message;
    private Instant timestamp;
    private boolean internalOnly;
}
