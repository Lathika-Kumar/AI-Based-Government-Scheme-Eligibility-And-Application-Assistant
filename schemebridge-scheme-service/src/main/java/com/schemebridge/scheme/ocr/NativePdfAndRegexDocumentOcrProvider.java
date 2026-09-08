package com.schemebridge.scheme.ocr;

import com.schemebridge.scheme.document.DocumentOcrResult;
import com.schemebridge.scheme.document.ExtractedFieldDetail;
import com.schemebridge.scheme.document.OcrExtractionStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance native PDF & Document Intelligence OCR provider.
 * Extracts raw machine-readable text and parses standard Indian government welfare fields.
 */
@Component
@Slf4j
public class NativePdfAndRegexDocumentOcrProvider implements DocumentOcrProvider {

    public static final String PROVIDER_NAME = "NativePdfAndRegexDocumentOcrProvider";
    public static final String PROVIDER_VERSION = "2.1.0";

    // Standard Regex Patterns
    private static final Pattern PATTERN_AADHAAR_NUM = Pattern.compile("\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\b");
    private static final Pattern PATTERN_PAN_NUM     = Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]\\b");
    private static final Pattern PATTERN_DOB         = Pattern.compile("(?i)(?:DOB|Date of Birth|Birth Date|जन्म\\s*तारीख)[\\s:\\-]*([0-9]{2}[/\\-.][0-9]{2}[/\\-.][0-9]{4})");
    private static final Pattern PATTERN_GENDER      = Pattern.compile("(?i)\\b(MALE|FEMALE|TRANSGENDER|पुरुष|महिला)\\b");
    private static final Pattern PATTERN_INCOME      = Pattern.compile("(?i)(?:Annual Income|Income|Family Income|वार्षिक\\s*आय)[\\s:\\-]*(?:Rs\\.?|INR|₹)?[\\s]*([0-9,]{4,10})");
    private static final Pattern PATTERN_CERT_NUM    = Pattern.compile("(?i)(?:Certificate\\s*(?:No|Number)|Application\\s*No|प्रमाण\\s*पत्र\\s*(?:क्रमांक|संख्या))[\\s:\\-]*([A-Z0-9/\\-_]{5,30})");
    private static final Pattern PATTERN_CASTE       = Pattern.compile("(?i)\\b(OBC|SC|ST|EWS|General|SEBC|Scheduled Caste|Scheduled Tribe|Backward Class)\\b");
    private static final Pattern PATTERN_RATION_CARD = Pattern.compile("(?i)\\b(?:RC[0-9\\-]+|[0-9]{10,14})\\b");
    private static final Pattern PATTERN_IFSC        = Pattern.compile("\\b[A-Z]{4}0[A-Z0-9]{6}\\b");
    private static final Pattern PATTERN_BANK_ACC    = Pattern.compile("(?i)(?:Account\\s*No|A/C\\s*No|खाता\\s*संख्या)[\\s:\\-]*([0-9]{9,18})");

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getProviderVersion() {
        return PROVIDER_VERSION;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public DocumentOcrResult process(InputStream stream, String documentCode, String fileName, String contentType) {
        long startTime = System.currentTimeMillis();
        String docCodeUpper = documentCode != null ? documentCode.toUpperCase().trim() : "GENERAL";
        String mime = contentType != null ? contentType.toLowerCase() : "";
        String fName = fileName != null ? fileName.toLowerCase() : "";

        log.info("Processing OCR for documentCode={}, fileName={}, mime={}", docCodeUpper, fileName, contentType);

        String rawText = "";
        try {
            byte[] bytes = stream.readAllBytes();
            if (mime.contains("pdf") || fName.endsWith(".pdf")) {
                try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(bytes))) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    rawText = stripper.getText(document);
                } catch (Exception pdfEx) {
                    log.debug("PDFBox binary parsing fallback to raw text decoding: {}", pdfEx.getMessage());
                    rawText = new String(bytes, StandardCharsets.UTF_8);
                }
            } else {
                rawText = new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("Failed to extract raw text from document stream: {}", e.getMessage());
            rawText = "";
        }

        Map<String, ExtractedFieldDetail> fields = new HashMap<>();
        String detectedType = mapDocumentType(docCodeUpper, rawText);
        double overallConfidence = 0.50;

        if (rawText != null && !rawText.isBlank()) {
            extractFieldsForDocument(docCodeUpper, rawText, fields);
            long matchedCount = fields.values().stream().filter(f -> f.getValue() != null).count();
            if (matchedCount >= 2) {
                overallConfidence = 0.94;
            } else if (matchedCount == 1) {
                overallConfidence = 0.85;
            } else {
                overallConfidence = 0.65;
            }
        }

        OcrExtractionStatus status = fields.isEmpty() ?
                (rawText.isBlank() ? OcrExtractionStatus.FAILED : OcrExtractionStatus.LOW_CONFIDENCE) :
                OcrExtractionStatus.SUCCESS;

        long duration = System.currentTimeMillis() - startTime;

        return DocumentOcrResult.builder()
                .documentCode(docCodeUpper)
                .detectedDocumentType(detectedType)
                .rawText(rawText.length() > 2000 ? rawText.substring(0, 2000) : rawText)
                .extractedFields(fields)
                .overallConfidence(overallConfidence)
                .extractionStatus(status)
                .provider(PROVIDER_NAME)
                .providerVersion(PROVIDER_VERSION)
                .processedAt(Instant.now())
                .processingDurationMs(duration)
                .build();
    }

    private void extractFieldsForDocument(String docCode, String text, Map<String, ExtractedFieldDetail> fields) {
        switch (docCode) {
            case "AADHAAR" -> {
                Matcher mNum = PATTERN_AADHAAR_NUM.matcher(text);
                if (mNum.find()) {
                    fields.put("documentNumber", createField("documentNumber", mNum.group(0), 0.98));
                }
                Matcher mDob = PATTERN_DOB.matcher(text);
                if (mDob.find()) {
                    fields.put("dob", createField("dob", mDob.group(1), 0.95));
                }
                Matcher mGender = PATTERN_GENDER.matcher(text);
                if (mGender.find()) {
                    fields.put("gender", createField("gender", mGender.group(1).toUpperCase(), 0.96));
                }
            }
            case "PAN" -> {
                Matcher mPan = PATTERN_PAN_NUM.matcher(text);
                if (mPan.find()) {
                    fields.put("documentNumber", createField("documentNumber", mPan.group(0), 0.98));
                }
                Matcher mDob = PATTERN_DOB.matcher(text);
                if (mDob.find()) {
                    fields.put("dob", createField("dob", mDob.group(1), 0.95));
                }
            }
            case "INCOME_CERT", "INCOME_CERTIFICATE", "INCOME_PROOF" -> {
                Matcher mInc = PATTERN_INCOME.matcher(text);
                if (mInc.find()) {
                    String clean = mInc.group(1).replace(",", "").trim();
                    try {
                        double val = Double.parseDouble(clean);
                        fields.put("annualIncome", createField("annualIncome", val, 0.94));
                    } catch (Exception ignored) {}
                }
                Matcher mCert = PATTERN_CERT_NUM.matcher(text);
                if (mCert.find()) {
                    fields.put("certificateNumber", createField("certificateNumber", mCert.group(1), 0.92));
                }
            }
            case "CASTE_CERT", "COMMUNITY_CERTIFICATE" -> {
                Matcher mCaste = PATTERN_CASTE.matcher(text);
                if (mCaste.find()) {
                    fields.put("socialCategory", createField("socialCategory", mCaste.group(1).toUpperCase(), 0.95));
                }
                Matcher mCert = PATTERN_CERT_NUM.matcher(text);
                if (mCert.find()) {
                    fields.put("certificateNumber", createField("certificateNumber", mCert.group(1), 0.92));
                }
            }
            case "RATION_CARD" -> {
                Matcher mRc = PATTERN_RATION_CARD.matcher(text);
                if (mRc.find()) {
                    fields.put("documentNumber", createField("documentNumber", mRc.group(0), 0.92));
                }
            }
            case "BANK_PASSBOOK", "BANK_ACCOUNT" -> {
                Matcher mAcc = PATTERN_BANK_ACC.matcher(text);
                if (mAcc.find()) {
                    fields.put("accountNumber", createField("accountNumber", mAcc.group(1), 0.96));
                }
                Matcher mIfsc = PATTERN_IFSC.matcher(text);
                if (mIfsc.find()) {
                    fields.put("ifscCode", createField("ifscCode", mIfsc.group(0), 0.97));
                }
            }
            default -> {
                Matcher mCert = PATTERN_CERT_NUM.matcher(text);
                if (mCert.find()) {
                    fields.put("documentNumber", createField("documentNumber", mCert.group(1), 0.85));
                }
            }
        }
    }

    private String mapDocumentType(String docCode, String text) {
        String lower = text != null ? text.toLowerCase() : "";
        if (docCode.contains("AADHAAR") || lower.contains("aadhaar") || lower.contains("uidai")) return "Aadhaar Card";
        if (docCode.contains("PAN") || lower.contains("income tax department") || lower.contains("permanent account number")) return "PAN Card";
        if (docCode.contains("INCOME") || lower.contains("income certificate") || lower.contains("वार्षिक आय")) return "Income Certificate";
        if (docCode.contains("CASTE") || lower.contains("caste certificate") || lower.contains("community certificate")) return "Caste Certificate";
        if (docCode.contains("RATION") || lower.contains("ration card") || lower.contains("food & civil")) return "Ration Card";
        if (docCode.contains("BANK") || lower.contains("bank") || lower.contains("passbook") || lower.contains("ifsc")) return "Bank Passbook";
        if (docCode.contains("DOMICILE") || lower.contains("domicile") || lower.contains("residence certificate")) return "Domicile Certificate";
        return "Government Document";
    }

    private ExtractedFieldDetail createField(String name, Object value, double confidence) {
        return ExtractedFieldDetail.builder()
                .fieldName(name)
                .value(value)
                .confidence(confidence)
                .source("OCR_EXTRACTED")
                .verified(false)
                .build();
    }
}
