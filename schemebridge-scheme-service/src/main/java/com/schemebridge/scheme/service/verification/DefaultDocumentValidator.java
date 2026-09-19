package com.schemebridge.scheme.service.verification;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.document.VerificationCheckDetail;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DefaultDocumentValidator implements DocumentValidator {

    private static final Pattern PATTERN_CERT_NUM = Pattern.compile("(?i)(?:Certificate\\s*(?:No|Number)|Application\\s*No|Record\\s*No|प्रमाण\\s*पत्र\\s*(?:क्रमांक|संख्या))[\\s:\\-]*([A-Z0-9/\\-_]{5,30})");
    private static final Pattern PATTERN_DATE = Pattern.compile("(?i)(?:Date|दिनांक)[\\s:\\-]*([0-9]{2}[/\\-.][0-9]{2}[/\\-.][0-9]{4})");
    private static final Pattern PATTERN_NAME = Pattern.compile("(?i)(?:Name|Applicant|Holder)[\\s:\\-]*([A-Za-z\\s.]{3,40})");

    @Override
    public boolean supports(String documentCode) {
        return true; // Fallback validator for any other document
    }

    @Override
    public DocumentValidationOutcome validate(String rawText, CitizenProfile profile) {
        Map<String, Object> fields = new HashMap<>();
        List<VerificationCheckDetail> checks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        boolean hasText = rawText != null && !rawText.trim().isEmpty();
        double typeScore = hasText ? 20.0 : 5.0;

        checks.add(VerificationCheckDetail.builder()
                .check("DOCUMENT_TYPE")
                .status(hasText ? "PASSED" : "WARNING")
                .message(hasText ? "Supporting document content successfully extracted" : "Document content is empty or unreadable")
                .score(typeScore)
                .weight(25.0)
                .build());

        Matcher mCert = PATTERN_CERT_NUM.matcher(rawText != null ? rawText : "");
        if (mCert.find()) {
            fields.put("certificateNumber", mCert.group(1).trim());
        }

        Matcher mDate = PATTERN_DATE.matcher(rawText != null ? rawText : "");
        if (mDate.find()) {
            fields.put("issueDate", mDate.group(1));
        }

        Matcher mName = PATTERN_NAME.matcher(rawText != null ? rawText : "");
        if (mName.find()) {
            fields.put("name", mName.group(1).trim());
        }

        double reqScore = !fields.isEmpty() ? 16.0 : (hasText ? 10.0 : 2.0);
        checks.add(VerificationCheckDetail.builder()
                .check("REQUIRED_FIELDS")
                .status(!fields.isEmpty() ? "PASSED" : "WARNING")
                .message(!fields.isEmpty() ? "Reference identification detected in supporting document" : "No structured reference fields found")
                .score(reqScore)
                .weight(20.0)
                .build());

        double patternScore = fields.containsKey("certificateNumber") ? 5.0 : 3.0;
        checks.add(VerificationCheckDetail.builder()
                .check("PATTERN_VALIDITY")
                .status("PASSED")
                .message("Document structure acceptable for officer review")
                .score(patternScore)
                .weight(5.0)
                .build());

        fields.put("documentType", "Supporting Document");

        return DocumentValidationOutcome.builder()
                .canonicalDocumentType("Supporting Document")
                .typeMatches(hasText)
                .typeMatchScore(typeScore)
                .requiredFieldsScore(reqScore)
                .patternValidityScore(patternScore)
                .extractedFields(fields)
                .checks(checks)
                .warnings(warnings)
                .failureReasons(failures)
                .build();
    }
}
