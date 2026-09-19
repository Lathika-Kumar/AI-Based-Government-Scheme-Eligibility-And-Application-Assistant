package com.schemebridge.scheme.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewActionRequest {

    @NotBlank(message = "Review action is mandatory. Supported actions: APPROVE, REJECT, REQUEST_MORE_DOCUMENTS")
    private String action;

    private String remarks;
    private String reason;
    private String reviewerNotes;
    private String correctionReason;
}
