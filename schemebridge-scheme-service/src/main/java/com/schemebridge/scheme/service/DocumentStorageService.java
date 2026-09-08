package com.schemebridge.scheme.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface DocumentStorageService {
    String store(String applicationId, String documentCode, MultipartFile file);
    InputStream retrieve(String storageReference);
    void delete(String storageReference);
}
