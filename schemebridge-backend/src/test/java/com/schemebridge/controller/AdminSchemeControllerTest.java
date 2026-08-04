package com.schemebridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.dto.SchemeRequest;
import com.schemebridge.dto.SchemeResponse;
import com.schemebridge.enums.SchemeType;
import com.schemebridge.service.SchemeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminSchemeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SchemeService schemeService;

    @Test
    @DisplayName("POST /api/v1/admin/schemes allows SUPER_ADMIN to create scheme")
    @WithMockUser(username = "admin@schemebridge.gov.in", roles = {"SUPER_ADMIN"})
    void testCreateSchemeAsAdmin() throws Exception {
        SchemeRequest request = SchemeRequest.builder()
                .schemeName("New Admin Scheme")
                .schemeCode("ADM-SCH-001")
                .description("Admin scheme description")
                .category("Education")
                .schemeType(SchemeType.CENTRAL)
                .build();

        SchemeResponse response = SchemeResponse.builder()
                .id("scheme-admin-1")
                .schemeName("New Admin Scheme")
                .schemeCode("ADM-SCH-001")
                .build();

        when(schemeService.createScheme(any(), eq("admin@schemebridge.gov.in"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/schemes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.schemeName").value("New Admin Scheme"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/schemes forbids CITIZEN role")
    @WithMockUser(username = "citizen@demo.com", roles = {"CITIZEN"})
    void testCreateSchemeForbiddenForCitizen() throws Exception {
        SchemeRequest request = SchemeRequest.builder()
                .schemeName("Forbidden Scheme")
                .schemeCode("FORBID-001")
                .description("Description")
                .category("Education")
                .schemeType(SchemeType.CENTRAL)
                .build();

        mockMvc.perform(post("/api/v1/admin/schemes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
