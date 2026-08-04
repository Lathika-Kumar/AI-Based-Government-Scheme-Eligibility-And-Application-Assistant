package com.schemebridge.service.impl;

import com.schemebridge.dto.DocumentRequest;
import com.schemebridge.dto.DocumentResponse;
import com.schemebridge.entity.Document;
import com.schemebridge.entity.User;
import com.schemebridge.enums.DocumentStatus;
import com.schemebridge.exception.BadRequestException;
import com.schemebridge.exception.ResourceNotFoundException;
import com.schemebridge.mapper.DocumentMapper;
import com.schemebridge.repository.DocumentRepository;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentMapper documentMapper;

    @Override
    public List<DocumentResponse> getDocuments(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Document> documents = documentRepository.findByUserId(user.getId());
        return documents.stream().map(documentMapper::toDocumentResponse).collect(Collectors.toList());
    }

    @Override
    public DocumentResponse uploadDocument(String userEmail, DocumentRequest request) {
        User user = getUserByEmail(userEmail);
        Document document = documentMapper.toEntity(request, user.getId());
        document.setUploadedAt(Instant.now());
        Document saved = documentRepository.save(document);
        log.info("Document uploaded for user {} with id {}", userEmail, saved.getId());
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
    public DocumentResponse verifyDocument(String id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
        if (!document.getUserId().equals(user.getId())) {
            throw new BadRequestException("Document does not belong to the authenticated user.");
        }
        document.setStatus(DocumentStatus.VERIFIED);
        document.setVerifiedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        Document saved = documentRepository.save(document);
        log.info("Document {} verified for user {}", id, userEmail);
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

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
