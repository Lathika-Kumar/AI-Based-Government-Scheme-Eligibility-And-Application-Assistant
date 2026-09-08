package com.schemebridge.scheme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.scheme.config.SecurityConfig;
import com.schemebridge.scheme.dto.request.CreateApplicationRequest;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.ApplicationResponse;
import com.schemebridge.scheme.security.JwtTokenProvider;
import com.schemebridge.scheme.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
public class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicationService applicationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    public void testGetMyApplications_Unauthenticated_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/applications/my")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetMyApplications_AuthenticatedUser_ReturnsOk() throws Exception {
        // Arrange
        String token = "valid-token";
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn("8");
        when(jwtTokenProvider.getRolesFromToken(token)).thenReturn(List.of("USER"));

        when(applicationService.getMyApplications("8")).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/applications/my")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testCreateApplication_AuthenticatedUser_ReturnsCreated() throws Exception {
        // Arrange
        String token = "valid-token";
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn("8");
        when(jwtTokenProvider.getRolesFromToken(token)).thenReturn(List.of("USER"));

        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .schemeCode("SCH-TEST-001")
                .profile(CitizenEligibilityProfile.builder().age(21).build())
                .build();

        ApplicationResponse response = ApplicationResponse.builder()
                .id("app-123")
                .applicationNumber("SB-APP-2026-000023")
                .userId("8")
                .schemeCode("SCH-TEST-001")
                .status("DOCUMENTS_PENDING")
                .build();

        when(applicationService.createApplication(any(CreateApplicationRequest.class), eq("8"))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    public void testGetMyApplications_InvalidJwt_ReturnsForbidden() throws Exception {
        // Arrange
        String token = "invalid-token";
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/applications/my")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testAdminEndpoints_UserRole_ReturnsForbidden() throws Exception {
        // Arrange
        String token = "user-token";
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn("8");
        // Only USER role, not ADMIN/SCHEME_MANAGER
        when(jwtTokenProvider.getRolesFromToken(token)).thenReturn(List.of("USER"));

        // Act & Assert (POST /api/schemes requires SCHEME_MANAGER or ADMIN)
        mockMvc.perform(post("/api/schemes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testJwtRoleMapping_RolesParsedSuccessfully() throws Exception {
        // Arrange
        String token = "admin-token";
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn("11");
        // ADMIN role
        when(jwtTokenProvider.getRolesFromToken(token)).thenReturn(List.of("ADMIN"));

        when(applicationService.getMyApplications("11")).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/applications/my")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
