package com.schemebridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.common.ApiResponse;
import com.schemebridge.dto.SchemeCardResponse;
import com.schemebridge.dto.SchemeResponse;
import com.schemebridge.enums.SchemeType;
import com.schemebridge.service.SchemeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SchemeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SchemeService schemeService;

    @Test
    @DisplayName("GET /api/v1/schemes returns list of published schemes")
    @WithMockUser(username = "citizen@demo.com", roles = {"CITIZEN"})
    void testListSchemes() throws Exception {
        SchemeCardResponse card = SchemeCardResponse.builder()
                .id("scheme-1")
                .schemeName("PM Kisan")
                .shortDescription("Income support")
                .category("Agriculture")
                .schemeType(SchemeType.CENTRAL)
                .featured(true)
                .build();

        when(schemeService.listSchemes(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(card), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/schemes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].schemeName").value("PM Kisan"));
    }
}
