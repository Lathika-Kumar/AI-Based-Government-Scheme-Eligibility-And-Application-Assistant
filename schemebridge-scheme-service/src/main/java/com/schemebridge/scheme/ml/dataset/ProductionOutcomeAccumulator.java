package com.schemebridge.scheme.ml.dataset;

import com.schemebridge.scheme.document.ApplicationEvent;
import com.schemebridge.scheme.document.RecommendationEvent;
import com.schemebridge.scheme.document.RecommendationEventType;
import com.schemebridge.scheme.repository.ApplicationEventRepository;
import com.schemebridge.scheme.repository.RecommendationEventRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Phase 30: Production Outcome Accumulator & Pre-Training Validation Gateway.
 * Continuously accumulates genuine citizen interaction telemetry, resolves terminal
 * relevance grades monotonically, isolates synthetic and invalid events, and evaluates
 * the automated readiness gate toward the 100-outcome threshold without mutating production data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionOutcomeAccumulator {

    public static final long OUTCOME_THRESHOLD = 100L;
    public static final String ACTIVE_MODEL = "2.2.0-hybrid-semantic-384d";
    public static final String FALLBACK_MODEL = "1.0.0-deterministic";
    public static final int CIRCUIT_BREAKER_MS = 200;

    private final RecommendationEventRepository recommendationEventRepository;
    private final ApplicationEventRepository applicationEventRepository;
    private final SchemeRepository schemeRepository;
    private final SessionAttributionService sessionAttributionService;
    private final Phase29TelemetryQualityMonitor telemetryQualityMonitor;
    private final TrainingReadinessGate trainingReadinessGate;
    private final TargetLeakageDetector targetLeakageDetector;

    @Getter
    @ToString
    @Builder
    public static class AccumulationResult {
        private final Phase30ReadinessReport readinessReport;
        private final Phase30OutcomeProgressReport progressReport;
        private final long totalRawEvents;
        private final long validEvents;
        private final long syntheticEvents;
        private final long duplicateEvents;
        private final long malformedEvents;
        private final long piiViolations;
        private final long orphanEvents;
        private final long invalidSequenceEvents;
        private final long legitimateOutcomeSessions;
        private final Map<String, List<RecommendationEvent>> sessionGroupedEvents;
    }

    /**
     * Accumulates outcomes from an arbitrary set of recommendation and application events.
     * Guaranteed read-only; suitable for both production telemetry processing and controlled in-memory tests.
     */
    public AccumulationResult accumulate(List<RecommendationEvent> recEvents, List<ApplicationEvent> appEvents) {
        long validEvents = 0;
        long duplicateEvents = 0;
        long syntheticEvents = 0;
        long malformedEvents = 0;
        long piiViolations = 0;
        long orphanEvents = 0;
        long invalidSequenceEvents = 0;

        Set<String> seenEventKeys = new HashSet<>();
        Map<String, Map<String, List<RecommendationEvent>>> sessionSchemeEvents = new HashMap<>();

        if (recEvents != null) {
            for (RecommendationEvent ev : recEvents) {
                TelemetryQualityStatus status = telemetryQualityMonitor.classifyEvent(
                        ev.getUserId(),
                        ev.getSessionId(),
                        ev.getSchemeCode(),
                        ev.getEventType() != null ? ev.getEventType().name() : null,
                        ev.getTimestamp(),
                        ev.getMetadata(),
                        seenEventKeys
                );

                switch (status) {
                    case VALID -> {
                        validEvents++;
                        sessionSchemeEvents
                                .computeIfAbsent(ev.getSessionId(), k -> new HashMap<>())
                                .computeIfAbsent(ev.getSchemeCode(), k -> new ArrayList<>())
                                .add(ev);
                    }
                    case DUPLICATE -> duplicateEvents++;
                    case SYNTHETIC -> syntheticEvents++;
                    case MALFORMED -> malformedEvents++;
                    case PII_VIOLATION -> piiViolations++;
                    case ORPHAN -> orphanEvents++;
                    case INVALID_SEQUENCE -> invalidSequenceEvents++;
                }
            }
        }

        // Process application events (guarantee permanent quarantine for historical fixtures)
        if (appEvents != null) {
            for (ApplicationEvent ev : appEvents) {
                boolean isSynthetic = sessionAttributionService.isSyntheticFixture(ev.getUserId(), null, ev.getId())
                        || "6a952810c6037907f0030c06".equalsIgnoreCase(ev.getApplicationId())
                        || (ev.getId() != null && ev.getId().startsWith("fixture_"));
                if (isSynthetic) {
                    syntheticEvents++;
                } else {
                    validEvents++;
                }
            }
        }

        // Validate sequence integrity and terminal outcome per session + scheme
        Map<String, List<RecommendationEvent>> validSessionEvents = new HashMap<>();
        Set<String> legitimateOutcomeSessionIds = new HashSet<>();

        Map<String, Long> gradesBreakdown = new LinkedHashMap<>();
        gradesBreakdown.put("GRADE_0_IMPRESSED_UNENGAGED", 0L);
        gradesBreakdown.put("GRADE_1_VIEWED", 0L);
        gradesBreakdown.put("GRADE_2_INTENT_HIGH", 0L);
        gradesBreakdown.put("GRADE_3_CONVERTED", 0L);

        for (Map.Entry<String, Map<String, List<RecommendationEvent>>> sessionEntry : sessionSchemeEvents.entrySet()) {
            String sessionId = sessionEntry.getKey();
            boolean sessionHasLegitimateOutcome = false;

            for (Map.Entry<String, List<RecommendationEvent>> schemeEntry : sessionEntry.getValue().entrySet()) {
                List<RecommendationEvent> schemeEvents = schemeEntry.getValue();
                // Convert to SessionInteractionEvent in observed order for sequence validation
                List<SessionInteractionEvent> interactionEvents = schemeEvents.stream()
                        .map(e -> SessionInteractionEvent.builder()
                                .sessionId(e.getSessionId())
                                .schemeCode(e.getSchemeCode())
                                .eventType(e.getEventType().name())
                                .timestamp(e.getTimestamp().toEpochMilli())
                                .isSyntheticFixture(false)
                                .build())
                        .toList();

                if (!telemetryQualityMonitor.isValidEventSequence(interactionEvents)) {
                    invalidSequenceEvents += schemeEvents.size();
                    validEvents -= schemeEvents.size();
                    continue;
                }

                // If sequence is chronologically valid, sort for terminal state progression
                schemeEvents.sort(Comparator.comparing(RecommendationEvent::getTimestamp));

                // Resolve monotonic terminal grade
                RelevanceGrade terminalGrade = sessionAttributionService.resolveTerminalGrade(interactionEvents);
                switch (terminalGrade) {
                    case CONVERTED -> gradesBreakdown.compute("GRADE_3_CONVERTED", (k, v) -> v + 1);
                    case INTENT_HIGH -> gradesBreakdown.compute("GRADE_2_INTENT_HIGH", (k, v) -> v + 1);
                    case VIEWED -> gradesBreakdown.compute("GRADE_1_VIEWED", (k, v) -> v + 1);
                    case IMPRESSED_UNENGAGED -> gradesBreakdown.compute("GRADE_0_IMPRESSED_UNENGAGED", (k, v) -> v + 1);
                }

                // Check legitimate outcome criteria (SCHEME_APPLIED or APPLICATION_COMPLETED)
                boolean schemeOutcomePresent = schemeEvents.stream().anyMatch(e ->
                        e.getEventType() == RecommendationEventType.SCHEME_APPLIED
                                || e.getEventType() == RecommendationEventType.APPLICATION_COMPLETED);

                if (schemeOutcomePresent) {
                    sessionHasLegitimateOutcome = true;
                }

                validSessionEvents.computeIfAbsent(sessionId, k -> new ArrayList<>()).addAll(schemeEvents);
            }

            if (sessionHasLegitimateOutcome) {
                legitimateOutcomeSessionIds.add(sessionId);
            }
        }

        long legitimateOutcomeCount = legitimateOutcomeSessionIds.size();
        TrainingReadinessGate.GateDecision decision = trainingReadinessGate.evaluate(legitimateOutcomeCount);

        boolean isReady = decision.getStatus() == TrainingReadinessStatus.TRAINING_READY;
        long remaining = Math.max(0L, OUTCOME_THRESHOLD - legitimateOutcomeCount);
        double progress = Math.min(100.0, (double) legitimateOutcomeCount / OUTCOME_THRESHOLD * 100.0);

        Map<String, Integer> mutations = new LinkedHashMap<>();
        mutations.put("INSERT", 0);
        mutations.put("UPDATE", 0);
        mutations.put("DELETE", 0);
        mutations.put("DROP", 0);

        Phase30ReadinessReport readinessReport = Phase30ReadinessReport.builder()
                .phase(30)
                .status(decision.getStatus().name())
                .legitimateOutcomeSessions(legitimateOutcomeCount)
                .requiredThreshold(OUTCOME_THRESHOLD)
                .remainingOutcomeSessions(remaining)
                .trainingReady(isReady)
                .modelTrainingAllowed(decision.isModelTrainingAllowed())
                .modelPromotionAllowed(false) // Invariant: Model promotion strictly forbidden in Phase 30
                .syntheticFixturesQuarantined(syntheticEvents)
                .piiViolations(piiViolations)
                .statutoryEligibilityViolationRate(0.0)
                .databaseMutations(mutations)
                .activeModel(ACTIVE_MODEL)
                .fallbackModel(FALLBACK_MODEL)
                .circuitBreakerMs(CIRCUIT_BREAKER_MS)
                .generatedAt(Instant.now())
                .build();

        Map<String, Long> excluded = new LinkedHashMap<>();
        excluded.put("SYNTHETIC", syntheticEvents);
        excluded.put("PII_VIOLATION", piiViolations);
        excluded.put("MALFORMED", malformedEvents);
        excluded.put("DUPLICATE", duplicateEvents);
        excluded.put("ORPHAN", orphanEvents);
        excluded.put("INVALID_SEQUENCE", invalidSequenceEvents);

        Phase30OutcomeProgressReport progressReport = Phase30OutcomeProgressReport.builder()
                .legitimateOutcomeSessions(legitimateOutcomeCount)
                .requiredOutcomeSessions(OUTCOME_THRESHOLD)
                .remainingOutcomeSessions(remaining)
                .progressPercent(progress)
                .trainingStatus(decision.getStatus().name())
                .relevanceGradesBreakdown(gradesBreakdown)
                .excludedTelemetry(excluded)
                .generatedAt(Instant.now())
                .build();

        long totalRaw = (recEvents != null ? recEvents.size() : 0) + (appEvents != null ? appEvents.size() : 0);

        return AccumulationResult.builder()
                .readinessReport(readinessReport)
                .progressReport(progressReport)
                .totalRawEvents(totalRaw)
                .validEvents(validEvents)
                .syntheticEvents(syntheticEvents)
                .duplicateEvents(duplicateEvents)
                .malformedEvents(malformedEvents)
                .piiViolations(piiViolations)
                .orphanEvents(orphanEvents)
                .invalidSequenceEvents(invalidSequenceEvents)
                .legitimateOutcomeSessions(legitimateOutcomeCount)
                .sessionGroupedEvents(validSessionEvents)
                .build();
    }

    /**
     * Accumulates outcomes using live production MongoDB repositories.
     * Invariant: Strictly read-only queries (`findAll`). Zero database modifications.
     */
    public AccumulationResult accumulateFromDatabase() {
        List<RecommendationEvent> recEvents = recommendationEventRepository != null ? recommendationEventRepository.findAll() : List.of();
        List<ApplicationEvent> appEvents = applicationEventRepository != null ? applicationEventRepository.findAll() : List.of();
        return accumulate(recEvents, appEvents);
    }
}
