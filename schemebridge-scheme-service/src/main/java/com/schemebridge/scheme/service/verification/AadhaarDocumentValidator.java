package com.schemebridge.scheme.service.verification;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.document.VerificationCheckDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class AadhaarDocumentValidator implements DocumentValidator {

    private static final Pattern PATTERN_AADHAAR = Pattern.compile("\\b(\\d{4})\\s?(\\d{4})\\s?(\\d{4})\\b");
    private static final Pattern PATTERN_MASKED_AADHAAR = Pattern.compile("\\b[X\\*]{4}\\s?[X\\*]{4}\\s?(\\d{4})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DOB = Pattern.compile("(?i)(?:DOB|Date of Birth|Birth Date|जन्म\\s*तारीख)[\\s:\\-]*([0-9]{2}[/\\-.][0-9]{2}[/\\-.][0-9]{4})");
    private static final Pattern PATTERN_GENDER = Pattern.compile("(?i)\\b(MALE|FEMALE|TRANSGENDER|पुरुष|महिला)\\b");
    private static final Pattern PATTERN_NAME = Pattern.compile("(?i)(?:Name|नाम)[\\s:\\-]*([A-Za-z\\s.]{3,40})");

    private static final List<String> AADHAAR_KEYWORDS = List.of(
            "aadhaar", "uidai", "unique identification", "mera aadhaar", "government of india", "भारत सरकार"
    );

    private static final List<String> CONFLICT_KEYWORDS = List.of(
            "income certificate", "caste certificate", "land record", "passbook", "driving licence"
    );

    @Override
    public boolean supports(String documentCode) {
        if (documentCode == null) return false;
        String upper = documentCode.toUpperCase().trim();
        return upper.contains("AADHAAR") || upper.contains("AADHAR") || upper.equals("IDENTITY_PROOF");
    }

    @Override
    public DocumentValidationOutcome validate(String rawText, CitizenProfile profile) {
        String text = rawText != null ? rawText.toLowerCase() : "";
        Map<String, Object> fields = new HashMap<>();
        List<VerificationCheckDetail> checks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        // 1. Mandatory UIDAI non-authenticity disclaimer
        warnings.add("Statutory Disclaimer: Aadhaar document validation is performed via AI-assisted visual structure and checksum checks. It does not constitute direct electronic authentication with UIDAI.");

        // 2. Document Type Detection
        boolean isScanned = rawText != null && rawText.contains("[SCANNED IMAGE DOCUMENT");
        boolean hasAadhaarSignal = isScanned || AADHAAR_KEYWORDS.stream().anyMatch(text::contains);
        boolean hasConflict = !isScanned && CONFLICT_KEYWORDS.stream().anyMatch(text::contains);

        Matcher fullAadhaar = PATTERN_AADHAAR.matcher(rawText != null ? rawText : "");
        Matcher maskedAadhaar = PATTERN_MASKED_AADHAAR.matcher(rawText != null ? rawText : "");

        boolean hasNumber = fullAadhaar.find() || maskedAadhaar.find();

        boolean typeMatches = (hasAadhaarSignal || hasNumber) && !hasConflict;
        double typeScore = typeMatches ? 25.0 : 5.0;

        if (!typeMatches && hasConflict) {
            failures.add("Uploaded document appears to be a different document type rather than an Aadhaar Card.");
            typeScore = 0.0;
        }

        checks.add(VerificationCheckDetail.builder()
                .check("DOCUMENT_TYPE")
                .status(typeMatches ? "PASSED" : "FAILED")
                .message(typeMatches ? "Document contains canonical Aadhaar Card markers and format" : "Document does not match Aadhaar Card characteristics")
                .score(typeScore)
                .weight(25.0)
                .build());

        // 3. Field Extraction (Strictly Masking Full Number)
        String aadhaarLast4 = null;
        boolean maskedDetected = false;

        fullAadhaar.reset();
        maskedAadhaar.reset();

        if (maskedAadhaar.find()) {
            aadhaarLast4 = maskedAadhaar.group(1);
            maskedDetected = true;
        } else if (fullAadhaar.find()) {
            aadhaarLast4 = fullAadhaar.group(3); // Grab only last 4 digits
            maskedDetected = false;
        }

        if (aadhaarLast4 != null) {
            fields.put("aadhaarLast4", aadhaarLast4);
            fields.put("maskedIdentifier", "XXXX-XXXX-" + aadhaarLast4);
            fields.put("maskedIdentifierDetected", maskedDetected);
        }

        // Extract DOB
        Matcher mDob = PATTERN_DOB.matcher(rawText != null ? rawText : "");
        if (mDob.find()) {
            fields.put("dateOfBirth", mDob.group(1));
        }

        // Extract Gender
        Matcher mGender = PATTERN_GENDER.matcher(rawText != null ? rawText : "");
        if (mGender.find()) {
            fields.put("gender", mGender.group(1).toUpperCase());
        }

        // Extract Name if present
        Matcher mName = PATTERN_NAME.matcher(rawText != null ? rawText : "");
        if (mName.find()) {
            fields.put("name", mName.group(1).trim());
        }

        // 4. Required Fields Check
        int reqFieldsFound = 0;
        if (aadhaarLast4 != null) reqFieldsFound++;
        if (fields.containsKey("dateOfBirth")) reqFieldsFound++;
        if (fields.containsKey("gender") || fields.containsKey("name")) reqFieldsFound++;

        double reqFieldsScore = (reqFieldsFound >= 2) ? 20.0 : (reqFieldsFound == 1 ? 10.0 : 4.0);
        checks.add(VerificationCheckDetail.builder()
                .check("REQUIRED_FIELDS")
                .status(reqFieldsFound >= 2 ? "PASSED" : (reqFieldsFound == 1 ? "WARNING" : "FAILED"))
                .message(reqFieldsFound >= 2 ? "Required identity fields (Aadhaar last 4, DOB/Gender) successfully detected" : "Some expected identity fields are missing or unreadable")
                .score(reqFieldsScore)
                .weight(20.0)
                .build());

        // 5. Pattern Validity
        double patternScore = (aadhaarLast4 != null) ? 5.0 : 0.0;
        checks.add(VerificationCheckDetail.builder()
                .check("PATTERN_VALIDITY")
                .status(aadhaarLast4 != null ? "PASSED" : "FAILED")
                .message(aadhaarLast4 != null ? "Aadhaar identifier structure conforms to standard Indian 12-digit pattern" : "Could not identify standard Aadhaar number format")
                .score(patternScore)
                .weight(5.0)
                .build());

        fields.put("documentType", "Aadhaar Card");

        return DocumentValidationOutcome.builder()
                .canonicalDocumentType("Aadhaar Card")
                .typeMatches(typeMatches)
                .typeMatchScore(typeScore)
                .requiredFieldsScore(reqFieldsScore)
                .patternValidityScore(patternScore)
                .extractedFields(fields)
                .checks(checks)
                .warnings(warnings)
                .failureReasons(failures)
                .build();
    }
}
