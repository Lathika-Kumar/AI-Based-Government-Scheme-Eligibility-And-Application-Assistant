package com.schemebridge.scheme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFieldExtraction {
    private Object value;
    private String status; // "FOUND", "NOT_FOUND", "UNCERTAIN"
    private Double confidence;
    private String source; // "PDF_TEXT", "OCR_VISUAL", "OCR_REGION", "MULTI_SOURCE", "NONE"
}
