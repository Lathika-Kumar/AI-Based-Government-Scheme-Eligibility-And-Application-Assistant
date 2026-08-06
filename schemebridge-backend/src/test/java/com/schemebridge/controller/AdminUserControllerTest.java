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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        String adminEmail = "admin_user_test_" + System.currentTimeMillis() + "@schemebridge.gov.in";
        RegisterRequest registerReq = RegisterRequest.builder()
                .email(adminEmail)
                .password("SecurePass123!")
                .fullName("Admin User Controller")
                .role(RoleEnum.SUPER_ADMIN)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)));

        MvcResult otpRes = mockMvc.perform(post("/api/v1/auth/send-email-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(SendOtpRequest.builder().email(adminEmail).build())))
                .andReturn();
        String otpJson = otpRes.getResponse().getContentAsString();
        String otp = objectMapper.readTree(otpJson).path("data").path("simulatedOtp").asText();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(VerifyOtpRequest.builder()
                        .email(adminEmail)
                        .otp(otp)
                        .verificationMethod(VerificationMethod.EMAIL)
                        .build())));

        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequest.builder()
                        .email(adminEmail)
                        .password("SecurePass123!")
                        .build())))
                .andReturn();

        jwtToken = objectMapper.readTree(loginRes.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }

    @Test
    @DisplayName("GET /api/v1/admin/users returns paginated user administration list for trusted officers")
    void testAdminUsersEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
