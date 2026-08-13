package com.schemebridge.coreservice.citizen.dto;

import com.schemebridge.coreservice.citizen.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenProfileRequest {
    private PersonalDetails personalDetails;
    private ContactDetails contactDetails;
    private AddressDetails addressDetails;
    private FamilyDetails familyDetails;
    private IncomeDetails incomeDetails;
    private EducationDetails educationDetails;
    private OccupationDetails occupationDetails;
    private SpecialCategoryDetails specialCategoryDetails;
    private Preferences preferences;
}
