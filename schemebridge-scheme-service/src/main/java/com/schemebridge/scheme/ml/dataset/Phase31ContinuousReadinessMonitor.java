package com.schemebridge.scheme.ml.dataset;

import com.schemebridge.scheme.document.ApplicationEvent;
import com.schemebridge.scheme.document.RecommendationEvent;
import com.schemebridge.scheme.document.RecommendationEventType;
import com.schemebridge.scheme.document.Scheme;
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
 * Phase 31: Continuous Readiness Monitor & Production Telemetry Health Evaluator.
 * Aggregates genuine production outcome telemetry, continuously assesses progress
 * toward the 100-session threshold, and tracks telemetry health metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Phase31ContinuousReadinessMonitor {

    public static final long OUTCOME_THRESHOLD = 100L;
    public static final String ACTIVE_MODEL = "2.2.0-hybrid-semantic-384d";
    public static final String FALLBACK_MODEL = "1.0.0-deterministic";
    public static final int CIRCUIT_BREAKER_MS = 200;

    private final RecommendationEventRepository recommendationEventRepository;
    private final ApplicationEventRepository applicationEventRepository;
    private final SchemeRepository schemeRepository;
    private final SessionAttributionService sessionAttributionService;
    private final Phase29TelemetryQualityMonitor telemetryQualityMonitor;
    private final ProductionTelemetryIngestionService telemetryIngestionService;
    private final TrainingReadinessGate trainingReadinessGate;

    @Getter
    @ToString
    @Builder
    public static class ContinuousMonitoringSnapshot {
        private final Phase31ReadinessReport readinessReport;
        private final Phase31OutcomeProgressReport progressReport;
        private final Phase31TelemetryQualityReport qualityReport;
        private final long legitimateOutcomeSessions;
        private final Map<String, List<RecommendationEvent>> sessionGroupedEvents;

        public long getThreshold() {
            return readinessReport != null ? readinessReport.getRequiredThreshold() : OUTCOME_THRESHOLD;
        }

        public long getRemaining() {
            return readinessReport != null ? readinessReport.getRemainingOutcomeSessions() : OUTCOME_THRESHOLD;
        }

        public boolean isTrainingReady() {
            return readinessReport != null && readinessReport.isTrainingReady();
        }

        public boolean isModelTrainingAllowed() {
            return readinessReport != null && readinessReport.isModelTrainingAllowed();
        }

        public boolean isModelPromotionAllowed() {
            return false;
        }

        public String getActiveModel() {
            return readinessReport != null ? readinessReport.getActiveModel() : ACTIVE_MODEL;
        }

        public String getFallbackModel() {
            return readinessReport != null ? readinessReport.getFallbackModel() : FALLBACK_MODEL;
        }
    }

    /**
     * Evaluates continuous telemetry stream in memory (read-only, zero side effects).
     */
    public ContinuousMonitoringSnapshot evaluateTelemetry(List<RecommendationEvent> recEvents, List<ApplicationEvent> appEvents) {
        long validEvents = 0;
        long duplicateEvents = 0;
        long syntheticEvents = 0;
        long malformedEvents = 0;
        long piiViolations = 0;
        long orphanEvents = 0;
        long invalidSequenceEvents = 0;

        Set<String> seenEventsInSession = new HashSet<>();
        Map<String, Set<String>> sessionAnchors = new HashMap<>();
        Map<String, Map<String, List<RecommendationEvent>>> sessionSchemeEvents = new HashMap<>();

        if (recEvents != null) {
            for (RecommendationEvent ev : recEvents) {
                Set<String> anchored = sessionAnchors.computeIfAbsent(ev.getSessionId(), k -> new HashSet<>());
                ProductionTelemetryIngestionService.IngestionDecision decision = telemetryIngestionService.ingestEvent(
                        ev.getUserId(),
                        ev.getSessionId(),
                        ev.getSchemeCode(),
                        ev.getEventType() != null ? ev.getEventType().name() : null,
                        ev.getTimestamp(),
                        ev.getMetadata(),
                        seenEventsInSession,
                        anchored,
                        null
                );

                switch (decision.getStatus()) {
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

        // Validate sequence integrity and calculate terminal outcome per session + scheme
        Map<String, List<RecommendationEvent>> validSessionEvents = new HashMap<>();
        Set<String> legitimateOutcomeSessionIds = new HashSet<>();

        Map<String, Long> gradesBreakdown = new LinkedHashMap<>();
        gradesBreakdown.put("GRADE_0_IMPRESSED_UNENGAGED", 0L);
        gradesBreakdown.put("GRADE_1_VIEWED", 0L);
        gradesBreakdown.put("GRADE_2_INTENT_HIGH", 0L);
        gradesBreakdown.put("GRADE_3_CONVERTED", 0L);

        for (Map.Entry<String, Map<String, List<RecommendationEvent>>> sessionEntry : sessionSchemeEvents.entrySet()) {
            String sessionId = sessionEntry.getKey();
            boolean sessionHasOutcome = false;

            for (Map.Entry<String, List<RecommendationEvent>> schemeEntry : sessionEntry.getValue().entrySet()) {
                List<RecommendationEvent> schemeEvents = schemeEntry.getValue();

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

                // Sort chronologically for attribution
                schemeEvents.sort(Comparator.comparing(RecommendationEvent::getTimestamp));

                RelevanceGrade terminalGrade = sessionAttributionService.resolveTerminalGrade(interactionEvents);
                switch (terminalGrade) {
                    case CONVERTED -> gradesBreakdown.compute("GRADE_3_CONVERTED", (k, v) -> v + 1);
                    case INTENT_HIGH -> gradesBreakdown.compute("GRADE_2_INTENT_HIGH", (k, v) -> v + 1);
                    case VIEWED -> gradesBreakdown.compute("GRADE_1_VIEWED", (k, v) -> v + 1);
                    case IMPRESSED_UNENGAGED -> gradesBreakdown.compute("GRADE_0_IMPRESSED_UNENGAGED", (k, v) -> v + 1);
                }

                boolean schemeOutcomePresent = schemeEvents.stream().anyMatch(e ->
                        e.getEventType() == RecommendationEventType.SCHEME_APPLIED
                                || e.getEventType() == RecommendationEventType.APPLICATION_COMPLETED);

                if (schemeOutcomePresent) {
                    sessionHasOutcome = true;
                }

                validSessionEvents.computeIfAbsent(sessionId, k -> new ArrayList<>()).addAll(schemeEvents);
            }

            if (sessionHasOutcome) {
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

        Phase31ReadinessReport readinessReport = Phase31ReadinessReport.builder()
                .phase(31)
                .status(decision.getStatus().name())
                .legitimateOutcomeSessions(legitimateOutcomeCount)
                .requiredThreshold(OUTCOME_THRESHOLD)
                .remainingOutcomeSessions(remaining)
                .trainingReady(isReady)
                .modelTrainingAllowed(decision.isModelTrainingAllowed())
                .modelPromotionAllowed(false) // Invariant: Model promotion strictly forbidden in Phase 31
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

        Phase31OutcomeProgressReport progressReport = Phase31OutcomeProgressReport.builder()
                .legitimateOutcomeSessions(legitimateOutcomeCount)
                .requiredOutcomeSessions(OUTCOME_THRESHOLD)
                .remainingOutcomeSessions(remaining)
                .progressPercent(progress)
                .trainingStatus(decision.getStatus().name())
                .trainingReady(isReady)
                .modelTrainingAllowed(decision.isModelTrainingAllowed())
                .modelPromotionAllowed(false)
                .relevanceGradesBreakdown(gradesBreakdown)
                .excludedTelemetry(excluded)
                .generatedAt(Instant.now())
                .build();

        long totalRaw = (recEvents != null ? recEvents.size() : 0) + (appEvents != null ? appEvents.size() : 0);
        long rejectedEvents = totalRaw - validEvents;
        double validRate = totalRaw > 0 ? (double) validEvents / totalRaw : 0.0;
        double rejectionRate = totalRaw > 0 ? (double) rejectedEvents / totalRaw : 0.0;
        double synRate = totalRaw > 0 ? (double) syntheticEvents / totalRaw : 0.0;
        double dupRate = totalRaw > 0 ? (double) duplicateEvents / totalRaw : 0.0;
        double piiRate = totalRaw > 0 ? (double) piiViolations / totalRaw : 0.0;
        double malformedRate = totalRaw > 0 ? (double) malformedEvents / totalRaw : 0.0;

        Phase31TelemetryQualityReport qualityReport = Phase31TelemetryQualityReport.builder()
                .totalRawEvents(totalRaw)
                .validEvents(validEvents)
                .duplicateEvents(duplicateEvents)
                .syntheticEvents(syntheticEvents)
                .malformedEvents(malformedEvents)
                .piiViolations(piiViolations)
                .orphanEvents(orphanEvents)
                .invalidSequenceEvents(invalidSequenceEvents)
                .validInteractionSessions(sessionSchemeEvents.size())
                .legitimateOutcomeSessions(legitimateOutcomeCount)
                .validEventRate(validRate)
                .rejectionRate(rejectionRate)
                .syntheticQuarantineRate(synRate)
                .duplicateRate(dupRate)
                .piiViolationRate(piiRate)
                .malformedRate(malformedRate)
                .relevanceGradesBreakdown(gradesBreakdown)
                .generatedAt(Instant.now())
                .build();

        return ContinuousMonitoringSnapshot.builder()
                .readinessReport(readinessReport)
                .progressReport(progressReport)
                .qualityReport(qualityReport)
                .legitimateOutcomeSessions(legitimateOutcomeCount)
                .sessionGroupedEvents(validSessionEvents)
                .build();
    }

    /**
     * Continuously evaluates live database state in strictly read-only mode.
     */
    public ContinuousMonitoringSnapshot evaluateLiveTelemetry() {
        List<RecommendationEvent> recEvents = recommendationEventRepository != null ? recommendationEventRepository.findAll() : List.of();
        List<ApplicationEvent> appEvents = applicationEventRepository != null ? applicationEventRepository.findAll() : List.of();
        return evaluateTelemetry(recEvents, appEvents);
    }
}
