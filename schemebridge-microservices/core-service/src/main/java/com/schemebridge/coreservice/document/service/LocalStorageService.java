package com.schemebridge.coreservice.document.service;

import com.schemebridge.coreservice.document.enums.StorageProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;

@Service
@Slf4j
public class LocalStorageService implements StorageService {

    private final Path fileStorageLocation;

    public LocalStorageService(@Value("${document.storage.local.base-path:d:/schemeBridge/uploads/documents}") String basePath) {
        this.fileStorageLocation = Paths.get(basePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("Initialized LocalStorageService at {}", this.fileStorageLocation);
        } catch (Exception ex) {
            log.error("Could not create storage directory", ex);
            throw new RuntimeException("Could not create storage directory", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String authUserId, String customFileName) {
        String fileName = StringUtils.cleanPath(customFileName != null ? customFileName : file.getOriginalFilename());
        try {
            if (fileName.contains("..")) {
                throw new IllegalArgumentException("Invalid path sequence in file name " + fileName);
            }
            Path userDirectory = this.fileStorageLocation.resolve(authUserId);
            Files.createDirectories(userDirectory);

            Path targetLocation = userDirectory.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return targetLocation.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName, ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String storageLocation) {
        try {
            Path filePath = Paths.get(storageLocation).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + storageLocation);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File path invalid: " + storageLocation, ex);
        }
    }

    @Override
    public void deleteFile(String storageLocation) {
        try {
            Path filePath = Paths.get(storageLocation).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            log.warn("Could not delete file at {}", storageLocation, ex);
        }
    }

    @Override
    public String generateDownloadUrl(String documentId, String storageLocation) {
        return "/api/v1/documents/download/" + documentId;
    }

    @Override
    public StorageProvider getStorageProvider() {
        return StorageProvider.LOCAL;
    }
}
