package com.schemebridge.service;

import com.schemebridge.dto.DocumentRequest;
import com.schemebridge.dto.DocumentResponse;

import java.util.List;

public interface DocumentService {

    List<DocumentResponse> getDocuments(String userEmail);

    DocumentResponse uploadDocument(String userEmail, DocumentRequest request);

    void deleteDocument(String id, String userEmail);

    DocumentResponse verifyDocument(String id, String userEmail);

    DocumentResponse getDocument(String id, String userEmail);
}
