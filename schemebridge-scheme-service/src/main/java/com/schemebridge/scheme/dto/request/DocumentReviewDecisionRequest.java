package com.schemebridge.scheme.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentReviewDecisionRequest {
    private String decision; // APPROVE, REQUEST_CORRECTION, REJECT
    private String reason;
    private String remarks;
    private String correctionReason;
}
