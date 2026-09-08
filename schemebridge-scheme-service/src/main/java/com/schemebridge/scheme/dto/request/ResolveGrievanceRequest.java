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
public class ResolveGrievanceRequest {
    @NotBlank(message = "Resolution details are required")
    private String resolution;
}
