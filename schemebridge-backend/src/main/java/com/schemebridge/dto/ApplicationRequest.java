package com.schemebridge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationRequest {

    @NotBlank(message = "Scheme ID is required")
    private String schemeId;

    @NotBlank(message = "Scheme name is required")
    private String schemeName;

    private String ministry;
    private List<String> documents;
}
