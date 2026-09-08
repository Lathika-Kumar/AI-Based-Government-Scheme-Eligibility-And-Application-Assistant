package com.schemebridge.scheme.dto.request;

import com.schemebridge.scheme.document.GrievancePriority;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGrievanceRequest {
    private String applicationId;
    private String schemeCode;

    @NotBlank(message = "Category is required")
    private String category;

    private String subject;

    @NotBlank(message = "Description is required")
    private String description;

    private GrievancePriority priority;
}
