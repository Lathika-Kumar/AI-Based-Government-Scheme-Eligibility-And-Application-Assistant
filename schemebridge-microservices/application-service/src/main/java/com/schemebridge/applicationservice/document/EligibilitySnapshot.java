package com.schemebridge.applicationservice.document;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * EligibilitySnapshot captures the citizen's eligibility criteria at the time of application.
 * This is immutable after submission — even if the citizen profile changes later,
 * the application snapshot reflects the state at application time.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilitySnapshot {
    private String category;            // OBC, SC, ST, GENERAL, EWS
    private String gender;
    private Integer age;
    private BigDecimal annualIncome;
    private String state;
    private String district;
    private String occupationType;
    private Boolean isFarmer;
    private Boolean isDisabled;
    private Boolean isMinority;
    private Boolean isStudent;
    private Boolean isWidow;
    private Boolean isSeniorCitizen;
    private Double eligibilityScore;    // From scheme-service evaluate-eligibility
    private List<String> matchedRules;  // Rules that made this citizen eligible
    private LocalDateTime capturedAt;   // Time snapshot was taken
    private String schemeVersion;       // Scheme version at time of application
}
