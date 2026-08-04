package com.schemebridge.dto;

import com.schemebridge.enums.EligibilityResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityCheckResponse {

    private String schemeId;
    private String schemeName;
    private boolean eligible;
    private int matchPercentage;
    private EligibilityResult eligibilityResult;
    private Map<String, Boolean> criteriaBreakdown;
    private List<String> matchedRules;
    private List<String> failedRules;
    private List<String> missingFields;
    private List<String> missingDocuments;
    private String explanation;
}
