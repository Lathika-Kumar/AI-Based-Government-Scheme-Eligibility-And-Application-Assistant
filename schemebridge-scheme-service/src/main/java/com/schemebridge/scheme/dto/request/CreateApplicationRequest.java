package com.schemebridge.scheme.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApplicationRequest {
    @NotBlank(message = "Scheme code is required")
    private String schemeCode;

    @Valid
    private CitizenEligibilityProfile profile;

    private String previousApplicationId;
}
