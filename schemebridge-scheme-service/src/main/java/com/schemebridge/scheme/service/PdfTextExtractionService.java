package com.schemebridge.scheme.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Slf4j
public class PdfTextExtractionService {

    /**
     * Extracts text from an uploaded PDF file using Apache PDFBox.
     *
     * @param file The uploaded MultipartFile (PDF)
     * @return Extracted plain text content, or empty string if no text found.
     */
    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Cannot extract text from empty or null file");
            return "";
        }

        try {
            byte[] bytes = file.getBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (document.isEncrypted()) {
                    log.warn("PDF document is encrypted, extraction may be limited");
                }
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(document);
                log.info("Extracted {} characters of text from PDF: {}", text != null ? text.length() : 0, file.getOriginalFilename());
                return text != null ? text.trim() : "";
            }
        } catch (IOException e) {
            log.error("Failed to extract text from PDF: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to extract text from Government Circular PDF", e);
        }
    }
}
