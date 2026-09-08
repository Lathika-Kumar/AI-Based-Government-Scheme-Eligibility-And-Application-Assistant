package com.schemebridge.scheme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.scheme.document.RecommendationEventType;
import com.schemebridge.scheme.dto.request.RecommendationEventRequest;
import com.schemebridge.scheme.dto.response.RecommendationEventMetricsResponse;
import com.schemebridge.scheme.dto.response.RecommendationEventResponse;
import com.schemebridge.scheme.service.RecommendationEventService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecommendationEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecommendationEventService eventService;

    @Test
    @WithMockUser(username = "65", roles = {"CITIZEN"})
    @DisplayName("POST /api/recommendations/events - Successfully records authenticated event")
    void testRecordEvent_Authenticated_Success() throws Exception {
        RecommendationEventRequest request = RecommendationEventRequest.builder()
                .schemeCode("PM_KISAN_2026")
                .eventType(RecommendationEventType.SCHEME_VIEWED)
                .recommendationRank(1)
                .recommendationScore(0.9583)
                .sessionId("session-123")
                .build();

        RecommendationEventResponse mockResponse = RecommendationEventResponse.builder()
                .eventId("evt-001")
                .userId("65")
                .schemeCode("PM_KISAN_2026")
                .eventType(RecommendationEventType.SCHEME_VIEWED)
                .timestamp(Instant.now())
                .status("RECORDED")
                .build();

        when(eventService.recordEvent(eq("65"), any(RecommendationEventRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/recommendations/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-001"))
                .andExpect(jsonPath("$.userId").value("65"))
                .andExpect(jsonPath("$.schemeCode").value("PM_KISAN_2026"))
                .andExpect(jsonPath("$.eventType").value("SCHEME_VIEWED"))
                .andExpect(jsonPath("$.status").value("RECORDED"));
    }

    @Test
    @WithMockUser(username = "65", roles = {"CITIZEN"})
    @DisplayName("POST /api/recommendations/events - Invalid payload (missing schemeCode) returns 400 Bad Request")
    void testRecordEvent_MissingSchemeCode_Returns400() throws Exception {
        RecommendationEventRequest invalidRequest = RecommendationEventRequest.builder()
                .eventType(RecommendationEventType.SCHEME_VIEWED)
                .build();

        mockMvc.perform(post("/api/recommendations/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "65", roles = {"CITIZEN"})
    @DisplayName("GET /api/recommendations/events/metrics - Successfully returns event telemetry metrics")
    void testGetMetrics_Success() throws Exception {
        RecommendationEventMetricsResponse mockMetrics = RecommendationEventMetricsResponse.builder()
                .totalEvents(25L)
                .eventsByType(Map.of("SCHEME_VIEWED", 15L, "SCHEME_EXPANDED", 10L))
                .uniqueCitizens(5L)
                .uniqueSchemes(12L)
                .currentModelVersion("schemebridge-recommender-v2-hybrid-semantic")
                .calculatedAt(Instant.now())
                .build();

        when(eventService.getMetrics()).thenReturn(mockMetrics);

        mockMvc.perform(get("/api/recommendations/events/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(25))
                .andExpect(jsonPath("$.uniqueCitizens").value(5))
                .andExpect(jsonPath("$.uniqueSchemes").value(12))
                .andExpect(jsonPath("$.currentModelVersion").value("schemebridge-recommender-v2-hybrid-semantic"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /api/recommendations/events/readiness - Returns Phase 31/32/33 continuous monitoring snapshot")
    void testGetReadiness_Success() throws Exception {
        mockMvc.perform(get("/api/recommendations/events/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legitimateOutcomeSessions").value(0))
                .andExpect(jsonPath("$.threshold").value(100))
                .andExpect(jsonPath("$.remaining").value(100))
                .andExpect(jsonPath("$.trainingReady").value(false))
                .andExpect(jsonPath("$.modelTrainingAllowed").value(false))
                .andExpect(jsonPath("$.modelPromotionAllowed").value(false))
                .andExpect(jsonPath("$.activeModel").value("2.2.0-hybrid-semantic-384d"))
                .andExpect(jsonPath("$.fallbackModel").value("1.0.0-deterministic"))
                .andExpect(jsonPath("$.readinessReport").exists())
                .andExpect(jsonPath("$.readinessReport.status").value("TRAINING_NOT_READY"))
                .andExpect(jsonPath("$.readinessReport.modelPromotionAllowed").value(false))
                .andExpect(jsonPath("$.progressReport").exists())
                .andExpect(jsonPath("$.qualityReport").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /api/recommendations/events/training-readiness - Returns Phase 34 Training Readiness Handoff")
    void testGetTrainingReadiness_Success() throws Exception {
        mockMvc.perform(get("/api/recommendations/events/training-readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TRAINING_NOT_READY"))
                .andExpect(jsonPath("$.legitimateOutcomeSessions").value(0))
                .andExpect(jsonPath("$.threshold").value(100))
                .andExpect(jsonPath("$.remainingSessions").value(100))
                .andExpect(jsonPath("$.trainingReady").value(false))
                .andExpect(jsonPath("$.modelTrainingAllowed").value(false))
                .andExpect(jsonPath("$.modelPromotionAllowed").value(false))
                .andExpect(jsonPath("$.activeModel").value("2.2.0-hybrid-semantic-384d"))
                .andExpect(jsonPath("$.fallbackModel").value("1.0.0-deterministic"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /api/recommendations/events/training-report - Live returns BLOCKED_BY_READINESS below 100")
    void testGetTrainingReport_BlockedBelow100() throws Exception {
        mockMvc.perform(get("/api/recommendations/events/training-report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingStatus").value("BLOCKED_BY_READINESS"))
                .andExpect(jsonPath("$.candidateModelVersion").value("NONE"))
                .andExpect(jsonPath("$.activeModel").value("2.2.0-hybrid-semantic-384d"))
                .andExpect(jsonPath("$.fallbackModel").value("1.0.0-deterministic"))
                .andExpect(jsonPath("$.modelTrainingAllowed").value(false))
                .andExpect(jsonPath("$.modelPromotionAllowed").value(false))
                .andExpect(jsonPath("$.governanceState").value("NONE"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /api/recommendations/events/model-evaluation - Live returns INSUFFICIENT_DATA below 100")
    void testGetModelEvaluation_InsufficientDataBelow100() throws Exception {
        mockMvc.perform(get("/api/recommendations/events/model-evaluation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationStatus").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.baselineModel").value("2.2.0-hybrid-semantic-384d"))
                .andExpect(jsonPath("$.fallbackModel").value("1.0.0-deterministic"))
                .andExpect(jsonPath("$.candidateModel").value("NONE"))
                .andExpect(jsonPath("$.comparisonResult").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.modelPromotionAllowed").value(false))
                .andExpect(jsonPath("$.governanceStatus").value("PENDING_HUMAN_GOVERNANCE"));
    }
}
