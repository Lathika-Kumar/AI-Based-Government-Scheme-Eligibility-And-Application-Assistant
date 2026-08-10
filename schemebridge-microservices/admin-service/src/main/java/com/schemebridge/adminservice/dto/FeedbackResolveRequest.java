package com.schemebridge.adminservice.dto;

import com.schemebridge.adminservice.enums.FeedbackStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackResolveRequest {

    @NotNull(message = "Feedback status is required")
    private FeedbackStatus status;

    @NotBlank(message = "Resolution notes are required")
    private String resolutionNotes;

    private String assignedOfficerId;
}
