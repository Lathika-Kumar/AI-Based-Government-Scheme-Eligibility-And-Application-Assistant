package com.schemebridge.scheme.service.verification;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.document.VerificationCheckDetail;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class IncomeCertificateValidator implements DocumentValidator {

    private static final Pattern PATTERN_INCOME = Pattern.compile("(?i)(?:Annual Income|Family Income|वार्षिक\\s*आय)[\\s:\\-]*(?:Rs\\.?|INR|₹)?[\\s]*([0-9,]{4,10})");
    private static final Pattern PATTERN_CERT_NUM = Pattern.compile("(?i)(?:Certificate\\s*(?:No|Number)|Application\\s*No|प्रमाण\\s*पत्र\\s*(?:क्रमांक|संख्या))[\\s:\\-]*([A-Z0-9/\\-_]{5,30})");
    private static final Pattern PATTERN_DATE = Pattern.compile("(?i)(?:Date of Issue|Dated|Issue Date|दिनांक)[\\s:\\-]*([0-9]{2}[/\\-.][0-9]{2}[/\\-.][0-9]{4})");
    private static final Pattern PATTERN_NAME = Pattern.compile("(?i)(?:Name|Applicant)[\\s:\\-]*([A-Za-z\\s.]{3,40})");

    @Override
    public boolean supports(String documentCode) {
        if (documentCode == null) return false;
        String upper = documentCode.toUpperCase().trim();
        return upper.contains("INCOME");
    }

    @Override
    public DocumentValidationOutcome validate(String rawText, CitizenProfile profile) {
        String text = rawText != null ? rawText.toLowerCase() : "";
        Map<String, Object> fields = new HashMap<>();
        List<VerificationCheckDetail> checks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        boolean hasIncomeSignal = text.contains("income certificate") || text.contains("वार्षिक आय") || text.contains("annual income") || text.contains("revenue department");
        boolean typeMatches = hasIncomeSignal;
        double typeScore = typeMatches ? 25.0 : 6.0;

        if (!typeMatches) {
            failures.add("Uploaded document does not appear to be an official Income Certificate.");
        }

        checks.add(VerificationCheckDetail.builder()
                .check("DOCUMENT_TYPE")
                .status(typeMatches ? "PASSED" : "FAILED")
                .message(typeMatches ? "Document matches State Revenue Department Income Certificate layout" : "Missing Income Certificate markers")
                .score(typeScore)
                .weight(25.0)
                .build());

        // Extract annual income
        Matcher mInc = PATTERN_INCOME.matcher(rawText != null ? rawText : "");
        if (mInc.find()) {
            try {
                String clean = mInc.group(1).replace(",", "").trim();
                fields.put("annualIncome", Double.parseDouble(clean));
            } catch (Exception ignored) {}
        }

        // Extract certificate number
        Matcher mCert = PATTERN_CERT_NUM.matcher(rawText != null ? rawText : "");
        if (mCert.find()) {
            fields.put("certificateNumber", mCert.group(1).trim());
        }

        // Extract issue date
        Matcher mDate = PATTERN_DATE.matcher(rawText != null ? rawText : "");
        if (mDate.find()) {
            fields.put("issueDate", mDate.group(1));
        }

        // Extract name
        Matcher mName = PATTERN_NAME.matcher(rawText != null ? rawText : "");
        if (mName.find()) {
            fields.put("name", mName.group(1).trim());
        }

        if (text.contains("tahsildar") || text.contains("revenue officer") || text.contains("sub-divisional magistrate")) {
            fields.put("issuingAuthority", "Revenue Department / Tahsildar");
        }

        int fieldsFound = (fields.containsKey("annualIncome") ? 1 : 0) +
                (fields.containsKey("certificateNumber") ? 1 : 0) +
                (fields.containsKey("issueDate") ? 1 : 0) +
                (fields.containsKey("name") ? 1 : 0);

        double reqScore = fieldsFound >= 2 ? 20.0 : (fieldsFound == 1 ? 12.0 : 4.0);
        checks.add(VerificationCheckDetail.builder()
                .check("REQUIRED_FIELDS")
                .status(fieldsFound >= 2 ? "PASSED" : (fieldsFound == 1 ? "WARNING" : "FAILED"))
                .message(fieldsFound >= 2 ? "Required income and certificate metadata extracted" : "Incomplete income certificate metadata")
                .score(reqScore)
                .weight(20.0)
                .build());

        double patternScore = fields.containsKey("certificateNumber") ? 5.0 : 2.0;
        checks.add(VerificationCheckDetail.builder()
                .check("PATTERN_VALIDITY")
                .status(fields.containsKey("certificateNumber") ? "PASSED" : "WARNING")
                .message(fields.containsKey("certificateNumber") ? "Certificate reference number format verified" : "Certificate number not clearly identifiable")
                .score(patternScore)
                .weight(5.0)
                .build());

        fields.put("documentType", "Income Certificate");

        return DocumentValidationOutcome.builder()
                .canonicalDocumentType("Income Certificate")
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
