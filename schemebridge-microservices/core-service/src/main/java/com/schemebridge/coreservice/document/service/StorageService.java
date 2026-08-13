package com.schemebridge.coreservice.document.service;

import com.schemebridge.coreservice.document.enums.StorageProvider;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface StorageService {

    String storeFile(MultipartFile file, String authUserId, String customFileName);

    Resource loadFileAsResource(String storageLocation);

    void deleteFile(String storageLocation);

    String generateDownloadUrl(String documentId, String storageLocation);

    StorageProvider getStorageProvider();
}
