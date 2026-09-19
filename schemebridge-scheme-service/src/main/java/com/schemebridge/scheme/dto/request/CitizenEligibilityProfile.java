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
    private String district;
    private String socialCategory;
    private Boolean disabilityStatus;
    private Boolean isFarmer;
    private Boolean isStudent;
    private Boolean bplStatus;
    private String maritalStatus;
    private String employmentStatus;
    private String residentialAreaType;
    private String rationCardType;
    private Double landholdingArea;
    private Boolean minorityStatus;
    private Boolean isExServiceman;

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
}
