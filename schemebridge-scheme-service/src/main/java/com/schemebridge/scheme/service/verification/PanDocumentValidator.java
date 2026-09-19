package com.schemebridge.scheme.service.verification;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.document.VerificationCheckDetail;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PanDocumentValidator implements DocumentValidator {

    private static final Pattern PATTERN_PAN = Pattern.compile("\\b([A-Z]{5})(\\d{4})([A-Z])\\b");
    private static final Pattern PATTERN_DOB = Pattern.compile("(?i)(?:DOB|Date of Birth)[\\s:\\-]*([0-9]{2}[/\\-.][0-9]{2}[/\\-.][0-9]{4})");
    private static final Pattern PATTERN_NAME = Pattern.compile("(?i)(?:Name)[\\s:\\-]*([A-Za-z\\s.]{3,40})");

    @Override
    public boolean supports(String documentCode) {
        if (documentCode == null) return false;
        String upper = documentCode.toUpperCase().trim();
        return upper.equals("PAN") || upper.equals("PAN_CARD");
    }

    @Override
    public DocumentValidationOutcome validate(String rawText, CitizenProfile profile) {
        String text = rawText != null ? rawText.toLowerCase() : "";
        Map<String, Object> fields = new HashMap<>();
        List<VerificationCheckDetail> checks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        boolean hasPanSignal = text.contains("income tax department") || text.contains("permanent account number") || text.contains("govt. of india");
        Matcher mPan = PATTERN_PAN.matcher(rawText != null ? rawText : "");
        boolean hasPanNumber = mPan.find();

        boolean typeMatches = hasPanSignal || hasPanNumber;
        double typeScore = typeMatches ? 25.0 : 5.0;

        if (!typeMatches) {
            failures.add("Uploaded document does not contain PAN card indicators or standard 10-character PAN format.");
        }

        checks.add(VerificationCheckDetail.builder()
                .check("DOCUMENT_TYPE")
                .status(typeMatches ? "PASSED" : "FAILED")
                .message(typeMatches ? "Document matches Permanent Account Number (PAN) layout" : "Uploaded file does not appear to be a PAN Card")
                .score(typeScore)
                .weight(25.0)
                .build());

        String panLast4 = null;
        if (hasPanNumber) {
            panLast4 = mPan.group(2);
            fields.put("panLast4", panLast4);
            fields.put("maskedIdentifier", "XXXXX" + panLast4 + mPan.group(3));
        }

        Matcher mDob = PATTERN_DOB.matcher(rawText != null ? rawText : "");
        if (mDob.find()) {
            fields.put("dateOfBirth", mDob.group(1));
        }

        Matcher mName = PATTERN_NAME.matcher(rawText != null ? rawText : "");
        if (mName.find()) {
            fields.put("name", mName.group(1).trim());
        }

        int reqCount = (panLast4 != null ? 1 : 0) + (fields.containsKey("dateOfBirth") ? 1 : 0) + (fields.containsKey("name") ? 1 : 0);
        double reqScore = reqCount >= 2 ? 20.0 : (reqCount == 1 ? 12.0 : 4.0);

        checks.add(VerificationCheckDetail.builder()
                .check("REQUIRED_FIELDS")
                .status(reqCount >= 2 ? "PASSED" : (reqCount == 1 ? "WARNING" : "FAILED"))
                .message(reqCount >= 2 ? "Mandatory PAN card fields successfully extracted" : "Incomplete PAN card data")
                .score(reqScore)
                .weight(20.0)
                .build());

        double patternScore = panLast4 != null ? 5.0 : 0.0;
        checks.add(VerificationCheckDetail.builder()
                .check("PATTERN_VALIDITY")
                .status(panLast4 != null ? "PASSED" : "FAILED")
                .message(panLast4 != null ? "PAN conforms to alphanumeric pattern (5 letters, 4 digits, 1 letter)" : "Invalid PAN pattern")
                .score(patternScore)
                .weight(5.0)
                .build());

        fields.put("documentType", "PAN Card");

        return DocumentValidationOutcome.builder()
                .canonicalDocumentType("PAN Card")
                .typeMatches(typeMatches)
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
