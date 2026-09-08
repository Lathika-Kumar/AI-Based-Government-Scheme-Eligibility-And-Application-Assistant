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

    private Integer rating;

    @NotBlank(message = "Comment is required")
    private String comment;

    private String relatedScheme;

    private String citizenEmail;

    private String citizenName;
}
