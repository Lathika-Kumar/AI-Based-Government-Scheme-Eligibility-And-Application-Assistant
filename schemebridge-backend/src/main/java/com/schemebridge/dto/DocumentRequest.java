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
public class DocumentRequest {

    @NotBlank(message = "Document name is required")
    private String name;

    @NotBlank(message = "Document type is required")
    private String type;

    private String issuer;
    private String expiryDate;
    private String source;
    private List<String> linkedSchemes;
}
