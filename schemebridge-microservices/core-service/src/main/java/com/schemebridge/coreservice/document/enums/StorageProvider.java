package com.schemebridge.coreservice.document.enums;

/**
 * StorageProvider identifies the backend storage implementation.
 * Switching between providers requires only replacing StorageService implementation.
 */
public enum StorageProvider {
    LOCAL,  // Local filesystem (current phase)
    S3,     // AWS S3 (future)
    AZURE,  // Azure Blob Storage (future)
    MINIO   // MinIO (future self-hosted)
}
