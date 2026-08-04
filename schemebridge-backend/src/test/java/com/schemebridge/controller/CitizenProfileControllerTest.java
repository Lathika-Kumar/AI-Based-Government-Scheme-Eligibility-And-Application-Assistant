package com.schemebridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.dto.LoginRequest;
import com.schemebridge.dto.ProfileRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CitizenProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;
    private String testEmail;

    @BeforeEach
    void setUp() throws Exception {
        testEmail = "profile_test_" + System.currentTimeMillis() + "@schemebridge.gov.in";

        // Register
        RegisterRequest registerReq = RegisterRequest.builder()
                .email(testEmail)
                .password("SecurePass123!")
                .fullName("Profile Test User")
                .role(RoleEnum.CITIZEN)
                .build();
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)));

        // Send OTP
        MvcResult otpRes = mockMvc.perform(post("/api/v1/auth/send-email-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(SendOtpRequest.builder().email(testEmail).build())))
                .andReturn();
        String otpJson = otpRes.getResponse().getContentAsString();
        String simulatedOtp = objectMapper.readTree(otpJson).path("data").path("simulatedOtp").asText();

        // Verify OTP
        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .email(testEmail)
                .otp(simulatedOtp)
                .verificationMethod(VerificationMethod.EMAIL)
                .build();
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)));

        // Login & Obtain JWT
        LoginRequest loginReq = LoginRequest.builder().email(testEmail).password("SecurePass123!").build();
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn();
        String loginJson = loginRes.getResponse().getContentAsString();
        jwtToken = objectMapper.readTree(loginJson).path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("GET /api/v1/profile without auth token returns 403 Forbidden")
    void testGetProfileUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/profile with valid token returns user profile")
    void testGetProfileAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/profile")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(testEmail))
                .andExpect(jsonPath("$.data.profileVersion").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/profile updates profile and masks PAN/Aadhaar in response")
    void testUpdateProfileSuccess() throws Exception {
        ProfileRequest updateReq = ProfileRequest.builder()
                .fullName("Profile Test User Updated")
                .dateOfBirth(LocalDate.of(1995, 8, 20))
                .gender("Male")
                .state("Gujarat")
                .district("Gandhinagar")
                .cityOrVillage("Sector 17")
                .pincode("382017")
                .annualIncome(new BigDecimal("250000"))
                .category("OBC")
                .occupation("Farmer")
                .employmentStatus("Self-Employed")
                .qualification("Graduate")
                .aadhaarNumber("999988887777")
                .panNumber("abcde1234f")
                .build();

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Profile Test User Updated"))
                .andExpect(jsonPath("$.data.state").value("Gujarat"))
                .andExpect(jsonPath("$.data.maskedAadhaarNumber").value("XXXX-XXXX-7777"))
                .andExpect(jsonPath("$.data.maskedPanNumber").value("XXXXX1234F"))
                .andExpect(jsonPath("$.data.profileVersion").value(2))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/profile with invalid pincode returns 400 Bad Request")
    void testUpdateProfileInvalidPincode() throws Exception {
        ProfileRequest updateReq = ProfileRequest.builder()
                .pincode("123")
                .build();

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/profile/completion returns completion score and status")
    void testGetProfileCompletion() throws Exception {
        mockMvc.perform(get("/api/v1/profile/completion")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status", notNullValue()))
                .andExpect(jsonPath("$.data.completionPercentage", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/profile/summary returns lightweight dashboard summary")
    void testGetProfileSummary() throws Exception {
        mockMvc.perform(get("/api/v1/profile/summary")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName", notNullValue()))
                .andExpect(jsonPath("$.data.status", notNullValue()));
    }
}
