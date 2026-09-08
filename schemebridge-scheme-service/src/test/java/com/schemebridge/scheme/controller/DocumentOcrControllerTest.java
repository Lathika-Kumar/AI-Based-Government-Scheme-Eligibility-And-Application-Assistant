package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.document.ExtractedFieldDetail;
import com.schemebridge.scheme.document.OcrExtractionStatus;
import com.schemebridge.scheme.dto.response.DocumentOcrResponse;
import com.schemebridge.scheme.service.DocumentOcrService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentOcrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentOcrService documentOcrService;

    @Test
    @WithMockUser(username = "65", roles = {"CITIZEN"})
    @DisplayName("POST /api/applications/{id}/documents/{code}/ocr - Successfully processes OCR for authenticated owner")
    void testProcessOcr_Success() throws Exception {
        DocumentOcrResponse mockResponse = DocumentOcrResponse.builder()
                .id("ocr-001")
                .applicationId("app-123")
                .documentCode("INCOME_CERT")
                .userId("65")
                .version(1)
                .detectedDocumentType("Income Certificate")
                .overallConfidence(0.94)
                .extractionStatus(OcrExtractionStatus.SUCCESS)
                .extractedFields(Map.of("annualIncome", ExtractedFieldDetail.builder()
                        .fieldName("annualIncome")
                        .value(160000.0)
                        .confidence(0.94)
                        .source("OCR_EXTRACTED")
                        .verified(false)
                        .build()))
                .processedAt(Instant.now())
                .processingDurationMs(35L)
                .build();

        when(documentOcrService.processOcr(eq("app-123"), eq("INCOME_CERT"), eq("65"), anyBoolean()))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/applications/app-123/documents/INCOME_CERT/ocr")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ocr-001"))
                .andExpect(jsonPath("$.applicationId").value("app-123"))
                .andExpect(jsonPath("$.documentCode").value("INCOME_CERT"))
                .andExpect(jsonPath("$.detectedDocumentType").value("Income Certificate"))
                .andExpect(jsonPath("$.extractionStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.extractedFields.annualIncome.value").value(160000.0))
                .andExpect(jsonPath("$.extractedFields.annualIncome.source").value("OCR_EXTRACTED"))
                .andExpect(jsonPath("$.extractedFields.annualIncome.verified").value(false));
    }

    @Test
    @WithMockUser(username = "65", roles = {"CITIZEN"})
    @DisplayName("GET /api/applications/{id}/documents/{code}/ocr - Returns cached OCR extraction")
    void testGetOcrResult_Success() throws Exception {
        DocumentOcrResponse mockResponse = DocumentOcrResponse.builder()
                .id("ocr-001")
                .applicationId("app-123")
                .documentCode("INCOME_CERT")
                .userId("65")
                .version(1)
                .detectedDocumentType("Income Certificate")
                .overallConfidence(0.94)
                .extractionStatus(OcrExtractionStatus.SUCCESS)
                .processedAt(Instant.now())
                .build();

        when(documentOcrService.getOcrResult(eq("app-123"), eq("INCOME_CERT"), eq("65"), anyBoolean()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/applications/app-123/documents/INCOME_CERT/ocr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ocr-001"))
                .andExpect(jsonPath("$.documentCode").value("INCOME_CERT"));
    }
}
