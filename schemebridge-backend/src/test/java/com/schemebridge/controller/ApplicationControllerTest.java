package com.schemebridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.dto.ApplicationRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;
    private String testEmail;
    private String applicationId;

    @BeforeEach
    void setUp() throws Exception {
        testEmail = "application_test_" + System.currentTimeMillis() + "@schemebridge.gov.in";

        RegisterRequest registerReq = RegisterRequest.builder()
                .email(testEmail)
                .password("SecurePass123!")
                .fullName("Application Test User")
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

        ApplicationRequest request = ApplicationRequest.builder()
                .schemeId("scheme-001")
                .schemeName("Test Scheme")
                .ministry("Ministry of Social Justice")
                .documents(java.util.List.of("doc-1"))
                .build();

        MvcResult applicationRes = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        applicationId = objectMapper.readTree(applicationRes.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText();
    }

    @Test
    @DisplayName("GET /api/v1/applications/{id}/timeline returns timeline history for the authenticated user")
    void testGetApplicationTimeline() throws Exception {
        mockMvc.perform(get("/api/v1/applications/{id}/timeline", applicationId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].stage").value("SUBMITTED"));
    }
}
