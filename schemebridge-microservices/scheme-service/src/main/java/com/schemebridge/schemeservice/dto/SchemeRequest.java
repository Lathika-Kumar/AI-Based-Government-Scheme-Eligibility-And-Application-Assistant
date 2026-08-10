package com.schemebridge.schemeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeRequest {

    @NotBlank(message = "Scheme code is required")
    private String schemeCode;

    @NotBlank(message = "Title in English is required")
    private String titleEnglish;

    private String titleTamil;
    private String descriptionEnglish;
    private String descriptionTamil;
    private String categoryCode;
    private String departmentCode;
    private String schemeType; // CENTRAL, STATE, CENTRALLY_SPONSORED
    private Integer launchYear;
    private String schemeUrl;
    private String applicationUrl;
    private String helplineNumber;
    private Boolean stateSpecific;
    private String applicableStates;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer priority;
    private BigDecimal popularityScore;
    private Boolean featured;
    private Boolean newlyAdded;
}
