package com.schemebridge.scheme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.scheme.config.SecurityConfig;
import com.schemebridge.scheme.dto.request.ReviewActionRequest;
import com.schemebridge.scheme.dto.request.ReviewDecisionRequest;
import com.schemebridge.scheme.dto.response.ApplicationResponse;
import com.schemebridge.scheme.security.JwtTokenProvider;
import com.schemebridge.scheme.service.ApplicationReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationReviewController.class)
@Import(SecurityConfig.class)
public class ApplicationReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicationReviewService applicationReviewService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final String ADMIN_TOKEN = "valid-admin-token";
    private static final String CITIZEN_TOKEN = "valid-citizen-token";
    private static final String APP_ID = "6a9fdf570fd527585a2305c9";

    private void setupAdminAuth() {
        when(jwtTokenProvider.validateToken(ADMIN_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(ADMIN_TOKEN)).thenReturn("52");
        when(jwtTokenProvider.getRolesFromToken(ADMIN_TOKEN)).thenReturn(List.of("ADMIN"));
    }

    private void setupCitizenAuth() {
        when(jwtTokenProvider.validateToken(CITIZEN_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(CITIZEN_TOKEN)).thenReturn("53");
        when(jwtTokenProvider.getRolesFromToken(CITIZEN_TOKEN)).thenReturn(List.of("CITIZEN"));
    }

    @Test
    @DisplayName("1. Canonical Review: APPROVE action succeeds and returns updated application")
    public void testReviewApplication_ApproveAction_ReturnsOk() throws Exception {
        setupAdminAuth();

        ReviewActionRequest request = ReviewActionRequest.builder()
                .action("APPROVE")
                .remarks("All eligibility conditions and documents verified.")
                .build();

        ApplicationResponse response = ApplicationResponse.builder()
                .id(APP_ID)
                .applicationNumber("SB-APP-2026-000555")
                .status("APPROVED")
                .build();

        when(applicationReviewService.approveApplication(eq(APP_ID), eq(request.getRemarks()), eq("52"), eq("ROLE_ADMIN")))
                .thenReturn(response);

        mockMvc.perform(post("/api/admin/applications/" + APP_ID + "/review")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.applicationNumber").value("SB-APP-2026-000555"));
    }

    @Test
    @DisplayName("2. Canonical Review: REJECT action with remarks succeeds")
    public void testReviewApplication_RejectAction_ReturnsOk() throws Exception {
        setupAdminAuth();

        ReviewActionRequest request = ReviewActionRequest.builder()
                .action("REJECT")
                .remarks("Income exceeds statutory scheme threshold.")
                .build();

        ApplicationResponse response = ApplicationResponse.builder()
                .id(APP_ID)
                .applicationNumber("SB-APP-2026-000555")
                .status("REJECTED")
                .build();

        when(applicationReviewService.rejectApplication(eq(APP_ID), eq(request.getRemarks()), eq("52"), eq("ROLE_ADMIN")))
                .thenReturn(response);

        mockMvc.perform(post("/api/admin/applications/" + APP_ID + "/review")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("3. Canonical Review: REQUEST_MORE_DOCUMENTS action succeeds")
    public void testReviewApplication_RequestMoreDocuments_ReturnsOk() throws Exception {
        setupAdminAuth();

        ReviewActionRequest request = ReviewActionRequest.builder()
                .action("REQUEST_MORE_DOCUMENTS")
                .remarks("Land certificate scan is blurry. Please re-upload.")
                .build();

        ApplicationResponse response = ApplicationResponse.builder()
                .id(APP_ID)
                .applicationNumber("SB-APP-2026-000555")
                .status("CORRECTION_REQUIRED")
                .build();

        when(applicationReviewService.requestMoreDocuments(eq(APP_ID), eq(request.getRemarks()), eq("52"), eq("ROLE_ADMIN")))
                .thenReturn(response);

        mockMvc.perform(post("/api/admin/applications/" + APP_ID + "/review")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CORRECTION_REQUIRED"));
    }

    @Test
    @DisplayName("4. Canonical Review: Undefined or blank action returns 400 Bad Request")
    public void testReviewApplication_UndefinedAction_ReturnsBadRequest() throws Exception {
        setupAdminAuth();

        ReviewActionRequest request = ReviewActionRequest.builder()
                .remarks("Testing undefined action")
                .build();

        mockMvc.perform(post("/api/admin/applications/" + APP_ID + "/review")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("5. Canonical Review: Unknown review action returns 400 Bad Request")
    public void testReviewApplication_UnknownAction_ReturnsBadRequest() throws Exception {
        setupAdminAuth();

        ReviewActionRequest request = ReviewActionRequest.builder()
                .action("UNRECOGNIZED_ACTION")
                .remarks("Invalid")
                .build();

        mockMvc.perform(post("/api/admin/applications/" + APP_ID + "/review")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6. Canonical Review: REJECT without remarks returns 400 Bad Request")
    public void testReviewApplication_RejectWithoutRemarks_ReturnsBadRequest() throws Exception {
        setupAdminAuth();

        ReviewActionRequest request = ReviewActionRequest.builder()
                .action("REJECT")
                .build();

        mockMvc.perform(post("/api/admin/applications/" + APP_ID + "/review")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7. Canonical Review: REQUEST_MORE_DOCUMENTS without remarks returns 400 Bad Request")
    public void testReviewApplication_RequestMoreDocsWithoutRemarks_ReturnsBadRequest() throws Exception {
        setupAdminAuth();

        ReviewActionRequest request = ReviewActionRequest.builder()
                .action("REQUEST_MORE_DOCUMENTS")
                .build();

        mockMvc.perform(post("/api/admin/applications/" + APP_ID + "/review")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("8. RBAC Security: Citizen cannot access admin review endpoint (403 Forbidden)")
    public void testReviewApplication_CitizenRole_ReturnsForbidden() throws Exception {
        setupCitizenAuth();

        ReviewActionRequest request = ReviewActionRequest.builder()
                .action("APPROVE")
                .remarks("Citizen attempting self-approval")
                .build();

        mockMvc.perform(post("/api/admin/applications/" + APP_ID + "/review")
                .header("Authorization", "Bearer " + CITIZEN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("9. Dedicated Endpoint: POST /review/request-documents succeeds")
    public void testDedicatedRequestDocuments_ReturnsOk() throws Exception {
        setupAdminAuth();

        ReviewDecisionRequest request = ReviewDecisionRequest.builder()
                .remarks("Please re-upload clearer bank passbook copy.")
                .build();

        ApplicationResponse response = ApplicationResponse.builder()
                .id(APP_ID)
                .applicationNumber("SB-APP-2026-000555")
                .status("CORRECTION_REQUIRED")
                .build();

        when(applicationReviewService.requestMoreDocuments(eq(APP_ID), eq(request.getRemarks()), eq("52"), eq("ROLE_ADMIN")))
                .thenReturn(response);

        mockMvc.perform(post("/api/admin/applications/" + APP_ID + "/review/request-documents")
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CORRECTION_REQUIRED"));
    }

    @Test
    @DisplayName("10. Exact Application Routing: GET /api/admin/applications/{applicationId} succeeds for Admin")
    public void testGetApplicationById_AdminRole_ReturnsOk() throws Exception {
        setupAdminAuth();

        ApplicationResponse response = ApplicationResponse.builder()
                .id(APP_ID)
                .applicationNumber("SB-APP-2026-000555")
                .status("SUBMITTED")
                .schemeCode("SCH-AGRI-001")
                .build();

        when(applicationReviewService.getApplicationForReview(APP_ID)).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/applications/" + APP_ID)
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(APP_ID))
                .andExpect(jsonPath("$.applicationNumber").value("SB-APP-2026-000555"))
                .andExpect(jsonPath("$.schemeCode").value("SCH-AGRI-001"));
    }

    @Test
    @DisplayName("11. Exact Application Routing: GET /api/admin/applications/{applicationId} returns 403 for Citizen")
    public void testGetApplicationById_CitizenRole_ReturnsForbidden() throws Exception {
        setupCitizenAuth();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/applications/" + APP_ID)
                .header("Authorization", "Bearer " + CITIZEN_TOKEN)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
