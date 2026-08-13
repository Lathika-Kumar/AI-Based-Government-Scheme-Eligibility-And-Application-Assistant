package com.schemebridge.coreservice.citizen.dto;

import com.schemebridge.coreservice.citizen.model.ActivityHistoryItem;
import com.schemebridge.coreservice.citizen.model.DocumentReadiness;
import com.schemebridge.coreservice.citizen.model.ProfileCompletion;
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
