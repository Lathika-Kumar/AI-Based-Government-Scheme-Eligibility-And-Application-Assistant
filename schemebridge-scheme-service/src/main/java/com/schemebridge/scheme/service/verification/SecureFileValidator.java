package com.schemebridge.scheme.service.verification;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Set;

/**
 * Secure file validator enforcing size limits, MIME types, magic byte signatures,
 * script detection, filename hygiene, and SHA-256 hash calculation.
 */
@Component
@Slf4j
public class SecureFileValidator {

    public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    public static final long MIN_FILE_SIZE_BYTES = 100;              // 100 bytes

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("PDF", "JPG", "JPEG", "PNG");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    // Magic bytes
    private static final byte[] MAGIC_PDF = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
    private static final byte[] MAGIC_JPEG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] MAGIC_PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Getter
    @Builder
    public static class FileValidationResult {
        private final boolean valid;
        private final String sha256;
        private final String detectedMime;
        private final String errorMessage;
    }

    public FileValidationResult validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return FileValidationResult.builder()
                    .valid(false)
                    .errorMessage("Uploaded file is empty or missing.")
                    .build();
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return FileValidationResult.builder()
                    .valid(false)
                    .errorMessage("File size (" + (file.getSize() / 1024 / 1024) + "MB) exceeds maximum limit of 5MB.")
                    .build();
        }

        if (file.getSize() < MIN_FILE_SIZE_BYTES) {
            return FileValidationResult.builder()
                    .valid(false)
                    .errorMessage("File is too small (" + file.getSize() + " bytes) or corrupt.")
                    .build();
        }

        // Filename sanitize & extension validation
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        if (originalName.contains("..") || originalName.contains("/") || originalName.contains("\\") || originalName.contains("\0")) {
            return FileValidationResult.builder()
                    .valid(false)
                    .errorMessage("Invalid file name characters detected.")
                    .build();
        }

        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx < 0) {
            return FileValidationResult.builder()
                    .valid(false)
                    .errorMessage("File has no extension. Allowed formats: PDF, JPG, JPEG, PNG.")
                    .build();
        }

        String extension = originalName.substring(dotIdx + 1).toUpperCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return FileValidationResult.builder()
                    .valid(false)
                    .errorMessage("File extension ." + extension + " is not supported. Allowed formats: PDF, JPG, JPEG, PNG.")
                    .build();
        }

        // Content-Type validation
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase().trim() : "";
        if (!contentType.isEmpty() && !ALLOWED_MIME_TYPES.contains(contentType)) {
            return FileValidationResult.builder()
                    .valid(false)
                    .errorMessage("MIME type " + contentType + " is not permitted.")
                    .build();
        }

        // Magic bytes & SHA-256 calculation
        try {
            byte[] bytes = file.getBytes();

            // Check executable or script patterns
            if (bytes.length >= 2 && bytes[0] == 'M' && bytes[1] == 'Z') {
                return FileValidationResult.builder()
                        .valid(false)
                        .errorMessage("Executable files are strictly rejected.")
                        .build();
            }

            // Check embedded malicious scripts in header
            int headerCheckLen = Math.min(bytes.length, 1024);
            String headerAscii = new String(bytes, 0, headerCheckLen, StandardCharsets.ISO_8859_1).toLowerCase();
            if (headerAscii.contains("<script") || headerAscii.contains("<?php") || headerAscii.contains("<html")) {
                return FileValidationResult.builder()
                        .valid(false)
                        .errorMessage("Embedded script or HTML detected in uploaded document.")
                        .build();
            }

            String detectedMime = null;
            if (matchesMagic(bytes, MAGIC_PDF)) {
                detectedMime = "application/pdf";
            } else if (matchesMagic(bytes, MAGIC_JPEG)) {
                detectedMime = "image/jpeg";
            } else if (matchesMagic(bytes, MAGIC_PNG)) {
                detectedMime = "image/png";
            }

            if (detectedMime == null) {
                return FileValidationResult.builder()
                        .valid(false)
                        .errorMessage("File signature (magic bytes) does not match valid PDF, JPEG, or PNG format.")
                        .build();
            }

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(bytes);
            String sha256 = HexFormat.of().formatHex(hashBytes);

            return FileValidationResult.builder()
                    .valid(true)
                    .sha256(sha256)
                    .detectedMime(detectedMime)
                    .build();

        } catch (Exception e) {
            log.error("File validation failed with exception", e);
            return FileValidationResult.builder()
                    .valid(false)
                    .errorMessage("Error validating file content: " + e.getMessage())
                    .build();
        }
    }

    private boolean matchesMagic(byte[] fileBytes, byte[] magic) {
        if (fileBytes.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (fileBytes[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
