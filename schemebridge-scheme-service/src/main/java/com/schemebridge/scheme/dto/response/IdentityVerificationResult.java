package com.schemebridge.scheme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityVerificationResult {
    private String status; // "MATCH", "MISMATCH", "UNCERTAIN", "NOT_CHECKED"
    private boolean nameMatch;
    private Boolean dobMatch;
    private Boolean genderMatch;
    private boolean overallMatch;
    private String failureReason;
    private List<String> details;

    public Boolean isDobMatch() {
        return dobMatch != null && dobMatch;
    }
}
