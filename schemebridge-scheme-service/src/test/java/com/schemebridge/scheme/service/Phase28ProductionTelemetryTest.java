package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.request.RecommendationEventRequest;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResult;
import com.schemebridge.scheme.dto.response.EligibilityStatus;
import com.schemebridge.scheme.dto.response.RecommendationEventResponse;
import com.schemebridge.scheme.ml.dataset.*;
import com.schemebridge.scheme.ml.feature.*;
import com.schemebridge.scheme.repository.ApplicationEventRepository;
import com.schemebridge.scheme.repository.RecommendationEventRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase28ProductionTelemetryTest {

    @Mock
    private RecommendationEventRepository recommendationEventRepository;

    @Mock
    private ApplicationEventRepository applicationEventRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private EligibilityEngine eligibilityEngine;

    private UserFeatureExtractor userFeatureExtractor;
    private SchemeFeatureExtractor schemeFeatureExtractor;
    private UserSchemeComparisonService userSchemeComparisonService;
    private FeatureVectorBuilder featureVectorBuilder;
    private SessionAttributionService sessionAttributionService;
    private RecommendationEventService recommendationEventService;
    private Phase28ReadinessMonitor readinessMonitor;

    @BeforeEach
    void setUp() {
        userFeatureExtractor = new UserFeatureExtractor();
        schemeFeatureExtractor = new SchemeFeatureExtractor();
        userSchemeComparisonService = new UserSchemeComparisonService();
        featureVectorBuilder = new FeatureVectorBuilder();
        sessionAttributionService = new SessionAttributionService(
                eligibilityEngine,
                userFeatureExtractor,
                schemeFeatureExtractor,
                userSchemeComparisonService,
                featureVectorBuilder
        );
        recommendationEventService = new RecommendationEventService(
                recommendationEventRepository,
                schemeRepository
        );
        readinessMonitor = new Phase28ReadinessMonitor(
                recommendationEventRepository,
                applicationEventRepository,
                sessionAttributionService
        );
    }

    private Scheme createTestScheme(String schemeCode) {
        Scheme s = new Scheme();
        s.setId("scheme_id_" + schemeCode);
        s.setSchemeCode(schemeCode);
        s.setSchemeLevel(SchemeLevel.CENTRAL);
        s.setStateOrUt("ALL");
        return s;
    }

    @Nested
    @DisplayName("Impression Deduplication Tests")
    class ImpressionDeduplicationTests {

        @Test
        @DisplayName("1. First recommendation impression is recorded")
        void testFirstImpressionRecorded() {
            String schemeCode = "SCH_001";
            String sessionId = "sess_user_1";
            Scheme scheme = createTestScheme(schemeCode);

            when(schemeRepository.findBySchemeCode(schemeCode)).thenReturn(Optional.of(scheme));
            when(recommendationEventRepository.findFirstBySessionIdAndSchemeCodeAndEventType(
                    sessionId, schemeCode, RecommendationEventType.RECOMMENDATION_SHOWN))
                    .thenReturn(Optional.empty());

            RecommendationEvent savedEvent = RecommendationEvent.builder()
                    .id("evt_001")
                    .userId("usr_real_1")
                    .schemeCode(schemeCode)
                    .eventType(RecommendationEventType.RECOMMENDATION_SHOWN)
                    .sessionId(sessionId)
                    .timestamp(Instant.now())
                    .build();
            when(recommendationEventRepository.save(any(RecommendationEvent.class))).thenReturn(savedEvent);

            RecommendationEventRequest req = RecommendationEventRequest.builder()
                    .schemeCode(schemeCode)
                    .eventType(RecommendationEventType.RECOMMENDATION_SHOWN)
                    .recommendationRank(1)
                    .recommendationScore(0.95)
                    .sessionId(sessionId)
                    .build();

            RecommendationEventResponse resp = recommendationEventService.recordEvent("usr_real_1", req);
            assertThat(resp.getStatus()).isEqualTo("RECORDED");
            verify(recommendationEventRepository, times(1)).save(any(RecommendationEvent.class));
        }

        @Test
        @DisplayName("2. Repeated rendering of same scheme in same session is deduplicated")
        void testRepeatedRenderingDeduplicated() {
            String schemeCode = "SCH_001";
            String sessionId = "sess_user_1";
            Scheme scheme = createTestScheme(schemeCode);

            when(schemeRepository.findBySchemeCode(schemeCode)).thenReturn(Optional.of(scheme));

            RecommendationEvent existingEvent = RecommendationEvent.builder()
                    .id("evt_existing_001")
                    .userId("usr_real_1")
                    .schemeCode(schemeCode)
                    .eventType(RecommendationEventType.RECOMMENDATION_SHOWN)
                    .sessionId(sessionId)
                    .timestamp(Instant.now().minusSeconds(10))
                    .build();

            when(recommendationEventRepository.findFirstBySessionIdAndSchemeCodeAndEventType(
                    sessionId, schemeCode, RecommendationEventType.RECOMMENDATION_SHOWN))
                    .thenReturn(Optional.of(existingEvent));

            RecommendationEventRequest req = RecommendationEventRequest.builder()
                    .schemeCode(schemeCode)
                    .eventType(RecommendationEventType.RECOMMENDATION_SHOWN)
                    .recommendationRank(1)
                    .recommendationScore(0.95)
                    .sessionId(sessionId)
                    .build();

            RecommendationEventResponse resp = recommendationEventService.recordEvent("usr_real_1", req);
            assertThat(resp.getStatus()).isEqualTo("DEDUPLICATED");
            assertThat(resp.getEventId()).isEqualTo("evt_existing_001");
            verify(recommendationEventRepository, never()).save(any(RecommendationEvent.class));
        }

        @Test
        @DisplayName("3. Different schemes in same session are recorded separately")
        void testDifferentSchemesRecordedSeparately() {
            String schemeCode1 = "SCH_001";
            String schemeCode2 = "SCH_002";
            String sessionId = "sess_user_1";

            Scheme s1 = createTestScheme(schemeCode1);
            Scheme s2 = createTestScheme(schemeCode2);

            when(schemeRepository.findBySchemeCode(schemeCode1)).thenReturn(Optional.of(s1));
            when(schemeRepository.findBySchemeCode(schemeCode2)).thenReturn(Optional.of(s2));

            when(recommendationEventRepository.findFirstBySessionIdAndSchemeCodeAndEventType(
                    sessionId, schemeCode1, RecommendationEventType.RECOMMENDATION_SHOWN))
                    .thenReturn(Optional.empty());
            when(recommendationEventRepository.findFirstBySessionIdAndSchemeCodeAndEventType(
                    sessionId, schemeCode2, RecommendationEventType.RECOMMENDATION_SHOWN))
                    .thenReturn(Optional.empty());

            when(recommendationEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            RecommendationEventRequest req1 = RecommendationEventRequest.builder()
                    .schemeCode(schemeCode1)
                    .eventType(RecommendationEventType.RECOMMENDATION_SHOWN)
                    .sessionId(sessionId)
                    .build();
            RecommendationEventRequest req2 = RecommendationEventRequest.builder()
                    .schemeCode(schemeCode2)
                    .eventType(RecommendationEventType.RECOMMENDATION_SHOWN)
                    .sessionId(sessionId)
                    .build();

            RecommendationEventResponse r1 = recommendationEventService.recordEvent("usr_real_1", req1);
            RecommendationEventResponse r2 = recommendationEventService.recordEvent("usr_real_1", req2);

            assertThat(r1.getStatus()).isEqualTo("RECORDED");
            assertThat(r2.getStatus()).isEqualTo("RECORDED");
            verify(recommendationEventRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("4. Different sessions for same scheme are recorded separately")
        void testDifferentSessionsRecordedSeparately() {
            String schemeCode = "SCH_001";
            String sessionA = "sess_A";
            String sessionB = "sess_B";
            Scheme scheme = createTestScheme(schemeCode);

            when(schemeRepository.findBySchemeCode(schemeCode)).thenReturn(Optional.of(scheme));

            when(recommendationEventRepository.findFirstBySessionIdAndSchemeCodeAndEventType(
                    sessionA, schemeCode, RecommendationEventType.RECOMMENDATION_SHOWN))
                    .thenReturn(Optional.empty());
            when(recommendationEventRepository.findFirstBySessionIdAndSchemeCodeAndEventType(
                    sessionB, schemeCode, RecommendationEventType.RECOMMENDATION_SHOWN))
                    .thenReturn(Optional.empty());

            when(recommendationEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            RecommendationEventRequest reqA = RecommendationEventRequest.builder()
                    .schemeCode(schemeCode)
                    .eventType(RecommendationEventType.RECOMMENDATION_SHOWN)
                    .sessionId(sessionA)
                    .build();
            RecommendationEventRequest reqB = RecommendationEventRequest.builder()
                    .schemeCode(schemeCode)
                    .eventType(RecommendationEventType.RECOMMENDATION_SHOWN)
                    .sessionId(sessionB)
                    .build();

            RecommendationEventResponse rA = recommendationEventService.recordEvent("usr_1", reqA);
            RecommendationEventResponse rB = recommendationEventService.recordEvent("usr_2", reqB);

            assertThat(rA.getStatus()).isEqualTo("RECORDED");
            assertThat(rB.getStatus()).isEqualTo("RECORDED");
            verify(recommendationEventRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("5. Session restart creates fresh telemetry event")
        void testSessionRestart() {
            String schemeCode = "SCH_RESTART";
            String oldSession = "sess_old";
            String newSession = "sess_new";
            Scheme scheme = createTestScheme(schemeCode);

            when(schemeRepository.findBySchemeCode(schemeCode)).thenReturn(Optional.of(scheme));

            // Old session already had impression
            when(recommendationEventRepository.findFirstBySessionIdAndSchemeCodeAndEventType(
                    oldSession, schemeCode, RecommendationEventType.RECOMMENDATION_SHOWN))
                    .thenReturn(Optional.of(new RecommendationEvent()));

            // New session does not
            when(recommendationEventRepository.findFirstBySessionIdAndSchemeCodeAndEventType(
                    newSession, schemeCode, RecommendationEventType.RECOMMENDATION_SHOWN))
                    .thenReturn(Optional.empty());

            when(recommendationEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RecommendationEventRequest oldReq = RecommendationEventRequest.builder()
                    .schemeCode(schemeCode)
                    .eventType(RecommendationEventType.RECOMMENDATION_SHOWN)
                    .sessionId(oldSession)
                    .build();
            RecommendationEventRequest newReq = RecommendationEventRequest.builder()
                    .schemeCode(schemeCode)
                    .eventType(RecommendationEventType.RECOMMENDATION_SHOWN)
                    .sessionId(newSession)
                    .build();

            RecommendationEventResponse oldResp = recommendationEventService.recordEvent("usr_1", oldReq);
            RecommendationEventResponse newResp = recommendationEventService.recordEvent("usr_1", newReq);

            assertThat(oldResp.getStatus()).isEqualTo("DEDUPLICATED");
            assertThat(newResp.getStatus()).isEqualTo("RECORDED");
        }
    }

    @Nested
    @DisplayName("PII Sanitization Tests")
    class PiiSanitizationTests {

        @Test
        @DisplayName("Sanitization: Exact forbidden keys are completely stripped")
        void testExactForbiddenKeysStripped() {
            Map<String, Object> raw = new HashMap<>();
            raw.put("aadhaar", "123456789012");
            raw.put("email", "citizen@example.com");
            raw.put("phone", "9876543210");
            raw.put("pan", "ABCDE1234F");
            raw.put("name", "John Doe");
            raw.put("surface", "recommendations_feed");

            Map<String, Object> sanitized = recommendationEventService.sanitizeMetadata(raw);
            assertThat(sanitized).doesNotContainKeys("aadhaar", "email", "phone", "pan", "name");
            assertThat(sanitized).containsEntry("surface", "recommendations_feed");
        }

        @Test
        @DisplayName("Sanitization: Mixed-case and delimited forbidden keys are stripped")
        void testMixedCaseForbiddenKeysStripped() {
            Map<String, Object> raw = new HashMap<>();
            raw.put("Aadhaar_Number", "1234-5678-9012");
            raw.put("User-Email", "user@gov.in");
            raw.put("Mobile_Number", "9988776655");
            raw.put("FirstName", "Raj");
            raw.put("LastName", "Patel");
            raw.put("IP_Address", "127.0.0.1");
            raw.put("clientPlatform", "WEB");

            Map<String, Object> sanitized = recommendationEventService.sanitizeMetadata(raw);
            assertThat(sanitized).doesNotContainKeys(
                    "Aadhaar_Number", "User-Email", "Mobile_Number", "FirstName", "LastName", "IP_Address"
            );
            assertThat(sanitized).containsEntry("clientPlatform", "WEB");
        }

        @Test
        @DisplayName("Sanitization: Nested metadata structures are recursively sanitized")
        void testNestedMetadataSanitized() {
            Map<String, Object> nested = new HashMap<>();
            nested.put("email", "nested@example.com");
            nested.put("theme", "dark");

            Map<String, Object> raw = new HashMap<>();
            raw.put("userPreferences", nested);
            raw.put("viewportWidth", 1024);

            Map<String, Object> sanitized = recommendationEventService.sanitizeMetadata(raw);
            assertThat(sanitized).containsKey("userPreferences");

            @SuppressWarnings("unchecked")
            Map<String, Object> cleanNested = (Map<String, Object>) sanitized.get("userPreferences");
            assertThat(cleanNested).doesNotContainKey("email");
            assertThat(cleanNested).containsEntry("theme", "dark");
            assertThat(sanitized).containsEntry("viewportWidth", 1024);
        }

        @Test
        @DisplayName("Sanitization: Null and empty metadata safely return empty map")
        void testNullAndEmptyMetadata() {
            assertThat(recommendationEventService.sanitizeMetadata(null)).isEmpty();
            assertThat(recommendationEventService.sanitizeMetadata(Map.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Synthetic Fixture Quarantine Tests")
    class SyntheticFixtureQuarantineTests {

        @Test
        @DisplayName("Quarantine: Known fixture IDs are excluded from legitimate telemetry")
        void testKnownFixturesQuarantined() {
            assertThat(sessionAttributionService.isSyntheticFixture("citizen1", null, null)).isTrue();
            assertThat(sessionAttributionService.isSyntheticFixture("citizen_user", null, null)).isTrue();
            assertThat(sessionAttributionService.isSyntheticFixture("citizen_sharma_65", null, null)).isTrue();
            assertThat(sessionAttributionService.isSyntheticFixture("test_citizen_65", null, null)).isTrue();
            assertThat(sessionAttributionService.isSyntheticFixture(null, "fixture_session", null)).isTrue();

            // Genuine citizen
            assertThat(sessionAttributionService.isSyntheticFixture("usr_real_7812", "sess_9123", "evt_881")).isFalse();
        }
    }

    @Nested
    @DisplayName("Phase 28 Readiness Monitor Tests")
    class ReadinessMonitorTests {

        @Test
        @DisplayName("Monitor: 0 legitimate outcome sessions strictly reports TRAINING_NOT_READY")
        void testReadinessMonitorZeroOutcomes() {
            // Mock 29 historical application fixtures
            List<ApplicationEvent> fixtures = new ArrayList<>();
            for (int i = 0; i < 29; i++) {
                fixtures.add(ApplicationEvent.builder()
                        .id("app_evt_" + i)
                        .applicationId("6a952810c6037907f0030c06")
                        .userId("citizen_user")
                        .createdAt(Instant.now())
                        .build());
            }

            when(applicationEventRepository.findAll()).thenReturn(fixtures);
            when(recommendationEventRepository.findAll()).thenReturn(Collections.emptyList());

            Phase28ReadinessReport report = readinessMonitor.generateReadinessReport();

            assertThat(report.getTrainingStatus()).isEqualTo(TrainingReadinessStatus.TRAINING_NOT_READY);
            assertThat(report.getLegitimateOutcomeSessions()).isEqualTo(0L);
            assertThat(report.getRequiredOutcomeSessions()).isEqualTo(100L);
            assertThat(report.getRemainingOutcomeSessions()).isEqualTo(100L);
            assertThat(report.getSyntheticFixturesQuarantined()).isEqualTo(29L);
            assertThat(report.getPiiViolations()).isEqualTo(0L);
            assertThat(report.getStatutoryEligibilityViolations()).isEqualTo(0L);
            assertThat(report.getActiveModel()).isEqualTo("2.2.0-hybrid-semantic-384d");
            assertThat(report.getFallbackModel()).isEqualTo("1.0.0-deterministic");
            assertThat(report.getCircuitBreakerMs()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Telemetry Lifecycle & Session Continuity Tests")
    class TelemetryLifecycleTests {

        @Test
        @DisplayName("Lifecycle: Full progression maintains sessionId + schemeCode continuity")
        void testSessionContinuity() {
            String sessionId = "sess_journey_441";
            String schemeCode = "SCH_JOURNEY_1";

            List<SessionInteractionEvent> journeyEvents = List.of(
                    SessionInteractionEvent.builder().sessionId(sessionId).schemeCode(schemeCode).eventType("RECOMMENDATION_SHOWN").timestamp(100L).build(),
                    SessionInteractionEvent.builder().sessionId(sessionId).schemeCode(schemeCode).eventType("SCHEME_VIEWED").timestamp(200L).build(),
                    SessionInteractionEvent.builder().sessionId(sessionId).schemeCode(schemeCode).eventType("SCHEME_EXPANDED").timestamp(300L).build(),
                    SessionInteractionEvent.builder().sessionId(sessionId).schemeCode(schemeCode).eventType("SCHEME_SAVED").timestamp(400L).build(),
                    SessionInteractionEvent.builder().sessionId(sessionId).schemeCode(schemeCode).eventType("APPLICATION_STARTED").timestamp(500L).build(),
                    SessionInteractionEvent.builder().sessionId(sessionId).schemeCode(schemeCode).eventType("SCHEME_APPLIED").timestamp(600L).build(),
                    SessionInteractionEvent.builder().sessionId(sessionId).schemeCode(schemeCode).eventType("APPLICATION_COMPLETED").timestamp(700L).build()
            );

            // Verify every event has required minimum context and continuity
            for (SessionInteractionEvent ev : journeyEvents) {
                assertThat(ev.getSessionId()).isEqualTo(sessionId);
                assertThat(ev.getSchemeCode()).isEqualTo(schemeCode);
                assertThat(ev.getTimestamp()).isGreaterThan(0);
            }

            // Verify terminal state resolves to CONVERTED (Grade 3)
            RelevanceGrade grade = sessionAttributionService.resolveTerminalGrade(journeyEvents);
            assertThat(grade).isEqualTo(RelevanceGrade.CONVERTED);
            assertThat(grade.getGrade()).isEqualTo(3);
        }

        @Test
        @DisplayName("Eligibility Gate Precedence: Ineligible scheme yields Optional.empty()")
        void testEligibilityGatePrecedence() {
            CitizenEligibilityProfile profile = new CitizenEligibilityProfile();
            profile.setAge(25);
            Scheme scheme = createTestScheme("SCH_INELIGIBLE");

            when(eligibilityEngine.evaluate(any(), any())).thenReturn(
                    EligibilityEvaluationResult.builder()
                            .schemeCode("SCH_INELIGIBLE")
                            .status(EligibilityStatus.NOT_ELIGIBLE)
                            .build()
            );

            Optional<TrainingPair> pair = sessionAttributionService.buildTrainingPair(
                    profile, scheme, "sess_1", List.of(), 1, 0.9, "2.2.0-hybrid-semantic-384d"
            );

            assertThat(pair).isEmpty();
        }
    }
}
