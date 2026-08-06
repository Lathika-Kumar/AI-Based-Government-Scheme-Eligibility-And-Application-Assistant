package com.schemebridge.service;

import com.schemebridge.dto.DocumentMetadataResponse;
import com.schemebridge.dto.DocumentResponse;
import com.schemebridge.dto.DocumentUploadRequest;
import com.schemebridge.dto.DocumentVerificationRequest;
import com.schemebridge.dto.VaultScoreResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    List<DocumentResponse> getDocuments(String userEmail);

    DocumentResponse uploadDocument(String userEmail, MultipartFile file, DocumentUploadRequest request);

    void deleteDocument(String id, String userEmail);

    DocumentResponse verifyDocument(String id, String userEmail, DocumentVerificationRequest request);

    DocumentResponse getDocument(String id, String userEmail);

    VaultScoreResponse getVaultScore(String userEmail);

    DocumentMetadataResponse syncDigilocker(String userEmail, DocumentMetadataResponse request);
}
