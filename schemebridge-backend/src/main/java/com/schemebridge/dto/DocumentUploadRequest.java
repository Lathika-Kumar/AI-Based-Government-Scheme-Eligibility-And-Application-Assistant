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
public class DocumentUploadRequest {

    @NotBlank(message = "Document type is required")
    private String documentType;

    @NotBlank(message = "Document name is required")
    private String documentName;

    private String remarks;
    private String issuer;
    private String expiryDate;
    private String source;
    private List<String> linkedSchemes;
}
