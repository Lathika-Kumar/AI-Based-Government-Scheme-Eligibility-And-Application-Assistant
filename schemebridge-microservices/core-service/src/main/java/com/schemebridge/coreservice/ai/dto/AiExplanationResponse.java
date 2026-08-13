package com.schemebridge.coreservice.ai.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiExplanationResponse {
    private String schemeCode;
    private String schemeName;
    private String language;
    private boolean isEligible;
    private Double matchScore;
    private String explanation;
    private String confidence;
    private List<String> keyTakeaways;
    private List<String> suggestedNextSteps;
    private boolean isFallback;
    private String timestamp;
}
