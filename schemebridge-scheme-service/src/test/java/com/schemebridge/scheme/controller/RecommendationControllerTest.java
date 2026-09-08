package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.response.PersonalizedSchemeRecommendationResponse;
import com.schemebridge.scheme.dto.response.RankedSchemeItem;
import com.schemebridge.scheme.service.EligibleSchemeRecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EligibleSchemeRecommendationService recommendationService;

    @Test
    @WithMockUser(username = "65", roles = {"CITIZEN"})
    @DisplayName("GET /api/recommendations returns 200 OK with ranked recommendations")
    void testGetRecommendations_Success() throws Exception {
        RankedSchemeItem item = RankedSchemeItem.builder()
                .rank(1)
                .schemeCode("PM_KISAN_2026")
                .schemeTitle("PM-KISAN Samman Nidhi")
                .recommendationScore(0.9250)
                .eligibilityStatus("ELIGIBLE")
                .confidenceScore(1.0)
                .build();

        PersonalizedSchemeRecommendationResponse response = PersonalizedSchemeRecommendationResponse.builder()
                .userId("65")
                .citizenState("Maharashtra")
                .totalCatalogEvaluated(794)
                .eligibleCandidatesFound(1)
                .totalRecommendationsReturned(1)
                .page(0)
                .size(10)
                .generatedAt(Instant.now())
                .modelFamily("Multi-Criteria Feature Utility & Vector Space Model")
                .modelVersion("v1.0-deterministic-gated")
                .recommendations(List.of(item))
                .build();

        when(recommendationService.getPersonalizedRecommendations(eq("65"), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("65"))
                .andExpect(jsonPath("$.eligibleCandidatesFound").value(1))
                .andExpect(jsonPath("$.recommendations[0].schemeCode").value("PM_KISAN_2026"))
                .andExpect(jsonPath("$.recommendations[0].rank").value(1))
                .andExpect(jsonPath("$.recommendations[0].eligibilityStatus").value("ELIGIBLE"))
                .andExpect(jsonPath("$.recommendations[0].recommendationScore").value(0.925));
    }

    @Test
    @WithMockUser(username = "65", roles = {"CITIZEN"})
    @DisplayName("POST /api/recommendations returns 200 OK with ranked recommendations")
    void testPostRecommendations_Success() throws Exception {
        PersonalizedSchemeRecommendationResponse response = PersonalizedSchemeRecommendationResponse.builder()
                .userId("65")
                .citizenState("Maharashtra")
                .totalCatalogEvaluated(794)
                .eligibleCandidatesFound(0)
                .totalRecommendationsReturned(0)
                .page(0)
                .size(10)
                .recommendations(List.of())
                .build();

        when(recommendationService.getPersonalizedRecommendations(eq("65"), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("65"))
                .andExpect(jsonPath("$.eligibleCandidatesFound").value(0));
    }
}
