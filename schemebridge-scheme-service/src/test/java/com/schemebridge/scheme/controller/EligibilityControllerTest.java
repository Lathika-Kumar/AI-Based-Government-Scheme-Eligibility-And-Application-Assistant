package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.response.CitizenEligibilityEvaluationResponse;
import com.schemebridge.scheme.dto.response.CitizenEligibilityEvaluationResponse.EvaluationSummary;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResult;
import com.schemebridge.scheme.dto.response.EligibilityStatus;
import com.schemebridge.scheme.service.EligibilityEvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EligibilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EligibilityEvaluationService eligibilityEvaluationService;

    @Test
    @WithMockUser(username = "101")
    @DisplayName("POST /api/eligibility/evaluate: Returns 200 with partitioned evaluation response")
    void testEvaluateAll() throws Exception {
        CitizenEligibilityEvaluationResponse resp = CitizenEligibilityEvaluationResponse.builder()
                .userId("101")
                .citizenState("Tamil Nadu")
                .evaluatedAt(Instant.now())
                .summary(EvaluationSummary.builder()
                        .totalEvaluated(10)
                        .eligibleCount(2)
                        .insufficientDataCount(3)
                        .notEligibleCount(5)
                        .build())
                .eligibleSchemes(List.of(
                        EligibilityEvaluationResult.builder()
                                .schemeCode("PM-KISAN")
                                .status(EligibilityStatus.ELIGIBLE)
                                .build()
                ))
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("101")).thenReturn(resp);

        mockMvc.perform(post("/api/eligibility/evaluate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("101"))
                .andExpect(jsonPath("$.citizenState").value("Tamil Nadu"))
                .andExpect(jsonPath("$.summary.totalEvaluated").value(10))
                .andExpect(jsonPath("$.summary.eligibleCount").value(2))
                .andExpect(jsonPath("$.eligibleSchemes[0].schemeCode").value("PM-KISAN"));
    }

    @Test
    @WithMockUser(username = "101")
    @DisplayName("POST /api/eligibility/evaluate/{schemeCode}: Returns 200 with single scheme evaluation result")
    void testEvaluateScheme() throws Exception {
        EligibilityEvaluationResult result = EligibilityEvaluationResult.builder()
                .schemeCode("PM-KISAN")
                .schemeTitle("PM Kisan Samman Nidhi")
                .status(EligibilityStatus.ELIGIBLE)
                .passedConditions(List.of("National Central Scheme", "Farmer status verified"))
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstScheme("101", "PM-KISAN")).thenReturn(result);

        mockMvc.perform(post("/api/eligibility/evaluate/PM-KISAN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemeCode").value("PM-KISAN"))
                .andExpect(jsonPath("$.status").value("ELIGIBLE"))
                .andExpect(jsonPath("$.passedConditions[0]").value("National Central Scheme"));
    }
}
