package com.schemebridge.schemeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentRequirementResponse {
    private String id;
    private String documentName;
    private String documentType;
    private String description;
    private Boolean mandatory;
    private Integer displayOrder;
}
