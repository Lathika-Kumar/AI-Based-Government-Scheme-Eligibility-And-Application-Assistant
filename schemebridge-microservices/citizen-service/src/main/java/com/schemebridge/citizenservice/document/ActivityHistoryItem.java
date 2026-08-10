package com.schemebridge.citizenservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityHistoryItem {
    private String action; // PROFILE_CREATED, PROFILE_UPDATED, PROFILE_COMPLETED, SCHEME_SAVED, SCHEME_UNSAVED, ELIGIBILITY_RECALCULATED
    private String description;
    private String severity; // NORMAL, IMPORTANT, SYSTEM
    private LocalDateTime performedAt;
    private String performedBy;
    private String ipAddress;
    private String device;
}
