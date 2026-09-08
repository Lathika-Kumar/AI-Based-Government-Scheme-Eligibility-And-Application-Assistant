package com.schemebridge.scheme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.scheme.config.SecurityConfig;
import com.schemebridge.scheme.document.GrievanceStatus;
import com.schemebridge.scheme.dto.request.GrievanceReplyRequest;
import com.schemebridge.scheme.dto.response.GrievanceResponse;
import com.schemebridge.scheme.security.JwtTokenProvider;
import com.schemebridge.scheme.service.GrievanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GrievanceController.class)
@Import(SecurityConfig.class)
public class GrievanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GrievanceService grievanceService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private void mockAdminToken(String token) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn("admin-user-1");
        when(jwtTokenProvider.getRolesFromToken(token)).thenReturn(List.of("ADMIN"));
    }

    private void mockUserToken(String token) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn("regular-user-1");
        when(jwtTokenProvider.getRolesFromToken(token)).thenReturn(List.of("USER"));
    }

    @Test
    public void testAdminReply_Unauthenticated_ReturnsForbidden() throws Exception {
        GrievanceReplyRequest request = GrievanceReplyRequest.builder()
                .message("Official response from admin")
                .build();

        mockMvc.perform(post("/api/admin/grievances/g123/reply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testAdminReply_CitizenRole_ReturnsForbidden() throws Exception {
        String token = "user-token";
        mockUserToken(token);

        GrievanceReplyRequest request = GrievanceReplyRequest.builder()
                .message("Official response from admin")
                .build();

        mockMvc.perform(post("/api/admin/grievances/g123/reply")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testAdminReply_WithValidAdminToken_ReturnsOk() throws Exception {
        String token = "admin-token";
        mockAdminToken(token);

        GrievanceReplyRequest request = GrievanceReplyRequest.builder()
                .message("We have processed your request.")
                .attachments(List.of("doc1.pdf"))
                .internalOnly(false)
                .build();

        GrievanceResponse mockResponse = GrievanceResponse.builder()
                .id("g123")
                .grievanceNumber("GRV-2026-001")
                .status(GrievanceStatus.IN_PROGRESS)
                .category("SCHEME_APPLICATION")
                .subject("Delay in verification")
                .description("Application pending for 2 weeks")
                .userId("citizen-10")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .timeline(Collections.emptyList())
                .build();

        when(grievanceService.replyToGrievance(eq("g123"), any(GrievanceReplyRequest.class), eq("admin-user-1"), eq("ROLE_ADMIN")))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/admin/grievances/g123/reply")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("g123"))
                .andExpect(jsonPath("$.grievanceNumber").value("GRV-2026-001"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    public void testAdminRespond_WithValidAdminToken_ReturnsOk() throws Exception {
        String token = "admin-token";
        mockAdminToken(token);

        GrievanceReplyRequest request = GrievanceReplyRequest.builder()
                .message("Legacy respond endpoint works as well.")
                .build();

        GrievanceResponse mockResponse = GrievanceResponse.builder()
                .id("g123")
                .grievanceNumber("GRV-2026-001")
                .status(GrievanceStatus.IN_PROGRESS)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .timeline(Collections.emptyList())
                .build();

        when(grievanceService.replyToGrievance(eq("g123"), any(GrievanceReplyRequest.class), eq("admin-user-1"), eq("ROLE_ADMIN")))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/admin/grievances/g123/respond")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("g123"));
    }

    @Test
    public void testAdminReply_BlankMessage_ReturnsBadRequest() throws Exception {
        String token = "admin-token";
        mockAdminToken(token);

        GrievanceReplyRequest request = GrievanceReplyRequest.builder()
                .message("")
                .build();

        mockMvc.perform(post("/api/admin/grievances/g123/reply")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
