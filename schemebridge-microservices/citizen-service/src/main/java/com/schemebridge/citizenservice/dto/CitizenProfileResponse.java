package com.schemebridge.citizenservice.dto;

import com.schemebridge.citizenservice.document.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenProfileResponse {
    private String id;
    private String authUserId;
    private PersonalDetails personalDetails;
    private ContactDetails contactDetails;
    private AddressDetails addressDetails;
    private FamilyDetails familyDetails;
    private IncomeDetails incomeDetails;
    private EducationDetails educationDetails;
    private OccupationDetails occupationDetails;
    private SpecialCategoryDetails specialCategoryDetails;
    private Preferences preferences;
    private EligibilitySnapshot eligibilitySnapshot;
    private ProfileCompletion profileCompletion;
    private DocumentReadiness documentReadiness;
    private Set<String> savedSchemes;
    private List<ActivityHistoryItem> activityHistory;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
