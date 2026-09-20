package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.ApplicationDocument;
import com.schemebridge.scheme.document.ApplicationDocumentVersion;
import com.schemebridge.scheme.document.CitizenVaultDocument;
import com.schemebridge.scheme.document.DocumentVerificationStatus;
import com.schemebridge.scheme.dto.response.DocumentDownloadDto;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.repository.CitizenVaultDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CitizenVaultDocumentService {

    private final CitizenVaultDocumentRepository vaultDocumentRepository;
    private final DocumentStorageService documentStorageService;
    private final DocumentExtractionService documentExtractionService;

    public List<CitizenVaultDocument> getVaultDocuments(String userId) {
        if (!StringUtils.hasText(userId)) {
            return Collections.emptyList();
        }
        return vaultDocumentRepository.findByUserIdOrderByUploadedAtDesc(userId);
    }

    public Optional<CitizenVaultDocument> getVaultDocumentById(String documentId, String userId, boolean isPrivileged) {
        return vaultDocumentRepository.findById(documentId).map(doc -> {
            if (!isPrivileged && !doc.getUserId().equals(userId)) {
                throw new SecurityException("Access denied: You do not own this document.");
            }
            return doc;
        });
    }

    @Transactional
    public CitizenVaultDocument uploadVaultDocument(
            String userId,
            String documentCode,
            String documentType,
            String documentName,
            String issuer,
            String expiryDate,
            String holderName,
            String docNumber,
            String source,
            MultipartFile file
    ) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("User ID is required for vault upload.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty.");
        }

        // 1. File size check (max 10MB for vault)
        long maxSize = 10 * 1024 * 1024L;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds 10MB limit.");
        }

        // 2. Format check
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf");
        String extension = "";
        int idx = originalFilename.lastIndexOf('.');
        if (idx >= 0) {
            extension = originalFilename.substring(idx + 1).toUpperCase();
        }
        List<String> allowedFormats = List.of("PDF", "JPG", "JPEG", "PNG");
        if (!allowedFormats.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file type: " + extension + ". Allowed: " + allowedFormats);
        }

        // 3. Compute SHA-256
        String sha256 = null;
        try (InputStream is = file.getInputStream()) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            sha256 = sb.toString();
        } catch (Exception e) {
            log.warn("Could not compute SHA-256 for vault file: {}", originalFilename);
        }

        // 4. Resolve normalized codes
        String resolvedName = StringUtils.hasText(documentName) ? documentName.trim() : originalFilename;
        String resolvedType = StringUtils.hasText(documentType) ? documentType.trim() : "Identity Proof";
        String normalizedCode = StringUtils.hasText(documentCode)
                ? documentCode.trim().toUpperCase()
                : deriveDocumentCode(resolvedName, resolvedType);

        String canonicalCode = deriveCanonicalCode(normalizedCode, resolvedName);

        // 5. Store physical file in GridFS
        String storageRef = documentStorageService.storeVaultDocument(userId, normalizedCode, file);

        // 6. Execute AI Document Extraction & Identity Binding against Authenticated Citizen
        com.schemebridge.scheme.dto.response.StructuredDocumentExtractionResponse extraction = null;
        try {
            extraction = documentExtractionService.extractAndVerify(file, normalizedCode, resolvedType, userId);
        } catch (Exception e) {
            log.warn("Document extraction during vault upload encountered error: {}", e.getMessage());
        }

        String extractedHolder = null;
        String extractedDob = null;
        String extractedDocNumber = null;
        String extractionStatus = "SUCCESS";
        String identityMatchStatus = "NOT_CHECKED";
        String extractionRejectionReason = null;

        if (extraction != null) {
            extractionStatus = extraction.getExtractionStatus();
            if (extraction.getFields() != null) {
                if (extraction.getFields().get("holderName") != null && extraction.getFields().get("holderName").getValue() != null) {
                    extractedHolder = extraction.getFields().get("holderName").getValue().toString();
                }
                if (extraction.getFields().get("dateOfBirth") != null && extraction.getFields().get("dateOfBirth").getValue() != null) {
                    extractedDob = extraction.getFields().get("dateOfBirth").getValue().toString();
                }
                if (extraction.getFields().get("documentNumber") != null && extraction.getFields().get("documentNumber").getValue() != null) {
                    extractedDocNumber = extraction.getFields().get("documentNumber").getValue().toString();
                }
            }
            if (extraction.getIdentityVerification() != null) {
                identityMatchStatus = extraction.getIdentityVerification().getStatus();
                if ("MISMATCH".equalsIgnoreCase(identityMatchStatus)) {
                    extractionRejectionReason = extraction.getIdentityVerification().getFailureReason();
                }
            }
        }

        boolean isIdentityMismatch = "MISMATCH".equalsIgnoreCase(identityMatchStatus);
        String finalHolderName = extractedHolder != null ? extractedHolder : holderName;
        String finalDocNumber = extractedDocNumber != null ? extractedDocNumber : docNumber;

        DetailedDocumentStatus initialDetailedStatus;
        DocumentVerificationStatus initialVerificationStatus;
        String initialAiDecision;
        Double initialScore;
        String initialRejection = null;

        if (isIdentityMismatch) {
            initialDetailedStatus = DetailedDocumentStatus.AI_REJECTED;
            initialVerificationStatus = DocumentVerificationStatus.REJECTED;
            initialAiDecision = "AI_REJECTED";
            initialScore = 20.0;
            initialRejection = extractionRejectionReason != null ? extractionRejectionReason :
                    "The uploaded document holder details do not match the authenticated applicant.";
        } else if ("MATCH".equalsIgnoreCase(identityMatchStatus)) {
            initialDetailedStatus = DetailedDocumentStatus.AI_VERIFIED;
            initialVerificationStatus = DocumentVerificationStatus.PENDING;
            initialAiDecision = "AI_VERIFIED";
            initialScore = 96.0;
        } else {
            initialDetailedStatus = DetailedDocumentStatus.UPLOADED;
            initialVerificationStatus = DocumentVerificationStatus.PENDING;
            initialAiDecision = "AI_PASSED";
            initialScore = 80.0;
        }

        // 7. Check existing document for deduplication / versioning
        Optional<CitizenVaultDocument> existingOpt = vaultDocumentRepository.findByUserIdAndDocumentCode(userId, normalizedCode);
        if (existingOpt.isEmpty() && StringUtils.hasText(canonicalCode)) {
            existingOpt = vaultDocumentRepository.findFirstByUserIdAndCanonicalDocumentCodeIgnoreCase(userId, canonicalCode);
        }

        Instant now = Instant.now();
        CitizenVaultDocument doc;

        if (existingOpt.isPresent()) {
            doc = existingOpt.get();
            // Archive old version
            ApplicationDocumentVersion oldVer = ApplicationDocumentVersion.builder()
                    .version(doc.getVersion() != null ? doc.getVersion() : 1)
                    .fileName(doc.getFileName())
                    .contentType(doc.getContentType())
                    .fileSize(doc.getFileSize())
                    .storageReference(doc.getStorageReference())
                    .gridFsFileId(doc.getGridFsFileId())
                    .status(doc.getDetailedStatus())
                    .verificationStatus(doc.getVerificationStatus())
                    .uploadedAt(doc.getUploadedAt())
                    .verifiedAt(doc.getVerifiedAt())
                    .verifiedBy(doc.getVerifiedBy())
                    .rejectionReason(doc.getRejectionReason())
                    .build();

            if (doc.getVersionHistory() == null) {
                doc.setVersionHistory(new ArrayList<>());
            }
            doc.getVersionHistory().add(oldVer);

            int nextVersion = (doc.getVersion() != null ? doc.getVersion() : 1) + 1;
            doc.setVersion(nextVersion);
            doc.setFileName(originalFilename);
            doc.setContentType(file.getContentType());
            doc.setFileSize(file.getSize());
            doc.setStorageReference(storageRef);
            doc.setGridFsFileId(storageRef);
            doc.setSha256(sha256);
            doc.setUpdatedAt(now);
            doc.setDetailedStatus(initialDetailedStatus);
            doc.setVerificationStatus(initialVerificationStatus);
            doc.setAiVerificationResult(initialAiDecision);
            doc.setVerificationScore(initialScore);
            doc.setExtractedHolderName(extractedHolder);
            doc.setExtractedDob(extractedDob);
            doc.setExtractedDocNumber(extractedDocNumber);
            doc.setExtractionStatus(extractionStatus);
            doc.setIdentityMatchStatus(identityMatchStatus);
            doc.setRejectionReason(initialRejection);
            if (StringUtils.hasText(issuer)) doc.setIssuer(issuer);
            if (StringUtils.hasText(expiryDate)) doc.setExpiryDate(expiryDate);
            doc.setHolderName(finalHolderName);
            doc.setDocNumber(finalDocNumber);
        } else {
            doc = CitizenVaultDocument.builder()
                    .userId(userId)
                    .documentCode(normalizedCode)
                    .canonicalDocumentCode(canonicalCode)
                    .documentName(resolvedName)
                    .documentType(resolvedType)
                    .fileName(originalFilename)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .storageReference(storageRef)
                    .gridFsFileId(storageRef)
                    .version(1)
                    .sha256(sha256)
                    .verificationStatus(initialVerificationStatus)
                    .detailedStatus(initialDetailedStatus)
                    .aiVerificationResult(initialAiDecision)
                    .verificationScore(initialScore)
                    .officerVerified(false)
                    .issuer(StringUtils.hasText(issuer) ? issuer : "Self-Uploaded")
                    .expiryDate(StringUtils.hasText(expiryDate) ? expiryDate : "No Expiration")
                    .holderName(finalHolderName)
                    .docNumber(finalDocNumber)
                    .extractedHolderName(extractedHolder)
                    .extractedDob(extractedDob)
                    .extractedDocNumber(extractedDocNumber)
                    .extractionStatus(extractionStatus)
                    .identityMatchStatus(identityMatchStatus)
                    .rejectionReason(initialRejection)
                    .source(StringUtils.hasText(source) ? source : "Manual Upload")
                    .provider("LOCAL")
                    .uploadedAt(now)
                    .updatedAt(now)
                    .linkedApplications(new ArrayList<>())
                    .versionHistory(new ArrayList<>())
                    .build();
        }

        return vaultDocumentRepository.save(doc);
    }

    public DocumentDownloadDto getVaultDocumentDownload(String documentId, String userId, boolean isPrivileged) {
        CitizenVaultDocument doc = vaultDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Vault document not found with ID: " + documentId));

        if (!isPrivileged && !doc.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: You do not own this document.");
        }

        if (!StringUtils.hasText(doc.getStorageReference())) {
            throw new ResourceNotFoundException("No binary storage reference available for this document.");
        }

        InputStream stream = documentStorageService.retrieve(doc.getStorageReference());
        String contentType = doc.getContentType();
        if (!StringUtils.hasText(contentType) || contentType.equals("application/octet-stream")) {
            String fName = doc.getFileName() != null ? doc.getFileName().toLowerCase() : "";
            if (fName.endsWith(".pdf")) contentType = "application/pdf";
            else if (fName.endsWith(".jpg") || fName.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (fName.endsWith(".png")) contentType = "image/png";
            else contentType = "application/octet-stream";
        }

        return DocumentDownloadDto.builder()
                .fileName(doc.getFileName())
                .contentType(contentType)
                .fileSize(doc.getFileSize())
                .inputStream(stream)
                .build();
    }

    @Transactional
    public void deleteVaultDocument(String documentId, String userId) {
        CitizenVaultDocument doc = vaultDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Vault document not found with ID: " + documentId));

        if (!doc.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: You do not own this document.");
        }

        vaultDocumentRepository.delete(doc);
    }

    /**
     * Synchronize a document uploaded during application creation/review into the citizen's permanent vault.
     */
    @Transactional
    public CitizenVaultDocument syncFromApplicationDocument(ApplicationDocument appDoc, String userId) {
        if (appDoc == null || !appDoc.isUploaded() || !StringUtils.hasText(userId)) {
            return null;
        }

        String docCode = appDoc.getDocumentCode();
        String canonicalCode = deriveCanonicalCode(docCode, appDoc.getDocumentName());

        Optional<CitizenVaultDocument> existingOpt = vaultDocumentRepository.findByUserIdAndDocumentCode(userId, docCode);
        if (existingOpt.isEmpty() && StringUtils.hasText(canonicalCode)) {
            existingOpt = vaultDocumentRepository.findFirstByUserIdAndCanonicalDocumentCodeIgnoreCase(userId, canonicalCode);
        }

        Instant now = Instant.now();
        CitizenVaultDocument vaultDoc;

        if (existingOpt.isPresent()) {
            vaultDoc = existingOpt.get();
            vaultDoc.setFileName(appDoc.getFileName());
            vaultDoc.setContentType(appDoc.getContentType());
            vaultDoc.setFileSize(appDoc.getFileSize());
            vaultDoc.setStorageReference(appDoc.getStorageReference());
            vaultDoc.setGridFsFileId(appDoc.getGridFsFileId());
            vaultDoc.setUpdatedAt(now);
            if (appDoc.getVerificationStatus() != null) {
                vaultDoc.setVerificationStatus(appDoc.getVerificationStatus());
            }
            if (appDoc.getDetailedStatus() != null) {
                vaultDoc.setDetailedStatus(appDoc.getDetailedStatus());
                if (appDoc.getDetailedStatus() == DetailedDocumentStatus.ADMIN_VERIFIED) {
                    vaultDoc.setOfficerVerified(true);
                    vaultDoc.setVerifiedBy(appDoc.getVerifiedBy());
                    vaultDoc.setVerifiedAt(appDoc.getVerifiedAt() != null ? appDoc.getVerifiedAt() : now);
                }
            }
            if (appDoc.getAiVerificationResult() != null) {
                vaultDoc.setAiVerificationResult(appDoc.getAiVerificationResult());
            }
            if (appDoc.getVerificationScore() != null) {
                vaultDoc.setVerificationScore(appDoc.getVerificationScore());
            }
        } else {
            vaultDoc = CitizenVaultDocument.builder()
                    .userId(userId)
                    .documentCode(docCode)
                    .canonicalDocumentCode(canonicalCode)
                    .documentName(StringUtils.hasText(appDoc.getDocumentName()) ? appDoc.getDocumentName() : docCode)
                    .documentType(deriveTypeFromName(appDoc.getDocumentName(), docCode))
                    .fileName(appDoc.getFileName())
                    .contentType(appDoc.getContentType())
                    .fileSize(appDoc.getFileSize())
                    .storageReference(appDoc.getStorageReference())
                    .gridFsFileId(appDoc.getGridFsFileId())
                    .version(appDoc.getVersion() != null ? appDoc.getVersion() : 1)
                    .sha256(appDoc.getSha256())
                    .verificationStatus(appDoc.getVerificationStatus() != null ? appDoc.getVerificationStatus() : DocumentVerificationStatus.PENDING)
                    .detailedStatus(appDoc.getDetailedStatus() != null ? appDoc.getDetailedStatus() : DetailedDocumentStatus.UPLOADED)
                    .aiVerificationResult(appDoc.getAiVerificationResult())
                    .verificationScore(appDoc.getVerificationScore())
                    .officerVerified(appDoc.getDetailedStatus() == DetailedDocumentStatus.ADMIN_VERIFIED)
                    .verifiedBy(appDoc.getVerifiedBy())
                    .verifiedAt(appDoc.getVerifiedAt())
                    .issuer("Self-Uploaded")
                    .expiryDate("No Expiration")
                    .source(appDoc.getSource() != null ? appDoc.getSource() : "Application Upload")
                    .uploadedAt(appDoc.getUploadedAt() != null ? appDoc.getUploadedAt() : now)
                    .updatedAt(now)
                    .linkedApplications(new ArrayList<>())
                    .build();
        }

        if (StringUtils.hasText(appDoc.getApplicationId()) && !vaultDoc.getLinkedApplications().contains(appDoc.getApplicationId())) {
            vaultDoc.getLinkedApplications().add(appDoc.getApplicationId());
        }
        if (StringUtils.hasText(appDoc.getIdentityMatchStatus())) {
            vaultDoc.setIdentityMatchStatus(appDoc.getIdentityMatchStatus());
        }

        return vaultDocumentRepository.save(vaultDoc);
    }

    @Transactional
    public void linkApplicationToVaultDocument(String vaultDocId, String applicationId) {
        if (!StringUtils.hasText(vaultDocId) || !StringUtils.hasText(applicationId)) return;
        vaultDocumentRepository.findById(vaultDocId).ifPresent(doc -> {
            if (doc.getLinkedApplications() == null) doc.setLinkedApplications(new ArrayList<>());
            if (!doc.getLinkedApplications().contains(applicationId)) {
                doc.getLinkedApplications().add(applicationId);
                vaultDocumentRepository.save(doc);
            }
        });
    }

    /**
     * Synchronize officer verification into the citizen's permanent vault.
     */
    @Transactional
    public void syncOfficerVerification(String userId, String documentCode, String reviewerId) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(documentCode)) {
            return;
        }

        String canonical = deriveCanonicalCode(documentCode, null);
        Optional<CitizenVaultDocument> docOpt = vaultDocumentRepository.findByUserIdAndDocumentCode(userId, documentCode);
        if (docOpt.isEmpty() && StringUtils.hasText(canonical)) {
            docOpt = vaultDocumentRepository.findFirstByUserIdAndCanonicalDocumentCodeIgnoreCase(userId, canonical);
        }

        if (docOpt.isPresent()) {
            CitizenVaultDocument doc = docOpt.get();
            doc.setOfficerVerified(true);
            doc.setDetailedStatus(DetailedDocumentStatus.ADMIN_VERIFIED);
            doc.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
            doc.setVerifiedBy(reviewerId);
            doc.setVerifiedAt(Instant.now());
            doc.setUpdatedAt(Instant.now());
            vaultDocumentRepository.save(doc);
            log.info("Synced officer verification to Citizen Vault Document: user={}, code={}, docId={}", userId, documentCode, doc.getId());
        }
    }

    /**
     * Find matching document in citizen vault for cross-scheme auto-reuse.
     */
    public Optional<CitizenVaultDocument> findMatchingVaultDocument(
            String userId,
            String reqDocumentCode,
            String reqDocumentName,
            List<String> alternatives
    ) {
        return findMatchingVaultDocument(userId, reqDocumentCode, deriveCanonicalCode(reqDocumentCode, reqDocumentName), reqDocumentName, alternatives);
    }

    public Optional<CitizenVaultDocument> findMatchingVaultDocument(
            String userId,
            String reqDocumentCode,
            String reqCanonicalCode,
            String reqDocumentName,
            List<String> alternatives
    ) {
        List<CitizenVaultDocument> userDocs = getVaultDocuments(userId);
        if (userDocs.isEmpty()) {
            return Optional.empty();
        }

        List<String> searchCandidates = new ArrayList<>();
        if (StringUtils.hasText(reqCanonicalCode)) {
            searchCandidates.add(reqCanonicalCode.trim().toUpperCase());
        }
        if (StringUtils.hasText(reqDocumentCode)) {
            searchCandidates.add(reqDocumentCode.trim().toUpperCase());
            searchCandidates.add(reqDocumentCode.replace("DOC_", "").trim().toUpperCase());
        }
        if (StringUtils.hasText(reqDocumentName)) {
            searchCandidates.add(reqDocumentName.trim().toUpperCase());
        }
        if (alternatives != null) {
            for (String alt : alternatives) {
                if (StringUtils.hasText(alt)) {
                    searchCandidates.add(alt.trim().toUpperCase());
                }
            }
        }

        for (CitizenVaultDocument vDoc : userDocs) {
            if (!isReusableVaultDocument(vDoc)) {
                continue;
            }

            String vCode = vDoc.getDocumentCode() != null ? vDoc.getDocumentCode().toUpperCase() : "";
            String vCanon = vDoc.getCanonicalDocumentCode() != null ? vDoc.getCanonicalDocumentCode().toUpperCase() : "";
            String vName = vDoc.getDocumentName() != null ? vDoc.getDocumentName().toUpperCase() : "";
            String vType = vDoc.getDocumentType() != null ? vDoc.getDocumentType().toUpperCase() : "";

            for (String cand : searchCandidates) {
                if (vCode.equals(cand) || vCanon.equals(cand) || vName.equals(cand)) {
                    return Optional.of(vDoc);
                }
                if (cand.length() >= 4 && (vName.contains(cand) || cand.contains(vName) || vCode.contains(cand) || cand.contains(vCode) || vCanon.contains(cand) || cand.contains(vCanon))) {
                    return Optional.of(vDoc);
                }
                if (isSemanticMatch(cand, vName, vCode, vCanon, vType)) {
                    return Optional.of(vDoc);
                }
            }
        }

        return Optional.empty();
    }

    private boolean isSemanticMatch(String target, String vName, String vCode, String vCanon, String vType) {
        String t = target.toUpperCase();
        if (t.contains("AADHAAR") || t.contains("AADHAR")) {
            return vName.contains("AADHAAR") || vName.contains("AADHAR") || vCode.contains("AADHAAR") || vCanon.contains("AADHAAR");
        }
        if (t.contains("INCOME")) {
            return vName.contains("INCOME") || vCode.contains("INCOME") || vCanon.contains("INCOME");
        }
        if (t.contains("COMMUNITY") || t.contains("CASTE") || t.contains("SOCIAL_CATEGORY")) {
            return vName.contains("COMMUNITY") || vName.contains("CASTE") || vCode.contains("CASTE") || vCode.contains("COMMUNITY") || vCanon.contains("COMMUNITY") || vCanon.contains("CASTE");
        }
        if (t.contains("BANK") || t.contains("PASSBOOK")) {
            return vName.contains("BANK") || vName.contains("PASSBOOK") || vCode.contains("BANK") || vCanon.contains("BANK");
        }
        if (t.contains("DOMICILE") || t.contains("RESIDENCE") || t.contains("NATIVITY")) {
            return vName.contains("DOMICILE") || vName.contains("RESIDENCE") || vName.contains("NATIVITY") || vCode.contains("DOMICILE") || vCode.contains("RESIDENCE") || vCanon.contains("DOMICILE") || vCanon.contains("RESIDENCE");
        }
        if (t.contains("BONAFIDE") || t.contains("STUDENT") || t.contains("EDUCATION") || t.contains("MARKSHEET")) {
            return vName.contains("BONAFIDE") || vName.contains("STUDENT") || vName.contains("EDUCATION") || vName.contains("MARKSHEET") || vCode.contains("BONAFIDE") || vCode.contains("EDUCATION") || vCanon.contains("EDUCATIONAL") || vCanon.contains("BONAFIDE");
        }
        if (t.contains("DISABILITY") || t.contains("UDID") || t.contains("DIVYANG") || t.contains("HANDICAP")) {
            return vName.contains("DISABILITY") || vName.contains("UDID") || vCode.contains("DISABILITY") || vCanon.contains("DISABILITY");
        }
        if (t.contains("LAND") || t.contains("PATTA") || t.contains("KHASRA") || t.contains("KHATAUNI") || t.contains("7_12") || t.contains("7/12")) {
            return vName.contains("LAND") || vName.contains("PATTA") || vName.contains("KHASRA") || vCode.contains("LAND") || vCanon.contains("LAND");
        }
        if (t.contains("GOVERNMENT") || t.contains("IDENTITY_PROOF") || t.contains("ID_PROOF")) {
            return vName.contains("GOVERNMENT") || vName.contains("IDENTITY") || vCode.contains("GOVERNMENT") || vCanon.contains("GOVERNMENT") || vCanon.contains("IDENTITY");
        }
        return false;
    }

    private String deriveDocumentCode(String name, String type) {
        String base = name != null ? name.toUpperCase() : (type != null ? type.toUpperCase() : "DOC");
        if (base.contains("AADHAAR") || base.contains("AADHAR")) return "DOC_AADHAAR";
        if (base.contains("INCOME")) return "DOC_INCOME";
        if (base.contains("CASTE") || base.contains("COMMUNITY")) return "DOC_CASTE";
        if (base.contains("BANK") || base.contains("PASSBOOK")) return "DOC_BANK_PASSBOOK";
        if (base.contains("DOMICILE") || base.contains("RESIDENCE")) return "DOC_DOMICILE";
        if (base.contains("BONAFIDE")) return "DOC_BONAFIDE";
        if (base.contains("PAN")) return "DOC_PAN";
        return "DOC_" + base.replaceAll("[^A-Z0-9]+", "_");
    }

    public String deriveCanonicalCode(String code, String name) {
        String str = ((code != null ? code : "") + " " + (name != null ? name : "")).toUpperCase();
        if (str.contains("AADHAAR") || str.contains("AADHAR")) return "AADHAAR";
        if (str.contains("INCOME")) return "INCOME_CERTIFICATE";
        if (str.contains("CASTE") || str.contains("COMMUNITY")) return "COMMUNITY_CERTIFICATE";
        if (str.contains("BANK") || str.contains("PASSBOOK")) return "BANK_PASSBOOK";
        if (str.contains("DOMICILE") || str.contains("RESIDENCE") || str.contains("NATIVITY")) return "DOMICILE_CERTIFICATE";
        if (str.contains("BONAFIDE") || str.contains("STUDENT") || str.contains("EDUCATION")) return "EDUCATIONAL_CERTIFICATE";
        if (str.contains("DISABILITY") || str.contains("UDID") || str.contains("DIVYANG")) return "DISABILITY_CERTIFICATE";
        if (str.contains("LAND") || str.contains("PATTA") || str.contains("KHASRA")) return "LAND_RECORDS";
        if (str.contains("GOVERNMENT") || str.contains("GOVT") || str.contains("IDENTITY")) return "GOVERNMENT_DOCUMENT";
        return code != null ? code.replace("DOC_", "") : "DOCUMENT";
    }

    private String deriveTypeFromName(String name, String code) {
        String combined = ((name != null ? name : "") + " " + (code != null ? code : "")).toUpperCase();
        if (combined.contains("AADHAAR") || combined.contains("PAN") || combined.contains("VOTER")) {
            return "Identity Proof";
        }
        if (combined.contains("INCOME") || combined.contains("BANK") || combined.contains("SALARY")) {
            return "Financial Proof";
        }
        if (combined.contains("CASTE") || combined.contains("COMMUNITY")) {
            return "Category Proof";
        }
        if (combined.contains("DOMICILE") || combined.contains("RESIDENCE")) {
            return "Domicile Proof";
        }
        if (combined.contains("STUDENT") || combined.contains("BONAFIDE") || combined.contains("MARKSHEET")) {
            return "Academic Proof";
        }
        return "Identity Proof";
    }

    private static boolean isExpiredDate(String expiryStr) {
        if (!StringUtils.hasText(expiryStr)
                || "No Expiration".equalsIgnoreCase(expiryStr.trim())
                || "Lifetime".equalsIgnoreCase(expiryStr.trim())
                || "Permanent".equalsIgnoreCase(expiryStr.trim())) {
            return false;
        }
        String cleaned = expiryStr.trim();
        try {
            java.time.Instant inst = java.time.Instant.parse(cleaned);
            return inst.isBefore(java.time.Instant.now());
        } catch (Exception ignored) {}

        String datePart = cleaned.contains("T") ? cleaned.substring(0, cleaned.indexOf('T')).trim() : cleaned;

        List<java.time.format.DateTimeFormatter> formatters = List.of(
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH)
        );
        for (java.time.format.DateTimeFormatter fmt : formatters) {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(datePart, fmt);
                return date.isBefore(java.time.LocalDate.now());
            } catch (java.time.format.DateTimeParseException ignored) {}
        }
        return false;
    }

    /**
     * Determine whether a document from Citizen Vault is eligible for cross-scheme or reapplication reuse.
     * Reusable documents must satisfy identity-aware verification rules:
     * - Must NEVER be MISMATCH, UNCERTAIN, PENDING for identity, or FAILED
     * - Must NEVER be REJECTED, AI_REJECTED, AI_FAILED, CORRECTION_REQUIRED, REUPLOAD_REQUIRED, or NOT_UPLOADED
     * - Must NOT be expired
     * - Must have MATCH identity status or be officer-verified, along with an accepted status
     */
    public static boolean isReusableVaultDocument(CitizenVaultDocument vDoc) {
        if (vDoc == null) {
            return false;
        }
        String idStatus = vDoc.getIdentityMatchStatus();
        if ("MISMATCH".equalsIgnoreCase(idStatus)
                || "UNCERTAIN".equalsIgnoreCase(idStatus)
                || "PENDING".equalsIgnoreCase(idStatus)
                || "FAILED".equalsIgnoreCase(idStatus)) {
            return false;
        }

        if (vDoc.getVerificationStatus() == DocumentVerificationStatus.REJECTED) {
            return false;
        }
        if (vDoc.getDetailedStatus() == DetailedDocumentStatus.AI_REJECTED
                || vDoc.getDetailedStatus() == DetailedDocumentStatus.AI_FAILED
                || vDoc.getDetailedStatus() == DetailedDocumentStatus.REJECTED
                || vDoc.getDetailedStatus() == DetailedDocumentStatus.CORRECTION_REQUIRED
                || vDoc.getDetailedStatus() == DetailedDocumentStatus.REUPLOAD_REQUIRED
                || vDoc.getDetailedStatus() == DetailedDocumentStatus.NOT_UPLOADED) {
            return false;
        }

        if (isExpiredDate(vDoc.getExpiryDate())) {
            return false;
        }

        boolean isMatch = "MATCH".equalsIgnoreCase(idStatus) || "VERIFIED".equalsIgnoreCase(idStatus);
        boolean isOfficerVerified = vDoc.isOfficerVerified() 
                || vDoc.getDetailedStatus() == DetailedDocumentStatus.ADMIN_VERIFIED
                || vDoc.getDetailedStatus() == DetailedDocumentStatus.VERIFIED
                || vDoc.getVerificationStatus() == DocumentVerificationStatus.VERIFIED;
        boolean isAiVerified = vDoc.getDetailedStatus() == DetailedDocumentStatus.AI_VERIFIED 
                || vDoc.getDetailedStatus() == DetailedDocumentStatus.AI_PASSED
                || "AI_PASSED".equalsIgnoreCase(vDoc.getAiVerificationResult()) 
                || "AI_VERIFIED".equalsIgnoreCase(vDoc.getAiVerificationResult());

        if (vDoc.getVerificationScore() != null && vDoc.getVerificationScore() < 50.0 && !isOfficerVerified) {
            return false;
        }

        return isOfficerVerified || (isMatch && isAiVerified);
    }
}
