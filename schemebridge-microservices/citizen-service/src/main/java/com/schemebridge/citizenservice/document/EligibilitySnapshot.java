package com.schemebridge.citizenservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilitySnapshot {
    private Double eligibilityScore;
    private Integer matchedSchemes;
    private LocalDateTime lastCalculated;
}
