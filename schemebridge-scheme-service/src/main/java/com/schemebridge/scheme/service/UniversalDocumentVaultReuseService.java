package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.CitizenVaultDocument;
import com.schemebridge.scheme.document.DocumentVerificationStatus;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.repository.CitizenVaultDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Universal Document Vault Reuse Service
 * 
 * Provides centralized, universal document-reuse capabilities across all supported document types
 * and all government schemes in SchemeBridge.
 * 
 * Invariants:
 * 1. Strict User Scoping: Never allow cross-citizen document access.
 * 2. Universal Matching: Matches canonical document codes and recognized synonyms rather than display names alone.
 * 3. Rigorous Validity Criteria: Disqualifies MISMATCH, REJECTED, UNCERTAIN, INVALID, or EXPIRED documents.
 * 4. Zero Physical Duplication: Applications reference the existing permanent vault document.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UniversalDocumentVaultReuseService {

    private final CitizenVaultDocumentRepository vaultDocumentRepository;

    /**
     * Universal canonical document type normalizer.
     * Maps codes, display names, and synonyms to canonical keys.
     */
    public String normalizeCanonicalType(String input) {
        if (!StringUtils.hasText(input)) {
            return "UNKNOWN";
        }
        String clean = input.trim().toUpperCase().replace("DOC_", "").replaceAll("[^A-Z0-9]+", "_");

        if (clean.contains("AADHAAR") || clean.contains("AADHAR") || clean.contains("UIDAI")) {
            return "AADHAAR";
        }
        if (clean.contains("INCOME")) {
            return "INCOME_CERTIFICATE";
        }
        if (clean.contains("CASTE") || clean.contains("COMMUNITY") || clean.contains("SOCIAL_CATEGORY")) {
            return "COMMUNITY_CERTIFICATE";
        }
        if (clean.contains("DOMICILE") || clean.contains("RESIDENCE") || clean.contains("NATIVITY")) {
            return "DOMICILE_CERTIFICATE";
        }
        if (clean.contains("BANK") || clean.contains("PASSBOOK") || clean.contains("CHEQUE")) {
            return "BANK_PASSBOOK";
        }
        if (clean.contains("EDUCATION") || clean.contains("BONAFIDE") || clean.contains("MARKSHEET") || clean.contains("STUDENT")) {
            return "EDUCATIONAL_CERTIFICATE";
        }
        if (clean.contains("DISABILITY") || clean.contains("UDID") || clean.contains("PWD") || clean.contains("DIVYANG") || clean.contains("HANDICAP")) {
            return "DISABILITY_CERTIFICATE";
        }
        if (clean.contains("LAND") || clean.contains("KHASRA") || clean.contains("KHATAUNI") || clean.contains("PATTA") || clean.contains("7_12")) {
            return "LAND_RECORDS";
        }
        if (clean.contains("GOVERNMENT_DOCUMENT") || clean.contains("GOVT_ID") || clean.contains("IDENTITY_PROOF") || clean.contains("ID_PROOF")) {
            return "GOVERNMENT_DOCUMENT";
        }
        if (clean.contains("BIRTH")) {
            return "BIRTH_CERTIFICATE";
        }
        if (clean.contains("RATION")) {
            return "RATION_CARD";
        }
        if (clean.contains("PAN")) {
            return "PAN_CARD";
        }

        return clean;
    }

    /**
     * Checks whether two document types or codes represent equivalent document requirements.
     */
    public boolean areEquivalentTypes(String type1, String type2) {
        if (!StringUtils.hasText(type1) || !StringUtils.hasText(type2)) {
            return false;
        }
        String canon1 = normalizeCanonicalType(type1);
        String canon2 = normalizeCanonicalType(type2);
        return canon1.equals(canon2);
    }

    /**
     * Validates whether a vault document meets all reusability criteria:
     * - Identity verification is MATCH or officer verified (NEVER MISMATCH, UNCERTAIN, PENDING identity, or FAILED)
     * - Document is NOT REJECTED, AI_REJECTED, AI_FAILED, CORRECTION_REQUIRED, REUPLOAD_REQUIRED, or NOT_UPLOADED
     * - Document is NOT expired (if expiry date applies)
     * - Document has physical storage backing (storageReference, gridFsFileId, or fileName)
     * - Document is in an accepted state (AI_VERIFIED, AI_PASSED, ADMIN_VERIFIED, or VERIFIED)
     */
    public boolean isValidAndReusable(CitizenVaultDocument vDoc) {
        if (vDoc == null) {
            return false;
        }

        // 1. Identity Check
        String idStatus = vDoc.getIdentityMatchStatus();
        if ("MISMATCH".equalsIgnoreCase(idStatus)
                || "UNCERTAIN".equalsIgnoreCase(idStatus)
                || "PENDING".equalsIgnoreCase(idStatus)
                || "FAILED".equalsIgnoreCase(idStatus)) {
            log.debug("Vault doc {} disqualified due to identity status: {}", vDoc.getId(), idStatus);
            return false;
        }

        // 2. Rejection & Error Check
        if (vDoc.getVerificationStatus() == DocumentVerificationStatus.REJECTED) {
            log.debug("Vault doc {} disqualified due to REJECTED verificationStatus", vDoc.getId());
            return false;
        }

        DetailedDocumentStatus dStatus = vDoc.getDetailedStatus();
        if (dStatus == DetailedDocumentStatus.REJECTED
                || dStatus == DetailedDocumentStatus.AI_REJECTED
                || dStatus == DetailedDocumentStatus.AI_FAILED
                || dStatus == DetailedDocumentStatus.CORRECTION_REQUIRED
                || dStatus == DetailedDocumentStatus.REUPLOAD_REQUIRED
                || dStatus == DetailedDocumentStatus.NOT_UPLOADED) {
            log.debug("Vault doc {} disqualified due to detailedStatus: {}", vDoc.getId(), dStatus);
            return false;
        }

        // 3. Expiry Check
        if (isExpired(vDoc.getExpiryDate())) {
            log.debug("Vault doc {} disqualified due to expiration date: {}", vDoc.getId(), vDoc.getExpiryDate());
            return false;
        }

        // 4. Physical Storage Backing Check
        if (!StringUtils.hasText(vDoc.getStorageReference())
                && !StringUtils.hasText(vDoc.getGridFsFileId())
                && !StringUtils.hasText(vDoc.getFileName())) {
            log.debug("Vault doc {} disqualified: No physical file backing", vDoc.getId());
            return false;
        }

        // 5. Verification Status Acceptance
        boolean isOfficerVerified = vDoc.isOfficerVerified()
                || dStatus == DetailedDocumentStatus.ADMIN_VERIFIED
                || dStatus == DetailedDocumentStatus.VERIFIED
                || vDoc.getVerificationStatus() == DocumentVerificationStatus.VERIFIED;

        boolean isAiValid = (dStatus == DetailedDocumentStatus.AI_VERIFIED || dStatus == DetailedDocumentStatus.AI_PASSED
                || "AI_PASSED".equalsIgnoreCase(vDoc.getAiVerificationResult()) || "AI_VERIFIED".equalsIgnoreCase(vDoc.getAiVerificationResult()))
                && ("MATCH".equalsIgnoreCase(idStatus) || "VERIFIED".equalsIgnoreCase(idStatus));

        // If score is present, enforce minimum passing threshold (e.g., >= 50)
        if (vDoc.getVerificationScore() != null && vDoc.getVerificationScore() < 50.0 && !isOfficerVerified) {
            log.debug("Vault doc {} disqualified due to low verification score: {}", vDoc.getId(), vDoc.getVerificationScore());
            return false;
        }

        return isOfficerVerified || isAiValid;
    }

    /**
     * Checks if an expiry date string represents an expired document.
     */
    private boolean isExpired(String expiryStr) {
        if (!StringUtils.hasText(expiryStr)
                || "No Expiration".equalsIgnoreCase(expiryStr.trim())
                || "Lifetime".equalsIgnoreCase(expiryStr.trim())
                || "Permanent".equalsIgnoreCase(expiryStr.trim())) {
            return false;
        }

        String cleaned = expiryStr.trim();
        try {
            Instant inst = Instant.parse(cleaned);
            return inst.isBefore(Instant.now());
        } catch (Exception ignored) {}

        String datePart = cleaned.contains("T") ? cleaned.substring(0, cleaned.indexOf('T')).trim() : cleaned;

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
        );

        for (DateTimeFormatter fmt : formatters) {
            try {
                LocalDate date = LocalDate.parse(datePart, fmt);
                return date.isBefore(LocalDate.now());
            } catch (DateTimeParseException ignored) {}
        }

        return false;
    }

    /**
     * Find a valid, matching document in the citizen's vault.
     * 
     * @param userId Authenticated citizen ID
     * @param reqDocumentCode Scheme requirement code (e.g., "AADHAAR", "DOC_INCOME")
     * @param reqCanonicalCode Scheme canonical code (e.g., "AADHAAR", "INCOME_CERTIFICATE")
     * @param reqDocumentName Scheme requirement display name (e.g., "Aadhaar Card")
     * @param alternatives Optional alternative document codes/names
     * @return Optional containing the matching valid CitizenVaultDocument
     */
    public Optional<CitizenVaultDocument> findReusableDocument(
            String userId,
            String reqDocumentCode,
            String reqCanonicalCode,
            String reqDocumentName,
            List<String> alternatives
    ) {
        if (!StringUtils.hasText(userId)) {
            return Optional.empty();
        }

        List<CitizenVaultDocument> userDocs = vaultDocumentRepository.findAllByUserId(userId);
        if (userDocs.isEmpty()) {
            return Optional.empty();
        }

        // Build normalized candidate keys for the requirement
        Set<String> candidateKeys = new HashSet<>();
        if (StringUtils.hasText(reqCanonicalCode)) {
            candidateKeys.add(normalizeCanonicalType(reqCanonicalCode));
        }
        if (StringUtils.hasText(reqDocumentCode)) {
            candidateKeys.add(normalizeCanonicalType(reqDocumentCode));
        }
        if (StringUtils.hasText(reqDocumentName)) {
            candidateKeys.add(normalizeCanonicalType(reqDocumentName));
        }
        if (alternatives != null) {
            for (String alt : alternatives) {
                if (StringUtils.hasText(alt)) {
                    candidateKeys.add(normalizeCanonicalType(alt));
                }
            }
        }
        candidateKeys.remove("UNKNOWN");

        for (CitizenVaultDocument vDoc : userDocs) {
            // Strict user ownership check
            if (!userId.equals(vDoc.getUserId())) {
                continue;
            }

            // Rigorous validity check
            if (!isValidAndReusable(vDoc)) {
                continue;
            }

            // Build normalized keys for the vault document
            Set<String> vaultKeys = new HashSet<>();
            if (StringUtils.hasText(vDoc.getCanonicalDocumentCode())) {
                vaultKeys.add(normalizeCanonicalType(vDoc.getCanonicalDocumentCode()));
            }
            if (StringUtils.hasText(vDoc.getDocumentCode())) {
                vaultKeys.add(normalizeCanonicalType(vDoc.getDocumentCode()));
            }
            if (StringUtils.hasText(vDoc.getDocumentName())) {
                vaultKeys.add(normalizeCanonicalType(vDoc.getDocumentName()));
            }
            if (StringUtils.hasText(vDoc.getDocumentType())) {
                vaultKeys.add(normalizeCanonicalType(vDoc.getDocumentType()));
            }
            vaultKeys.remove("UNKNOWN");

            // 1. Direct canonical intersection
            for (String cKey : candidateKeys) {
                if (vaultKeys.contains(cKey)) {
                    log.info("Vault reuse match found for user={} requirement={} via canonical key={}", userId, reqDocumentCode, cKey);
                    return Optional.of(vDoc);
                }
            }

            // 2. Tokenized domain match (e.g. "Aadhaar Card" vs "Aadhaar", "Caste" vs "Community")
            for (String cKey : candidateKeys) {
                for (String vKey : vaultKeys) {
                    if (areEquivalentTypes(cKey, vKey)) {
                        log.info("Vault reuse match found for user={} requirement={} via equivalence ({}, {})", userId, reqDocumentCode, cKey, vKey);
                        return Optional.of(vDoc);
                    }
                }
            }
        }

        return Optional.empty();
    }
}
