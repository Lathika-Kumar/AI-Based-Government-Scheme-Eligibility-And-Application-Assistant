package com.schemebridge.coreservice.application.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequiredDocumentRef {
    private String documentType;
    private String documentName;
    private boolean mandatory;
    private String description;
}
