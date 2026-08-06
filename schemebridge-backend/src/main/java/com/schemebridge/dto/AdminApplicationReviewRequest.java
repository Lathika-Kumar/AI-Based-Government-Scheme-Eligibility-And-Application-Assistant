package com.schemebridge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminApplicationReviewRequest {
    @NotBlank(message = "Officer remarks are required")
    private String remarks;
}
