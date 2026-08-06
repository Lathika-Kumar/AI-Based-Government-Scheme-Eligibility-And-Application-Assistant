package com.schemebridge.service.impl;

import com.schemebridge.dto.DocumentMetadataResponse;
import com.schemebridge.dto.DocumentResponse;
import com.schemebridge.dto.DocumentSummaryResponse;
import com.schemebridge.dto.DocumentUploadRequest;
import com.schemebridge.dto.DocumentVerificationRequest;
import com.schemebridge.dto.VaultScoreResponse;
import com.schemebridge.entity.Document;
import com.schemebridge.entity.User;
import com.schemebridge.enums.DocumentStatus;
import com.schemebridge.enums.DocumentType;
import com.schemebridge.enums.RoleEnum;
import com.schemebridge.exception.BadRequestException;
import com.schemebridge.exception.ResourceNotFoundException;
import com.schemebridge.mapper.DocumentMapper;
import com.schemebridge.repository.DocumentRepository;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private static final List<String> REQUIRED_DOCUMENT_TYPES = List.of(
            "AADHAAR",
            "PAN",
            "RESIDENCE_PROOF",
            "BANK_PASSBOOK"
    );

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentMapper documentMapper;

    @Override
    public List<DocumentResponse> getDocuments(String userEmail) {
        User user = getUserByEmail(userEmail);
        return documentRepository.findByUserId(user.getId())
                .stream()
                .map(documentMapper::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentResponse uploadDocument(String userEmail, MultipartFile file, DocumentUploadRequest request) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A document file is required for vault upload.");
        }

        User user = getUserByEmail(userEmail);
        String saveName = String.format("%s_%s_%s", user.getId(), Instant.now().toEpochMilli(), file.getOriginalFilename());
        Path uploadPath = saveFile(file, saveName);

        Document document = Document.builder()
                .userId(user.getId())
                .name(request.getDocumentName())
                .type(request.getDocumentType())
                .issuer(request.getIssuer())
                .expiryDate(request.getExpiryDate())
                .source(request.getSource())
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .linkedSchemes(request.getLinkedSchemes() != null ? request.getLinkedSchemes() : new ArrayList<>())
                .status(DocumentStatus.UPLOADED)
                .uploadedAt(Instant.now())
                .build();

        Document saved = documentRepository.save(document);
        log.info("Document uploaded for user {} with id {} saved at {}", userEmail, saved.getId(), uploadPath);
        return documentMapper.toDocumentResponse(saved);
    }

    @Override
    public void deleteDocument(String id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
        if (!document.getUserId().equals(user.getId())) {
            throw new BadRequestException("Document does not belong to the authenticated user.");
        }
        document.setStatus(DocumentStatus.REJECTED);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);
        log.info("Document {} marked deleted/rejected by user {}", id, userEmail);
    }

    @Override
    public DocumentResponse verifyDocument(String id, String userEmail, DocumentVerificationRequest request) {
        User user = getUserByEmail(userEmail);
        if (!user.getRoles().contains(RoleEnum.SUPER_ADMIN) && !user.getRoles().contains(RoleEnum.VERIFICATION_OFFICER)) {
            throw new BadRequestException("Only verification officers can verify documents.");
        }

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
        if (!document.getUserId().equals(user.getId())) {
            throw new BadRequestException("Document does not belong to the authenticated user.");
        }

        document.setStatus(document.getStatus() == DocumentStatus.REJECTED ? DocumentStatus.REJECTED : DocumentStatus.VERIFIED);
        document.setVerifiedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        if (request != null && request.getRemarks() != null && !request.getRemarks().isBlank()) {
            document.setRejectionReason(request.getRemarks());
        }
        Document saved = documentRepository.save(document);
        log.info("Document {} verified by officer {}", id, userEmail);
        return documentMapper.toDocumentResponse(saved);
    }

    @Override
    public DocumentResponse getDocument(String id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
        if (!document.getUserId().equals(user.getId())) {
            throw new BadRequestException("Document does not belong to the authenticated user.");
        }
        return documentMapper.toDocumentResponse(document);
    }

    @Override
    public VaultScoreResponse getVaultScore(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Document> documents = documentRepository.findByUserId(user.getId());

        List<String> requiredDocTypes = REQUIRED_DOCUMENT_TYPES;
        int verifiedCount = 0;
        int pendingCount = 0;
        int uploadedCount = 0;

        for (Document document : documents) {
            if (document.getStatus() == DocumentStatus.VERIFIED) {
                verifiedCount++;
            } else if (document.getStatus() == DocumentStatus.UPLOADED) {
                uploadedCount++;
            } else if (document.getStatus() == DocumentStatus.PENDING_REVIEW) {
                pendingCount++;
            }
        }

        List<DocumentSummaryResponse> summaryResponses = documents.stream()
                .map(document -> DocumentSummaryResponse.builder()
                        .id(document.getId())
                        .documentType(parseDocumentType(document.getType()))
                        .documentName(document.getName())
                        .status(document.getStatus())
                        .uploadedAt(document.getUploadedAt())
                        .verifiedAt(document.getVerifiedAt())
                        .build())
                .collect(Collectors.toList());

        int reported = Math.min(requiredDocTypes.size(), Math.max(uploadedCount + verifiedCount, 0));
        int completionPercentage = Math.round((reported * 100f) / requiredDocTypes.size());
        return VaultScoreResponse.builder()
                .completionPercentage(completionPercentage)
                .uploadedCount(uploadedCount)
                .missingCount(Math.max(requiredDocTypes.size() - (verifiedCount + uploadedCount), 0))
                .verifiedCount(verifiedCount)
                .pendingCount(pendingCount)
                .requiredDocumentTypes(requiredDocTypes)
                .documents(summaryResponses)
                .build();
    }

    @Override
    public DocumentMetadataResponse syncDigilocker(String userEmail, DocumentMetadataResponse request) {
        User user = getUserByEmail(userEmail);
        if (request == null) {
            throw new BadRequestException("DigiLocker metadata payload is required.");
        }
        return DocumentMetadataResponse.builder()
                .documentId(request.getDocumentId())
                .provider("DigiLocker")
                .remoteDocumentId(request.getRemoteDocumentId() != null ? request.getRemoteDocumentId() : "mock-digilocker-" + System.currentTimeMillis())
                .source(request.getSource() != null ? request.getSource() : "DigiLocker")
                .syncedAt(Instant.now())
                .status("SYNCED")
                .build();
    }

    private Path saveFile(MultipartFile file, String saveName) {
        try {
            Path uploadPath = Paths.get("uploads");
            Files.createDirectories(uploadPath);
            Path destination = uploadPath.resolve(saveName);
            file.transferTo(destination);
            return destination;
        } catch (IOException e) {
            throw new BadRequestException("Could not store uploaded document file: " + e.getMessage());
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private DocumentType parseDocumentType(String type) {
        try {
            return DocumentType.valueOf(type.toUpperCase());
        } catch (Exception ex) {
            return null;
        }
    }
}
