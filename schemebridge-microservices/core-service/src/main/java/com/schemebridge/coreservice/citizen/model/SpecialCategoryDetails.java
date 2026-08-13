package com.schemebridge.coreservice.citizen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialCategoryDetails {
    private Boolean isFarmer;
    private Boolean isDisabled;
    private Integer disabilityPercentage;
    private Boolean isMinority;
    private Boolean isExService;
}
