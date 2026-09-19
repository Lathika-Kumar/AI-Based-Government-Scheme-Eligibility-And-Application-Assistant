package com.schemebridge.scheme.service.verification;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.ocr.DocumentOcrProvider;
import com.schemebridge.scheme.repository.ApplicationDocumentRepository;
import com.schemebridge.scheme.repository.DocumentVerificationResultRepository;
import com.schemebridge.scheme.service.ApplicationEventService;
import com.schemebridge.scheme.service.DetailedDocumentStatusTransitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;

/**
 * End-to-end AI-assisted document verification engine.
 * Computes an explainable 100-point verification score across 8 distinct dimensions,
 * enforces hard failure rules, and records audit findings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentVerificationService {

    private final SecureFileValidator fileValidator;
    private final DocumentQualityAnalyzer qualityAnalyzer;
    private final List<DocumentValidator> documentValidators;
    private final ProfileConsistencyChecker consistencyChecker;
    private final DocumentOcrProvider ocrProvider;
    private final DocumentVerificationResultRepository verificationResultRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final DetailedDocumentStatusTransitionService transitionService;
    private final ApplicationEventService applicationEventService;

    @Transactional
    public DocumentVerificationResult verifyUploadedDocument(
            ApplicationDocument doc,
            MultipartFile file,
            CitizenProfile profile,
            String actorId
    ) {
        long startTime = System.currentTimeMillis();
        String applicationId = doc.getApplicationId();
        String documentCode = doc.getDocumentCode();
        int version = doc.getVersion() != null ? doc.getVersion() : 1;

        log.info("Starting AI-assisted document verification for appId={}, docCode={}, version={}", applicationId, documentCode, version);

        // Transition to PROCESSING
        try {
            transitionService.transitionDocumentStatus(doc, DetailedDocumentStatus.PROCESSING, actorId, "AI Document verification pipeline initiated.");
        } catch (Exception e) {
            log.debug("Status transition to PROCESSING skipped or already at PROCESSING: {}", e.getMessage());
        }

        List<VerificationCheckDetail> allChecks = new ArrayList<>();
        List<String> allWarnings = new ArrayList<>();
        List<String> failureReasons = new ArrayList<>();
        Map<String, Object> finalExtractedFields = new HashMap<>();

        // 1. File Validation
        SecureFileValidator.FileValidationResult fileResult = fileValidator.validate(file);
        if (!fileResult.isValid()) {
            allChecks.add(VerificationCheckDetail.builder()
                    .check("FILE_INTEGRITY")
                    .status("FAILED")
                    .message(fileResult.getErrorMessage())
                    .score(0.0)
                    .weight(5.0)
                    .build());
            failureReasons.add(fileResult.getErrorMessage());

            return buildAndSaveResult(doc, profile, 0.0, "AI_REJECTED", "File validation failed: " + fileResult.getErrorMessage(),
                    null, allChecks, finalExtractedFields, allWarnings, failureReasons, startTime, false, null);
        }

        String sha256 = fileResult.getSha256();
        doc.setSha256(sha256);

        // 2. Duplicate Check
        boolean isDuplicate = false;
        String duplicateRef = null;
        Optional<DocumentVerificationResult> existingWithHash = verificationResultRepository.findFirstBySha256AndDocumentIdNot(sha256, doc.getId());
        if (existingWithHash.isPresent()) {
            isDuplicate = true;
            duplicateRef = existingWithHash.get().getDocumentId();
            allWarnings.add("Identical document hash detected on another record (ID: " + duplicateRef + "). Requires officer review.");
        }

        double integrityScore = isDuplicate ? 2.0 : 5.0;
        allChecks.add(VerificationCheckDetail.builder()
                .check("FILE_INTEGRITY")
                .status(isDuplicate ? "WARNING" : "PASSED")
                .message(isDuplicate ? "Duplicate document hash detected across system" : "File structure, MIME type, and SHA-256 integrity confirmed")
                .score(integrityScore)
                .weight(5.0)
                .build());

        // 3. OCR / Text Extraction
        String rawText = "";
        Double ocrConfidence = 0.85;
        if (ocrProvider == null || !ocrProvider.isAvailable()) {
            allChecks.add(VerificationCheckDetail.builder()
                    .check("OCR_QUALITY")
                    .status("FAILED")
                    .message("OCR service unavailable")
                    .score(0.0)
                    .weight(15.0)
                    .build());
            allWarnings.add("OCR service unavailable. Verification requires manual officer inspection.");
            return buildAndSaveResult(doc, profile, 25.0, "AI_REVIEW_REQUIRED", "OCR service unavailable",
                    sha256, allChecks, finalExtractedFields, allWarnings, failureReasons, startTime, isDuplicate, duplicateRef);
        }

        try (InputStream stream = file.getInputStream()) {
            DocumentOcrResult ocrRes = ocrProvider.process(stream, documentCode, file.getOriginalFilename(), file.getContentType());
            rawText = ocrRes.getRawText() != null ? ocrRes.getRawText() : "";
            ocrConfidence = ocrRes.getOverallConfidence();
        } catch (Exception e) {
            log.warn("Failed to extract text during AI verification: {}", e.getMessage());
            rawText = "";
        }

        // 4. Quality Analysis (15% Weight)
        DocumentQualityAnalyzer.QualityAnalysisResult qualityRes = qualityAnalyzer.analyze(rawText, file.getSize(), file.getContentType(), ocrConfidence);
        allChecks.add(VerificationCheckDetail.builder()
                .check("OCR_QUALITY")
                .status(qualityRes.isReadable() ? "PASSED" : "FAILED")
                .message(qualityRes.isReadable() ? "Document text is clear and readable" : "Document is blurry, blank, or unreadable")
                .score(qualityRes.getScore())
                .weight(15.0)
                .build());
        if (!qualityRes.isReadable()) {
            failureReasons.addAll(qualityRes.getIssues());
        }

        // 5. Document Type & Field Extraction (Type: 25%, Fields: 20%, Pattern: 5%)
        DocumentValidator validator = resolveValidator(documentCode);
        DocumentValidator.DocumentValidationOutcome outcome = validator.validate(rawText, profile);

        allChecks.addAll(outcome.getChecks());
        allWarnings.addAll(outcome.getWarnings());
        failureReasons.addAll(outcome.getFailureReasons());
        finalExtractedFields.putAll(outcome.getExtractedFields());
        doc.setDocumentType(outcome.getCanonicalDocumentType());

        // 6. Profile Consistency Check (Name: 15%, DOB: 10%, State: 5% => 30%)
        ProfileConsistencyChecker.ConsistencyResult consistencyRes = consistencyChecker.checkConsistency(finalExtractedFields, rawText, profile);
        allChecks.addAll(consistencyRes.getChecks());
        if (!consistencyRes.getInconsistencies().isEmpty()) {
            allWarnings.addAll(consistencyRes.getInconsistencies());
        }

        // 7. Calculate 100-Point Score
        double calculatedScore = integrityScore + qualityRes.getScore() +
                outcome.getTypeMatchScore() + outcome.getRequiredFieldsScore() + outcome.getPatternValidityScore() +
                consistencyRes.getTotalConsistencyScore();

        calculatedScore = Math.min(100.0, Math.max(0.0, Math.round(calculatedScore * 10.0) / 10.0));

        // 8. Decision Rules (Hard Failures vs AI Verified vs Review Required)
        String decision;
        String summaryReason;

        if (!outcome.isTypeMatches()) {
            decision = "AI_REJECTED";
            summaryReason = "Uploaded document does not appear to match the selected document type (" + outcome.getCanonicalDocumentType() + ").";
        } else if (!qualityRes.isReadable()) {
            decision = "AI_REVIEW_REQUIRED";
            summaryReason = "Document text is unreadable or blurry; officer manual review required.";
        } else if (isDuplicate) {
            decision = "AI_REVIEW_REQUIRED";
            summaryReason = "Duplicate document detected; requires administrative verification.";
        } else if (!failureReasons.isEmpty()) {
            decision = "AI_REVIEW_REQUIRED";
            summaryReason = "Inconsistencies detected during AI inspection: " + String.join("; ", failureReasons);
        } else if (calculatedScore >= 70.0) {
            decision = "AI_VERIFIED";
            summaryReason = "AI document verification passed successfully. Awaiting final administrative officer approval.";
        } else if (calculatedScore < 45.0) {
            decision = "AI_REJECTED";
            summaryReason = "Document verification failed minimum confidence threshold (" + calculatedScore + "%).";
        } else {
            decision = "AI_REVIEW_REQUIRED";
            summaryReason = "AI verification score is marginal (" + calculatedScore + "%); requires officer review.";
        }

        return buildAndSaveResult(doc, profile, calculatedScore, decision, summaryReason,
                sha256, allChecks, finalExtractedFields, allWarnings, failureReasons, startTime, isDuplicate, duplicateRef);
    }

    private DocumentValidator resolveValidator(String docCode) {
        for (DocumentValidator v : documentValidators) {
            if (!(v instanceof DefaultDocumentValidator) && v.supports(docCode)) {
                return v;
            }
        }
        return documentValidators.stream()
                .filter(v -> v instanceof DefaultDocumentValidator)
                .findFirst()
                .orElse(new DefaultDocumentValidator());
    }

    private DocumentVerificationResult buildAndSaveResult(
            ApplicationDocument doc,
            CitizenProfile profile,
            double overallScore,
            String aiDecision,
            String reason,
            String sha256,
            List<VerificationCheckDetail> checks,
            Map<String, Object> extractedFields,
            List<String> warnings,
            List<String> failures,
            long startTime,
            boolean isDuplicate,
            String duplicateRef
    ) {
        long duration = System.currentTimeMillis() - startTime;
        int version = doc.getVersion() != null ? doc.getVersion() : 1;

        DocumentVerificationResult result = DocumentVerificationResult.builder()
                .applicationId(doc.getApplicationId())
                .documentId(doc.getId())
                .documentCode(doc.getDocumentCode())
                .documentType(doc.getDocumentType() != null ? doc.getDocumentType() : "Government Document")
                .userId(doc.getUserId())
                .version(version)
                .sha256(sha256)
                .overallScore(overallScore)
                .aiStatus(aiDecision)
                .officerStatus("PENDING")
                .checks(checks)
                .extractedFields(extractedFields)
                .warnings(warnings)
                .rejectionReasons(failures)
                .processedAt(Instant.now())
                .processingDurationMs(duration)
                .remarks(reason)
                .duplicateDetected(isDuplicate)
                .duplicateReferenceId(duplicateRef)
                .build();

        // Upsert by app + code + version
        Optional<DocumentVerificationResult> existing = verificationResultRepository.findByApplicationIdAndDocumentCodeAndVersion(
                doc.getApplicationId(), doc.getDocumentCode(), version);
        existing.ifPresent(documentVerificationResult -> result.setId(documentVerificationResult.getId()));

        DocumentVerificationResult saved = verificationResultRepository.save(result);

        // Update ApplicationDocument metadata
        doc.setVerificationScore(overallScore);
        doc.setAiVerificationResult(aiDecision);

        DetailedDocumentStatus intermediateAiStatus;
        if ("AI_VERIFIED".equals(aiDecision) || "AI_PASSED".equals(aiDecision)) {
            intermediateAiStatus = DetailedDocumentStatus.AI_PASSED;
        } else if ("AI_REJECTED".equals(aiDecision) || "AI_FAILED".equals(aiDecision)) {
            intermediateAiStatus = DetailedDocumentStatus.AI_FAILED;
        } else {
            intermediateAiStatus = DetailedDocumentStatus.AI_FLAGGED;
        }

        try {
            transitionService.transitionDocumentStatus(doc, intermediateAiStatus, doc.getUserId(), reason);
        } catch (Exception e) {
            log.debug("Intermediate AI status transition to {} skipped: {}", intermediateAiStatus, e.getMessage());
        }

        try {
            transitionService.transitionDocumentStatus(doc, DetailedDocumentStatus.UNDER_OFFICER_REVIEW, doc.getUserId(),
                    "AI document verification completed (" + aiDecision + ", score: " + overallScore + "%). Awaiting administrative officer review.");
        } catch (Exception e) {
            log.warn("Failed to transition document status to UNDER_OFFICER_REVIEW: {}", e.getMessage());
            doc.setDetailedStatus(DetailedDocumentStatus.UNDER_OFFICER_REVIEW);
            doc.setVerificationStatus(DocumentVerificationStatus.PENDING);
            applicationDocumentRepository.save(doc);
        }

        // Record audit event
        if (applicationEventService != null) {
            applicationEventService.recordEvent(
                    doc.getApplicationId(),
                    doc.getUserId(),
                    ApplicationEventType.DOCUMENT_STATUS_CHANGED,
                    null,
                    null,
                    "AI Document Verification completed with status: " + aiDecision + " (Score: " + overallScore + "%)",
                    Map.of(
                            "documentCode", doc.getDocumentCode(),
                            "aiStatus", aiDecision,
                            "score", String.valueOf(overallScore),
                            "version", version
                    )
            );
        }

        return saved;
    }
}
