package com.schemebridge.applicationservice.document;

import com.schemebridge.applicationservice.enums.ApplicationStage;
import com.schemebridge.applicationservice.enums.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingHistoryItem {
    private ApplicationStatus status;
    private ApplicationStage stage;
    private LocalDateTime timestamp;
    private String performedBy;    // "CITIZEN", "OFFICER", "SYSTEM"
    private String performedByName;
    private String remarks;
    private String action;         // "SUBMITTED", "ASSIGNED", "APPROVED", "REJECTED", etc.
}
