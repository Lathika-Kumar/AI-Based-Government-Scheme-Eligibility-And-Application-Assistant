package com.schemebridge.coreservice.citizen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileCompletion {
    private Double completionPercentage;
    private Integer completedSections;
    private Integer totalSections;
    private List<String> missingFields;
    private Boolean isComplete;
}
