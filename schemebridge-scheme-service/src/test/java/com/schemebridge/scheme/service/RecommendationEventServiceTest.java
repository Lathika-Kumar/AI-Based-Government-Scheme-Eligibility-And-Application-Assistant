package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.RecommendationEvent;
import com.schemebridge.scheme.document.RecommendationEventType;
import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.dto.request.RecommendationEventRequest;
import com.schemebridge.scheme.dto.response.RecommendationEventMetricsResponse;
import com.schemebridge.scheme.dto.response.RecommendationEventResponse;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.repository.RecommendationEventRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationEventServiceTest {

    @Mock
    private RecommendationEventRepository eventRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @InjectMocks
    private RecommendationEventService eventService;

    private Scheme mockScheme;

    @BeforeEach
    void setUp() {
        mockScheme = Scheme.builder()
                .schemeCode("PM_KISAN_2026")
                .slug("pm-kisan-samman-nidhi")
                .build();
    }

    @Test
    @DisplayName("Record Event: Successfully stores genuine user interaction event with recommendation snapshot")
    void testRecordEvent_Success() {
        when(schemeRepository.findBySchemeCode("PM_KISAN_2026")).thenReturn(Optional.of(mockScheme));

        RecommendationEvent savedEvent = RecommendationEvent.builder()
                .id("evt-123")
                .userId("65")
                .schemeCode("PM_KISAN_2026")
                .eventType(RecommendationEventType.SCHEME_VIEWED)
                .timestamp(Instant.now())
                .build();

        when(eventRepository.save(any(RecommendationEvent.class))).thenReturn(savedEvent);

        RecommendationEventRequest request = RecommendationEventRequest.builder()
                .schemeCode("PM_KISAN_2026")
                .eventType(RecommendationEventType.SCHEME_VIEWED)
                .recommendationRank(1)
                .recommendationScore(0.9583)
                .sessionId("sess-xyz")
                .build();

        RecommendationEventResponse response = eventService.recordEvent("65", request);

        assertNotNull(response);
        assertEquals("evt-123", response.getEventId());
        assertEquals("65", response.getUserId());
        assertEquals("PM_KISAN_2026", response.getSchemeCode());
        assertEquals(RecommendationEventType.SCHEME_VIEWED, response.getEventType());
        assertEquals("RECORDED", response.getStatus());

        verify(eventRepository, times(1)).save(any(RecommendationEvent.class));
    }

    @Test
    @DisplayName("Record Event: Throws ResourceNotFoundException when scheme code is invalid")
    void testRecordEvent_InvalidScheme_ThrowsException() {
        when(schemeRepository.findBySchemeCode("NON_EXISTENT")).thenReturn(Optional.empty());

        RecommendationEventRequest request = RecommendationEventRequest.builder()
                .schemeCode("NON_EXISTENT")
                .eventType(RecommendationEventType.SCHEME_VIEWED)
                .build();

        assertThrows(ResourceNotFoundException.class, () -> eventService.recordEvent("65", request));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get Metrics: Calculates total events, unique citizens, and unique schemes")
    void testGetMetrics_Success() {
        when(eventRepository.count()).thenReturn(10L);
        when(eventRepository.countByEventType(RecommendationEventType.RECOMMENDATION_SHOWN)).thenReturn(0L);
        when(eventRepository.countByEventType(RecommendationEventType.SCHEME_VIEWED)).thenReturn(5L);
        when(eventRepository.countByEventType(RecommendationEventType.SCHEME_EXPANDED)).thenReturn(3L);
        when(eventRepository.countByEventType(RecommendationEventType.SCHEME_SAVED)).thenReturn(1L);
        when(eventRepository.countByEventType(RecommendationEventType.APPLICATION_STARTED)).thenReturn(0L);
        when(eventRepository.countByEventType(RecommendationEventType.SCHEME_APPLIED)).thenReturn(1L);
        when(eventRepository.countByEventType(RecommendationEventType.APPLICATION_COMPLETED)).thenReturn(0L);

        RecommendationEvent e1 = RecommendationEvent.builder().userId("65").schemeCode("PM_KISAN_2026").build();
        RecommendationEvent e2 = RecommendationEvent.builder().userId("82").schemeCode("TN_EDU_01").build();
        when(eventRepository.findAll()).thenReturn(List.of(e1, e2));

        RecommendationEventMetricsResponse metrics = eventService.getMetrics();

        assertNotNull(metrics);
        assertEquals(10L, metrics.getTotalEvents());
        assertEquals(2L, metrics.getUniqueCitizens());
        assertEquals(2L, metrics.getUniqueSchemes());
        assertEquals(EligibleSchemeRecommendationService.MODEL_VERSION, metrics.getCurrentModelVersion());
    }
}
