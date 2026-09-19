package com.schemebridge.scheme.service.verification;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.document.VerificationCheckDetail;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface DocumentValidator {

    boolean supports(String documentCode);

    DocumentValidationOutcome validate(String rawText, CitizenProfile profile);

    @Getter
    @Builder
    class DocumentValidationOutcome {
        private final String canonicalDocumentType;
        private final boolean typeMatches;
        private final double typeMatchScore;        // max 25.0
        private final double requiredFieldsScore;   // max 20.0
        private final double patternValidityScore;  // max 5.0

        @Builder.Default
        private final Map<String, Object> extractedFields = new HashMap<>();

        @Builder.Default
        private final List<VerificationCheckDetail> checks = new ArrayList<>();

        @Builder.Default
        private final List<String> warnings = new ArrayList<>();

        @Builder.Default
        private final List<String> failureReasons = new ArrayList<>();
    }
}
