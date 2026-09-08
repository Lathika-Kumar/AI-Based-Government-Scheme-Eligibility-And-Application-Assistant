package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.document.SchemeLevel;
import com.schemebridge.scheme.ml.dataset.*;
import com.schemebridge.scheme.ml.feature.FeatureVectorBuilder;
import com.schemebridge.scheme.ml.feature.SchemeFeatureExtractor;
import com.schemebridge.scheme.ml.feature.UserFeatureExtractor;
import com.schemebridge.scheme.ml.feature.UserSchemeComparisonService;
import com.schemebridge.scheme.ml.feature.UserSchemeFeatureVector;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Phase29TelemetryQualityTest {

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
    private TrainingReadinessGate trainingReadinessGate;
    private TargetLeakageDetector targetLeakageDetector;
    private Phase29TelemetryQualityMonitor qualityMonitor;

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
        trainingReadinessGate = new TrainingReadinessGate();
        targetLeakageDetector = new TargetLeakageDetector();
        qualityMonitor = new Phase29TelemetryQualityMonitor(
                recommendationEventRepository,
                applicationEventRepository,
                schemeRepository,
                sessionAttributionService,
                trainingReadinessGate,
                targetLeakageDetector
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
    @DisplayName("Telemetry Classification & Hygiene Tests")
    class ClassificationTests {

        @Test
        @DisplayName("1. Valid genuine telemetry event receives VALID status")
        void testValidEventClassified() {
            String schemeCode = "SCH_VALID_01";
            when(schemeRepository.findBySchemeCode(schemeCode)).thenReturn(Optional.of(createTestScheme(schemeCode)));

            TelemetryQualityStatus status = qualityMonitor.classifyEvent(
                    "usr_real_01",
                    "sess_valid_01",
                    schemeCode,
                    "RECOMMENDATION_SHOWN",
                    Instant.now(),
                    Map.of("surface", "recommendations_feed"),
                    new HashSet<>()
            );

            assertThat(status).isEqualTo(TelemetryQualityStatus.VALID);
        }

        @Test
        @DisplayName("2. Synthetic user or fixture event receives SYNTHETIC status")
        void testSyntheticEventClassified() {
            TelemetryQualityStatus s1 = qualityMonitor.classifyEvent(
                    "citizen_user",
                    "sess_001",
                    "SCH_001",
                    "RECOMMENDATION_SHOWN",
                    Instant.now(),
                    Collections.emptyMap(),
                    new HashSet<>()
            );
            TelemetryQualityStatus s2 = qualityMonitor.classifyEvent(
                    "citizen1",
                    "sess_002",
                    "SCH_001",
                    "RECOMMENDATION_SHOWN",
                    Instant.now(),
                    Collections.emptyMap(),
                    new HashSet<>()
            );

            assertThat(s1).isEqualTo(TelemetryQualityStatus.SYNTHETIC);
            assertThat(s2).isEqualTo(TelemetryQualityStatus.SYNTHETIC);
        }

        @Test
        @DisplayName("3. Missing sessionId receives MALFORMED status")
        void testMissingSessionIdMalformed() {
            TelemetryQualityStatus status = qualityMonitor.classifyEvent(
                    "usr_real_01",
                    null,
                    "SCH_001",
                    "RECOMMENDATION_SHOWN",
                    Instant.now(),
                    Collections.emptyMap(),
                    new HashSet<>()
            );
            assertThat(status).isEqualTo(TelemetryQualityStatus.MALFORMED);
        }

        @Test
        @DisplayName("4. Missing schemeCode receives MALFORMED status")
        void testMissingSchemeCodeMalformed() {
            TelemetryQualityStatus status = qualityMonitor.classifyEvent(
                    "usr_real_01",
                    "sess_001",
                    "",
                    "RECOMMENDATION_SHOWN",
                    Instant.now(),
                    Collections.emptyMap(),
                    new HashSet<>()
            );
            assertThat(status).isEqualTo(TelemetryQualityStatus.MALFORMED);
        }

        @Test
        @DisplayName("5. Invalid eventType or timestamp receives MALFORMED status")
        void testInvalidEventOrTimestampMalformed() {
            TelemetryQualityStatus s1 = qualityMonitor.classifyEvent(
                    "usr_real_01",
                    "sess_001",
                    "SCH_001",
                    null,
                    Instant.now(),
                    Collections.emptyMap(),
                    new HashSet<>()
            );
            TelemetryQualityStatus s2 = qualityMonitor.classifyEvent(
                    "usr_real_01",
                    "sess_001",
                    "SCH_001",
                    "RECOMMENDATION_SHOWN",
                    Instant.ofEpochMilli(-100),
                    Collections.emptyMap(),
                    new HashSet<>()
            );
            TelemetryQualityStatus s3 = qualityMonitor.classifyEvent(
                    "usr_real_01",
                    "sess_001",
                    "SCH_001",
                    "RECOMMENDATION_SHOWN",
                    Instant.now().plusSeconds(7200), // In the distant future
                    Collections.emptyMap(),
                    new HashSet<>()
            );

            assertThat(s1).isEqualTo(TelemetryQualityStatus.MALFORMED);
            assertThat(s2).isEqualTo(TelemetryQualityStatus.MALFORMED);
            assertThat(s3).isEqualTo(TelemetryQualityStatus.MALFORMED);
        }

        @Test
        @DisplayName("6. PII metadata receives PII_VIOLATION status")
        void testPiiMetadataDetected() {
            Map<String, Object> dirty = new HashMap<>();
            dirty.put("aadhaar", "123456789012");
            dirty.put("surface", "feed");

            TelemetryQualityStatus status = qualityMonitor.classifyEvent(
                    "usr_real_01",
                    "sess_001",
                    "SCH_001",
                    "RECOMMENDATION_SHOWN",
                    Instant.now(),
                    dirty,
                    new HashSet<>()
            );
            assertThat(status).isEqualTo(TelemetryQualityStatus.PII_VIOLATION);
        }

        @Test
        @DisplayName("7. Nested PII metadata receives PII_VIOLATION status")
        void testNestedPiiDetected() {
            Map<String, Object> nested = Map.of("phone", "9876543210");
            Map<String, Object> dirty = Map.of("userMeta", nested, "surface", "feed");

            TelemetryQualityStatus status = qualityMonitor.classifyEvent(
                    "usr_real_01",
                    "sess_001",
                    "SCH_001",
                    "RECOMMENDATION_SHOWN",
                    Instant.now(),
                    dirty,
                    new HashSet<>()
            );
            assertThat(status).isEqualTo(TelemetryQualityStatus.PII_VIOLATION);
        }

        @Test
        @DisplayName("8. Orphan event with non-existent scheme receives ORPHAN status")
        void testOrphanEventDetected() {
            String nonExistentScheme = "SCH_NON_EXISTENT_999";
            when(schemeRepository.findBySchemeCode(nonExistentScheme)).thenReturn(Optional.empty());

            TelemetryQualityStatus status = qualityMonitor.classifyEvent(
                    "usr_real_01",
                    "sess_001",
                    nonExistentScheme,
                    "RECOMMENDATION_SHOWN",
                    Instant.now(),
                    Collections.emptyMap(),
                    new HashSet<>()
            );
            assertThat(status).isEqualTo(TelemetryQualityStatus.ORPHAN);
        }

        @Test
        @DisplayName("9. Repeated identical event within session receives DUPLICATE status")
        void testDuplicateEventDetected() {
            String schemeCode = "SCH_001";
            when(schemeRepository.findBySchemeCode(schemeCode)).thenReturn(Optional.of(createTestScheme(schemeCode)));

            Set<String> seen = new HashSet<>();

            TelemetryQualityStatus s1 = qualityMonitor.classifyEvent(
                    "usr_real_01",
                    "sess_001",
                    schemeCode,
                    "RECOMMENDATION_SHOWN",
                    Instant.now(),
                    Collections.emptyMap(),
                    seen
            );
            TelemetryQualityStatus s2 = qualityMonitor.classifyEvent(
                    "usr_real_01",
                    "sess_001",
                    schemeCode,
                    "RECOMMENDATION_SHOWN",
                    Instant.now(),
                    Collections.emptyMap(),
                    seen
            );

            assertThat(s1).isEqualTo(TelemetryQualityStatus.VALID);
            assertThat(s2).isEqualTo(TelemetryQualityStatus.DUPLICATE);
        }
    }

    @Nested
    @DisplayName("Chronological Event Sequence & Monotonicity Tests")
    class SequenceAndMonotonicityTests {

        @Test
        @DisplayName("1. Valid progressive chronological sequence passes")
        void testValidChronologicalSequence() {
            List<SessionInteractionEvent> sequence = List.of(
                    SessionInteractionEvent.builder().eventType("RECOMMENDATION_SHOWN").timestamp(1000L).build(),
                    SessionInteractionEvent.builder().eventType("SCHEME_VIEWED").timestamp(1050L).build(),
                    SessionInteractionEvent.builder().eventType("APPLICATION_STARTED").timestamp(1100L).build(),
                    SessionInteractionEvent.builder().eventType("APPLICATION_COMPLETED").timestamp(1200L).build()
            );

            boolean valid = qualityMonitor.isValidEventSequence(sequence);
            assertThat(valid).isTrue();

            RelevanceGrade terminal = sessionAttributionService.resolveTerminalGrade(sequence);
            assertThat(terminal).isEqualTo(RelevanceGrade.CONVERTED);
            assertThat(terminal.getGrade()).isEqualTo(3);
        }

        @Test
        @DisplayName("2. Chronologically reversed sequence is rejected")
        void testReversedSequenceRejected() {
            List<SessionInteractionEvent> sequence = List.of(
                    SessionInteractionEvent.builder().eventType("RECOMMENDATION_SHOWN").timestamp(2000L).build(),
                    SessionInteractionEvent.builder().eventType("SCHEME_VIEWED").timestamp(1000L).build() // time travel backwards
            );

            boolean valid = qualityMonitor.isValidEventSequence(sequence);
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("3. Terminal grade is monotonic and never regresses on subsequent lower events")
        void testTerminalGradeMonotonicity() {
            List<SessionInteractionEvent> sequence = List.of(
                    SessionInteractionEvent.builder().eventType("APPLICATION_COMPLETED").timestamp(1000L).build(),
                    SessionInteractionEvent.builder().eventType("RECOMMENDATION_SHOWN").timestamp(2000L).build(),
                    SessionInteractionEvent.builder().eventType("SCHEME_VIEWED").timestamp(3000L).build()
            );

            RelevanceGrade terminal = sessionAttributionService.resolveTerminalGrade(sequence);
            assertThat(terminal).isEqualTo(RelevanceGrade.CONVERTED);
            assertThat(terminal.getGrade()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Target / Label Leakage Audit Tests")
    class TargetLeakageTests {

        @Test
        @DisplayName("1. Clean feature vector with rank and score passes leakage audit")
        void testCleanFeatureVectorPassesAudit() {
            UserSchemeFeatureVector clean = featureVectorBuilder.buildVector(
                    null, null, null, 1, 0.95, "2.2.0-hybrid-semantic-384d"
            );
            // Should execute without exception
            targetLeakageDetector.audit(clean);
        }

        @Test
        @DisplayName("2. Feature vector containing target relevanceGrade throws IllegalStateException")
        void testFeatureVectorWithRelevanceGradeThrows() {
            Map<String, Object> leakedContext = new HashMap<>();
            leakedContext.put("currentRank", 1);
            leakedContext.put("relevanceGrade", 3);

            UserSchemeFeatureVector dirty = UserSchemeFeatureVector.builder()
                    .schemaVersion("1.0.0")
                    .rankingContext(leakedContext)
                    .build();

            assertThatThrownBy(() -> targetLeakageDetector.audit(dirty))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CRITICAL TARGET LEAKAGE DETECTED");
        }

        @Test
        @DisplayName("3. Feature vector containing applicationOutcome throws IllegalStateException")
        void testFeatureVectorWithOutcomeThrows() {
            Map<String, Object> leakedContext = new HashMap<>();
            leakedContext.put("applicationOutcome", "APPROVED");

            UserSchemeFeatureVector dirty = UserSchemeFeatureVector.builder()
                    .schemaVersion("1.0.0")
                    .rankingContext(leakedContext)
                    .build();

            assertThatThrownBy(() -> targetLeakageDetector.audit(dirty))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CRITICAL TARGET LEAKAGE DETECTED");
        }
    }
}
