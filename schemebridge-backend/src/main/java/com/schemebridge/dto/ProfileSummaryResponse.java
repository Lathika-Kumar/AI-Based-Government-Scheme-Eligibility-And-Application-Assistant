package com.schemebridge.dto;

import com.schemebridge.enums.ProfileCompletionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileSummaryResponse {

    private int completionPercentage;
    private ProfileCompletionStatus status;
    private String fullName;
    private String state;
    private String district;
    private boolean onboardingCompleted;
}
