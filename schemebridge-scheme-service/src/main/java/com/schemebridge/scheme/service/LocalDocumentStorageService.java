package com.schemebridge.scheme.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
public class LocalDocumentStorageService implements DocumentStorageService {

    private final Path rootLocation;

    public LocalDocumentStorageService(@Value("${scheme.storage.upload-dir:storage}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize local upload storage folder", e);
        }
    }

    @Override
    public String store(String applicationId, String documentCode, MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException("Cannot store file with relative path outside current directory " + originalFilename);
        }

        String extension = "";
        int lastIndex = originalFilename.lastIndexOf('.');
        if (lastIndex >= 0) {
            extension = originalFilename.substring(lastIndex + 1);
        }

        String safeFileName = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);

        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Failed to store empty file " + originalFilename);
            }

            Path targetDir = this.rootLocation.resolve("applications/" + applicationId + "/" + documentCode).normalize();
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(safeFileName).normalize();
            if (!targetPath.toAbsolutePath().startsWith(this.rootLocation)) {
                throw new SecurityException("Path traversal attempt detected!");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return "applications/" + applicationId + "/" + documentCode + "/" + safeFileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + originalFilename, e);
        }
    }

    @Override
    public InputStream retrieve(String storageReference) {
        try {
            Path targetPath = this.rootLocation.resolve(storageReference).normalize();
            if (!targetPath.toAbsolutePath().startsWith(this.rootLocation)) {
                throw new SecurityException("Path traversal attempt detected!");
            }
            if (!Files.exists(targetPath)) {
                throw new RuntimeException("File not found for reference: " + storageReference);
            }
            return Files.newInputStream(targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file reference: " + storageReference, e);
        }
    }

    @Override
    public void delete(String storageReference) {
        try {
            Path targetPath = this.rootLocation.resolve(storageReference).normalize();
            if (!targetPath.toAbsolutePath().startsWith(this.rootLocation)) {
                throw new SecurityException("Path traversal attempt detected!");
            }
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + storageReference, e);
        }
    }
}
