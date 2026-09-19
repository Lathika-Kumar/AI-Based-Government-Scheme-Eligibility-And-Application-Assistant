package com.schemebridge.scheme.document;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationCheckDetail {
    private String check;        // e.g. DOCUMENT_TYPE, OCR_QUALITY, REQUIRED_FIELDS, NAME_CONSISTENCY, DOB_CONSISTENCY, RESIDENCE_CONSISTENCY, PATTERN_VALIDITY, FILE_INTEGRITY
    private String status;       // PASSED, FAILED, WARNING, NOT_CHECKED
    private String message;
    private double score;
    private double weight;
}
