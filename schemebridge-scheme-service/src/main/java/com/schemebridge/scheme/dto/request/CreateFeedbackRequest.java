package com.schemebridge.scheme.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeedbackRequest {

    @NotBlank(message = "Feedback type is required")
    private String type;

    @jakarta.validation.constraints.NotNull(message = "Rating is required")
    @jakarta.validation.constraints.Min(value = 1, message = "Rating must be between 1 and 5")
    @jakarta.validation.constraints.Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @NotBlank(message = "Comment is required")
    private String comment;

    private String relatedScheme;

    private String citizenEmail;

    private String citizenName;
}
