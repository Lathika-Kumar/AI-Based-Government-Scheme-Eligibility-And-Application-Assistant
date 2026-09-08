package com.schemebridge.scheme.dto.request;

import lombok.*;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenEligibilityProfile {
    private Integer age;
    private String gender;
    private Double annualIncome;
    private String occupation;
    private String state;
    private String socialCategory;
    private Boolean disabilityStatus;

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
}
