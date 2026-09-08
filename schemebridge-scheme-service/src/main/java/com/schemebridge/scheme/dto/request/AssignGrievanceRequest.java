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
public class AssignGrievanceRequest {
    @NotBlank(message = "Assigned officer ID is required")
    private String assignedTo;
}
