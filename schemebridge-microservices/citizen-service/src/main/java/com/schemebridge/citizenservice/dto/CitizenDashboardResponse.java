package com.schemebridge.citizenservice.dto;

import com.schemebridge.citizenservice.document.ActivityHistoryItem;
import com.schemebridge.citizenservice.document.DocumentReadiness;
import com.schemebridge.citizenservice.document.ProfileCompletion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenDashboardResponse {
    private ProfileCompletion profileCompletion;
    private Double eligibilityScore;
    private Integer savedSchemesCount;
    private Integer matchedSchemes;
    private DocumentReadiness documentReadiness;
    private List<ActivityHistoryItem> recentActivities;
    private Integer notificationsCount;
}
