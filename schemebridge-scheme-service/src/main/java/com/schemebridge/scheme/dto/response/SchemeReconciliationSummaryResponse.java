package com.schemebridge.scheme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeReconciliationSummaryResponse {
    private long totalSchemes;
    private long matched;
    private long unmatched;
    private long ambiguous;
    private long conflicts;
    private long documentMapped;
    private long documentUnmapped;
    private long eligibilityComplete;
    private long benefitComplete;
    private long applicationComplete;
    private double averageCompletenessScore;
    private Map<String, Long> documentStatusBreakdown;
    private Map<String, Long> provenanceBreakdown;
    private Instant lastReconciledAt;
}
