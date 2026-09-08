package com.schemebridge.scheme.ocr;

import com.schemebridge.scheme.document.DocumentOcrResult;

import java.io.InputStream;

/**
 * Service Provider Interface (SPI) for Document OCR & Intelligence.
 * Enables zero-downtime swapping of OCR backends (PDFBox, Tesseract, Cloud Vision).
 */
public interface DocumentOcrProvider {
    String getProviderName();
    String getProviderVersion();
    boolean isAvailable();
    DocumentOcrResult process(InputStream stream, String documentCode, String fileName, String contentType);
}
