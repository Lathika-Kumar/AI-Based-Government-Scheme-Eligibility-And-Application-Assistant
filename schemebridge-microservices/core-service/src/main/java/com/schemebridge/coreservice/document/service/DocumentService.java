package com.schemebridge.coreservice.document.service;

import com.schemebridge.coreservice.document.model.DocumentMetadata;
import com.schemebridge.coreservice.document.dto.*;
import com.schemebridge.coreservice.document.enums.DocumentType;
import com.schemebridge.coreservice.document.enums.VerificationStatus;
import com.schemebridge.coreservice.document.repository.DocumentRepository;
import com.schemebridge.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final ChecksumService checksumService;
    private final DocumentHistoryService historyService;
    private final DocumentVerificationService verificationService;

    @Value("${document.storage.allowed-mime-types:image/jpeg,image/png,image/gif,application/pdf}")
    private List<String> allowedMimeTypes;

    @Value("${document.storage.allowed-extensions:jpg,jpeg,png,pdf,doc,docx}")
    private List<String> allowedExtensions;

    @Value("${document.storage.blocked-extensions:exe,bat,sh,php,js,jar}")
    private List<String> blockedExtensions;

    @Value("${document.storage.max-file-size-mb:10}")
    private long maxFileSizeMb;

    // ────────────────────────────────────────────────────────────────────────────
    // UPLOAD DOCUMENT
    // ────────────────────────────────────────────────────────────────────────────

    public DocumentResponse uploadDocument(MultipartFile file, String authUserId,
                                           String applicationId, DocumentType documentType,
                                           String documentName) {
        validateFile(file);

        String checksum = checksumService.calculateSHA256(file);
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = getFileExtension(originalFilename);
        String mimeType = file.getContentType();

        // 1. Check for duplicate upload (same authUserId + documentType + checksum)
        Optional<DocumentMetadata> existingWithSameHash = documentRepository
                .findByAuthUserIdAndDocumentTypeAndChecksumSHA256AndActiveTrue(authUserId, documentType, checksum);
        if (existingWithSameHash.isPresent()) {
            throw new IllegalStateException(
                    "Duplicate document detected! An identical file has already been uploaded for document type "
                            + documentType);
        }

        // 2. Check if a document of this type already exists for user -> if so, version it!
        Optional<DocumentMetadata> existingDocOpt = documentRepository
                .findByAuthUserIdAndDocumentTypeAndActiveTrue(authUserId, documentType);

        String storedFileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + "." + extension;
        String storageLocation = storageService.storeFile(file, authUserId, storedFileName);

        DocumentMetadata metadata;

        if (existingDocOpt.isPresent()) {
            // Update existing document metadata with versioning
            metadata = existingDocOpt.get();

            // Archive current state into versionHistory
            historyService.addVersion(metadata);

            // Set new current state
            metadata.setOriginalFileName(originalFilename);
            metadata.setStoredFileName(storedFileName);
            metadata.setMimeType(mimeType);
            metadata.setFileExtension(extension);
            metadata.setFileSize(file.getSize());
            metadata.setStorageLocation(storageLocation);
            metadata.setChecksumSHA256(checksum);
            metadata.setVerificationStatus(VerificationStatus.UPLOADED);
            metadata.setUpdatedAt(LocalDateTime.now());
            if (applicationId != null) metadata.setApplicationId(applicationId);
            if (documentName != null) metadata.setDocumentName(documentName);

            historyService.addAuditEntry(metadata, "REUPLOAD", authUserId, authUserId, "127.0.0.1",
                    "Uploaded new version v" + metadata.getCurrentVersion());
        } else {
            // Create new DocumentMetadata entry
            metadata = DocumentMetadata.builder()
                    .authUserId(authUserId)
                    .applicationId(applicationId)
                    .documentType(documentType)
                    .documentName(documentName != null ? documentName : documentType.name())
                    .originalFileName(originalFilename)
                    .storedFileName(storedFileName)
                    .mimeType(mimeType)
                    .fileExtension(extension)
                    .fileSize(file.getSize())
                    .storageLocation(storageLocation)
                    .storageProvider(storageService.getStorageProvider())
                    .verificationStatus(VerificationStatus.UPLOADED)
                    .checksumSHA256(checksum)
                    .currentVersion(1)
                    .uploadedAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .uploadedBy(authUserId)
                    .active(true)
                    .build();

            historyService.addAuditEntry(metadata, "UPLOAD", authUserId, authUserId, "127.0.0.1",
                    "Initial document upload v1");
        }

        DocumentMetadata saved = documentRepository.save(metadata);
        return mapToResponse(saved);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // GET DOCUMENT & DOWNLOAD
    // ────────────────────────────────────────────────────────────────────────────

    public DocumentResponse getDocumentById(String id) {
        DocumentMetadata metadata = documentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
        return mapToResponse(metadata);
    }

    public Resource downloadDocumentResource(String id) {
        DocumentMetadata metadata = documentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
        return storageService.loadFileAsResource(metadata.getStorageLocation());
    }

    public List<DocumentResponse> getDocumentsByApplicationId(String applicationId) {
        return documentRepository.findByApplicationIdAndActiveTrue(applicationId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<DocumentResponse> getMyDocuments(String authUserId) {
        return documentRepository.findByAuthUserIdAndActiveTrue(authUserId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────────────────
    // VERIFICATION WORKFLOW
    // ────────────────────────────────────────────────────────────────────────────

    public DocumentResponse verifyDocument(String id, DocumentVerifyRequest request) {
        DocumentMetadata verified = verificationService.verifyDocument(id, request);
        return mapToResponse(verified);
    }

    public DocumentResponse rejectDocument(String id, DocumentVerifyRequest request) {
        DocumentMetadata rejected = verificationService.rejectDocument(id, request);
        return mapToResponse(rejected);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // SOFT DELETE
    // ────────────────────────────────────────────────────────────────────────────

    public void deleteDocument(String id, String authUserId) {
        DocumentMetadata metadata = documentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));

        metadata.setActive(false);
        metadata.setUpdatedAt(LocalDateTime.now());

        historyService.addAuditEntry(metadata, "DELETE", authUserId, authUserId, "127.0.0.1", "Soft deleted");
        documentRepository.save(metadata);

        // Delete physical file
        storageService.deleteFile(metadata.getStorageLocation());
    }

    // ────────────────────────────────────────────────────────────────────────────
    // DOCUMENT READINESS (For Citizen Service Integration)
    // ────────────────────────────────────────────────────────────────────────────

    public DocumentReadinessResponse calculateDocumentReadiness(String authUserId) {
        List<DocumentMetadata> userDocs = documentRepository.findByAuthUserIdAndActiveTrue(authUserId);

        List<DocumentType> uploadedTypes = userDocs.stream().map(DocumentMetadata::getDocumentType).distinct().toList();
        List<DocumentType> verifiedTypes = userDocs.stream()
                .filter(d -> d.getVerificationStatus() == VerificationStatus.VERIFIED)
                .map(DocumentMetadata::getDocumentType).distinct().toList();

        List<DocumentType> essentialTypes = List.of(
                DocumentType.AADHAAR, DocumentType.PAN, DocumentType.INCOME_CERTIFICATE, DocumentType.BANK_PASSBOOK
        );

        List<DocumentType> missingEssential = essentialTypes.stream()
                .filter(t -> !uploadedTypes.contains(t))
                .toList();

        double percentage = (double) verifiedTypes.size() / essentialTypes.size() * 100.0;
        percentage = Math.min(100.0, percentage);

        return DocumentReadinessResponse.builder()
                .authUserId(authUserId)
                .isReadyForApplication(missingEssential.isEmpty())
                .readinessPercentage(percentage)
                .totalUploaded(uploadedTypes.size())
                .totalVerified(verifiedTypes.size())
                .uploadedTypes(uploadedTypes)
                .verifiedTypes(verifiedTypes)
                .missingEssentialTypes(missingEssential)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // FILE VALIDATION HELPERS
    // ────────────────────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file");
        }
        if (file.getSize() > maxFileSizeMb * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of " + maxFileSizeMb + "MB");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = getFileExtension(originalFilename).toLowerCase();

        if (blockedExtensions.contains(extension)) {
            throw new IllegalArgumentException("File extension ." + extension + " is prohibited for security reasons");
        }

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file extension ." + extension + ". Allowed: " + allowedExtensions);
        }

        String mimeType = file.getContentType();
        if (mimeType != null && !allowedMimeTypes.contains(mimeType)) {
            log.warn("MIME type {} not in strictly allowed list, but extension {} is allowed", mimeType, extension);
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    public DocumentResponse mapToResponse(DocumentMetadata metadata) {
        return DocumentResponse.builder()
                .id(metadata.getId())
                .authUserId(metadata.getAuthUserId())
                .applicationId(metadata.getApplicationId())
                .documentType(metadata.getDocumentType())
                .documentName(metadata.getDocumentName())
                .originalFileName(metadata.getOriginalFileName())
                .storedFileName(metadata.getStoredFileName())
                .mimeType(metadata.getMimeType())
                .fileExtension(metadata.getFileExtension())
                .fileSize(metadata.getFileSize())
                .storageProvider(metadata.getStorageProvider())
                .verificationStatus(metadata.getVerificationStatus())
                .verifiedBy(metadata.getVerifiedBy())
                .verifiedByName(metadata.getVerifiedByName())
                .verifiedAt(metadata.getVerifiedAt())
                .remarks(metadata.getRemarks())
                .rejectionReason(metadata.getRejectionReason())
                .checksumSHA256(metadata.getChecksumSHA256())
                .currentVersion(metadata.getCurrentVersion())
                .downloadUrl(storageService.generateDownloadUrl(metadata.getId(), metadata.getStorageLocation()))
                .versionHistory(metadata.getVersionHistory())
                .auditTrail(metadata.getAuditTrail())
                .uploadedAt(metadata.getUploadedAt())
                .updatedAt(metadata.getUpdatedAt())
                .build();
    }
}
