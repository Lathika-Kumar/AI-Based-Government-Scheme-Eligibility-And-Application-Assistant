package com.schemebridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.dto.LoginRequest;
import com.schemebridge.dto.RegisterRequest;
import com.schemebridge.dto.SendOtpRequest;
import com.schemebridge.dto.VerifyOtpRequest;
import com.schemebridge.enums.RoleEnum;
import com.schemebridge.enums.VerificationMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;
    private String testEmail;
    private String uploadedDocumentId;

    @BeforeEach
    void setUp() throws Exception {
        testEmail = "document_test_" + System.currentTimeMillis() + "@schemebridge.gov.in";

        RegisterRequest registerReq = RegisterRequest.builder()
                .email(testEmail)
                .password("SecurePass123!")
                .fullName("Document Test User")
                .role(RoleEnum.CITIZEN)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)));

        MvcResult otpRes = mockMvc.perform(post("/api/v1/auth/send-email-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(SendOtpRequest.builder().email(testEmail).build())))
                .andReturn();
        String otpJson = otpRes.getResponse().getContentAsString();
        String simulatedOtp = objectMapper.readTree(otpJson).path("data").path("simulatedOtp").asText();

        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .email(testEmail)
                .otp(simulatedOtp)
                .verificationMethod(VerificationMethod.EMAIL)
                .build();
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)));

        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequest.builder().email(testEmail).password("SecurePass123!").build())))
                .andReturn();
        String loginJson = loginRes.getResponse().getContentAsString();
        jwtToken = objectMapper.readTree(loginJson).path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Upload, list, and vault score endpoints work for authenticated citizen")
    void testUploadAndListDocumentsAndVaultScore() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "aadhaar.pdf",
                "application/pdf",
                "test-aadhaar-content".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("documentType", "AADHAAR")
                        .param("documentName", "Aadhaar Card")
                        .param("remarks", "Primary identity proof")
                        .param("expiryDate", "2030-12-31")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        uploadedDocumentId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText();

        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/documents/vault-score")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.completionPercentage", greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("Delete endpoint removes document ownership safely for authenticated citizen")
    void testDeleteDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pan.pdf",
                "application/pdf",
                "test-pan-content".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("documentType", "PAN")
                        .param("documentName", "PAN Card")
                        .param("remarks", "Identity and tax proof")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andReturn();

        String documentId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText();

        mockMvc.perform(delete("/api/v1/documents/{id}", documentId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
