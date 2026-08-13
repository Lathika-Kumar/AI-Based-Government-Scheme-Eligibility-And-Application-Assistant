package com.schemebridge.coreservice.citizen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "citizens")
@CompoundIndexes({
    @CompoundIndex(name = "idx_state_district", def = "{'addressDetails.state': 1, 'addressDetails.district': 1}"),
    @CompoundIndex(name = "idx_category_income", def = "{'personalDetails.category': 1, 'incomeDetails.annualIncome': 1}"),
    @CompoundIndex(name = "idx_authUserId_active", def = "{'authUserId': 1, 'active': 1}")
})
public class CitizenDocument {

    @Id
    private String id;

    @Indexed(unique = true)
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

    @Indexed
    @Builder.Default
    private Set<String> savedSchemes = new HashSet<>();

    @Builder.Default
    private List<SavedScheme> savedSchemeObjects = new ArrayList<>();

    @Builder.Default
    private List<ActivityHistoryItem> activityHistory = new ArrayList<>();

    // Dashboard & Redis readiness fields
    private LocalDateTime dashboardLastCalculated;
    @Builder.Default
    private Long dashboardVersion = 1L;

    // Soft delete fields
    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Boolean deleted = false;

    private LocalDateTime deletedAt;

    // Audit fields
    private LocalDateTime createdAt;

    @Indexed
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;
}
