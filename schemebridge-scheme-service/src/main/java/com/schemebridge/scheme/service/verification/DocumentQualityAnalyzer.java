package com.schemebridge.scheme.service.verification;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates document readability, text density, OCR confidence, and corruption indicators.
 */
@Component
@Slf4j
public class DocumentQualityAnalyzer {

    @Getter
    @Builder
    public static class QualityAnalysisResult {
        private final boolean readable;
        private final double score;       // 0 to 15.0 max weight
        private final double confidence;  // 0.0 to 1.0
        private final List<String> issues;
    }

    public QualityAnalysisResult analyze(String rawText, long fileSize, String contentType, Double providerConfidence) {
        List<String> issues = new ArrayList<>();
        double score = 15.0;
        double confidence = providerConfidence != null ? providerConfidence : 0.85;

        if (rawText == null || rawText.trim().isEmpty()) {
            issues.add("No readable text could be extracted from the uploaded document.");
            return QualityAnalysisResult.builder()
                    .readable(false)
                    .score(2.0)
                    .confidence(0.20)
                    .issues(issues)
                    .build();
        }

        String trimmed = rawText.trim();
        boolean isScannedDocument = trimmed.contains("[SCANNED IMAGE DOCUMENT");

        if (isScannedDocument) {
            // Scanned PDF pages rendered into images or uploaded document photos
            return QualityAnalysisResult.builder()
                    .readable(true)
                    .score(12.0)
                    .confidence(providerConfidence != null ? providerConfidence : 0.85)
                    .issues(List.of())
                    .build();
        }

        int charCount = trimmed.length();

        if (charCount < 20) {
            issues.add("Extracted text is very short (" + charCount + " characters). Document may be blurry or low resolution.");
            score = 6.0;
            confidence = Math.min(confidence, 0.40);
        } else if (charCount < 60) {
            score = 10.0;
            confidence = Math.min(confidence, 0.65);
        } else {
            score = 15.0;
        }

        // Check for gibberish / binary corruption signs in UTF-8 text
        long nonPrintableCount = trimmed.chars().filter(ch -> ch < 32 && ch != '\n' && ch != '\r' && ch != '\t').count();
        if (charCount > 0 && ((double) nonPrintableCount / charCount) > 0.10) {
            issues.add("Extracted text contains excessive non-printable characters, indicating potential file corruption.");
            score = Math.max(2.0, score - 8.0);
            confidence = Math.min(confidence, 0.30);
        }

        boolean readable = issues.isEmpty() || score >= 8.0;

        return QualityAnalysisResult.builder()
                .readable(readable)
                .score(score)
                .confidence(confidence)
                .issues(issues)
                .build();
    }
}
