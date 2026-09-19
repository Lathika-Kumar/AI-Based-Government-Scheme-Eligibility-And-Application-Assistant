package com.schemebridge.scheme.service.verification;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.document.VerificationCheckDetail;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BankPassbookValidator implements DocumentValidator {

    private static final Pattern PATTERN_IFSC = Pattern.compile("\\b([A-Z]{4}0[A-Z0-9]{6})\\b");
    private static final Pattern PATTERN_BANK_ACC = Pattern.compile("(?i)(?:Account\\s*(?:No|Number)|A/C\\s*No|खाता\\s*संख्या)[\\s:\\-]*([0-9]{9,18})");
    private static final Pattern PATTERN_NAME = Pattern.compile("(?i)(?:Name|Holder|A/C Holder)[\\s:\\-]*([A-Za-z\\s.]{3,40})");

    @Override
    public boolean supports(String documentCode) {
        if (documentCode == null) return false;
        String upper = documentCode.toUpperCase().trim();
        return upper.contains("BANK") || upper.contains("PASSBOOK");
    }

    @Override
    public DocumentValidationOutcome validate(String rawText, CitizenProfile profile) {
        String text = rawText != null ? rawText.toLowerCase() : "";
        Map<String, Object> fields = new HashMap<>();
        List<VerificationCheckDetail> checks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        boolean hasBankSignal = text.contains("passbook") || text.contains("bank") || text.contains("ifsc") || text.contains("savings account");
        boolean typeMatches = hasBankSignal;
        double typeScore = typeMatches ? 25.0 : 6.0;

        if (!typeMatches) {
            failures.add("Uploaded document does not appear to be an official Bank Passbook or statement.");
        }

        checks.add(VerificationCheckDetail.builder()
                .check("DOCUMENT_TYPE")
                .status(typeMatches ? "PASSED" : "FAILED")
                .message(typeMatches ? "Document matches Bank Passbook / Account statement layout" : "Missing Bank Passbook markers")
                .score(typeScore)
                .weight(25.0)
                .build());

        Matcher mIfsc = PATTERN_IFSC.matcher(rawText != null ? rawText : "");
        if (mIfsc.find()) {
            fields.put("ifsc", mIfsc.group(1));
        }

        Matcher mAcc = PATTERN_BANK_ACC.matcher(rawText != null ? rawText : "");
        if (mAcc.find()) {
            String fullAcc = mAcc.group(1);
            String last4 = fullAcc.length() > 4 ? fullAcc.substring(fullAcc.length() - 4) : fullAcc;
            fields.put("accountLast4", last4);
            fields.put("accountNumberMasked", "XXXX" + last4);
        }

        Matcher mName = PATTERN_NAME.matcher(rawText != null ? rawText : "");
        if (mName.find()) {
            fields.put("accountHolderName", mName.group(1).trim());
            fields.put("name", mName.group(1).trim());
        }

        int fieldsFound = (fields.containsKey("ifsc") ? 1 : 0) +
                (fields.containsKey("accountLast4") ? 1 : 0) +
                (fields.containsKey("accountHolderName") ? 1 : 0);

        double reqScore = fieldsFound >= 2 ? 20.0 : (fieldsFound == 1 ? 12.0 : 4.0);
        checks.add(VerificationCheckDetail.builder()
                .check("REQUIRED_FIELDS")
                .status(fieldsFound >= 2 ? "PASSED" : (fieldsFound == 1 ? "WARNING" : "FAILED"))
                .message(fieldsFound >= 2 ? "Extracted IFSC, masked account, and holder name" : "Incomplete banking details extracted")
                .score(reqScore)
                .weight(20.0)
                .build());

        double patternScore = fields.containsKey("ifsc") ? 5.0 : 2.0;
        checks.add(VerificationCheckDetail.builder()
                .check("PATTERN_VALIDITY")
                .status(fields.containsKey("ifsc") ? "PASSED" : "WARNING")
                .message(fields.containsKey("ifsc") ? "IFSC pattern conforms to RBI standard" : "IFSC pattern not found")
                .score(patternScore)
                .weight(5.0)
                .build());

        fields.put("documentType", "Bank Passbook");

        return DocumentValidationOutcome.builder()
                .canonicalDocumentType("Bank Passbook")
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
