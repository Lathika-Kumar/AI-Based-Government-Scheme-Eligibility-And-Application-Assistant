package com.schemebridge.dto;

import com.schemebridge.enums.ProfileCompletionStatus;
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
public class ProfileCompletionResponse {

    private int completionPercentage;
    private ProfileCompletionStatus status;
    private Map<String, Integer> sectionScores;
    private List<String> completedFields;
    private List<String> missingFields;
    private boolean onboardingCompleted;
}
