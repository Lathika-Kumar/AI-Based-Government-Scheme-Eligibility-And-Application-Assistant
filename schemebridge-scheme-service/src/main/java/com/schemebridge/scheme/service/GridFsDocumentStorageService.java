package com.schemebridge.scheme.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class GridFsDocumentStorageService implements DocumentStorageService {

    private final GridFsTemplate gridFsTemplate;
    private final GridFsOperations gridFsOperations;
    private final GridFSBucket gridFSBucket;

    @Override
    public String store(String applicationId, String documentCode, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");
        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException("Invalid filename path traversal detected.");
        }

        String extension = "";
        int lastIndex = originalFilename.lastIndexOf('.');
        if (lastIndex >= 0) {
            extension = originalFilename.substring(lastIndex + 1);
        }

        String safeFileName = "app_" + applicationId + "_" + documentCode + "_" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        Document metadata = new Document();
        metadata.put("applicationId", applicationId);
        metadata.put("documentCode", documentCode);
        metadata.put("originalFilename", originalFilename);
        metadata.put("contentType", file.getContentType());
        metadata.put("fileSize", file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            ObjectId fileId = gridFsTemplate.store(inputStream, safeFileName, file.getContentType(), metadata);
            log.info("Stored document in GridFS: applicationId={}, documentCode={}, fileId={}", applicationId, documentCode, fileId);
            return fileId.toHexString();
        } catch (IOException e) {
            log.error("Failed to store file in GridFS", e);
            throw new RuntimeException("Could not store document in MongoDB GridFS", e);
        }
    }

    @Override
    public String storeVaultDocument(String userId, String documentCode, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "vault_document");
        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException("Invalid filename path traversal detected.");
        }

        String extension = "";
        int lastIndex = originalFilename.lastIndexOf('.');
        if (lastIndex >= 0) {
            extension = originalFilename.substring(lastIndex + 1);
        }

        String safeFileName = "vault_" + userId + "_" + documentCode + "_" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        Document metadata = new Document();
        metadata.put("type", "CITIZEN_VAULT_DOCUMENT");
        metadata.put("userId", userId);
        metadata.put("documentCode", documentCode);
        metadata.put("originalFilename", originalFilename);
        metadata.put("contentType", file.getContentType());
        metadata.put("fileSize", file.getSize());
        metadata.put("uploadedAt", java.time.Instant.now().toString());

        try (InputStream inputStream = file.getInputStream()) {
            ObjectId fileId = gridFsTemplate.store(inputStream, safeFileName, file.getContentType(), metadata);
            log.info("Stored Citizen Vault document in GridFS: userId={}, documentCode={}, fileId={}", userId, documentCode, fileId);
            return fileId.toHexString();
        } catch (IOException e) {
            log.error("Failed to store vault document in GridFS", e);
            throw new RuntimeException("Could not store vault document in MongoDB GridFS", e);
        }
    }

    public String storeCircular(MultipartFile file, String uploadedBy) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty circular file.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "circular.pdf");
        String safeFileName = "circular_" + UUID.randomUUID() + "_" + originalFilename;

        Document metadata = new Document();
        metadata.put("type", "GOVERNMENT_CIRCULAR");
        metadata.put("uploadedBy", uploadedBy);
        metadata.put("originalFilename", originalFilename);
        metadata.put("contentType", file.getContentType());
        metadata.put("fileSize", file.getSize());
        metadata.put("uploadedAt", java.time.Instant.now().toString());

        try (InputStream inputStream = file.getInputStream()) {
            ObjectId fileId = gridFsTemplate.store(inputStream, safeFileName, file.getContentType(), metadata);
            log.info("Stored Government Circular PDF in GridFS: fileId={}, filename={}", fileId, originalFilename);
            return fileId.toHexString();
        } catch (IOException e) {
            log.error("Failed to store circular in GridFS", e);
            throw new RuntimeException("Could not store government circular in MongoDB GridFS", e);
        }
    }

    @Override
    public InputStream retrieve(String storageReference) {
        if (storageReference == null || storageReference.trim().isEmpty()) {
            throw new IllegalArgumentException("Storage reference cannot be empty.");
        }

        try {
            // Check if valid ObjectId for GridFS
            if (ObjectId.isValid(storageReference)) {
                ObjectId objectId = new ObjectId(storageReference);
                GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(objectId)));
                if (gridFSFile != null) {
                    GridFsResource resource = gridFsOperations.getResource(gridFSFile);
                    return resource.getInputStream();
                }
            }

            // Also check by filename in GridFS
            GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("filename").is(storageReference)));
            if (gridFSFile != null) {
                GridFsResource resource = gridFsOperations.getResource(gridFSFile);
                return resource.getInputStream();
            }

            log.warn("Document not found in GridFS for ref: {}", storageReference);
            return new ByteArrayInputStream(new byte[0]);
        } catch (Exception e) {
            log.error("Failed to retrieve file from GridFS for ref: {}", storageReference, e);
            throw new RuntimeException("Could not retrieve document from MongoDB GridFS", e);
        }
    }

    @Override
    public void delete(String storageReference) {
        if (storageReference == null || storageReference.trim().isEmpty()) {
            return;
        }

        try {
            if (ObjectId.isValid(storageReference)) {
                gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(storageReference))));
            } else {
                gridFsTemplate.delete(new Query(Criteria.where("filename").is(storageReference)));
            }
            log.info("Deleted document from GridFS for ref: {}", storageReference);
        } catch (Exception e) {
            log.warn("Failed to delete document from GridFS for ref: {}", storageReference, e);
        }
    }
}
