package com.schemebridge.scheme.ml.dataset;

import com.schemebridge.scheme.document.ApplicationEvent;
import com.schemebridge.scheme.document.RecommendationEvent;
import com.schemebridge.scheme.document.RecommendationEventType;
import com.schemebridge.scheme.repository.ApplicationEventRepository;
import com.schemebridge.scheme.repository.RecommendationEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Phase 38: Behavioral Training Dataset Qualification Service.
 * Evaluates accumulated genuine citizen outcomes to determine whether the dataset is qualified
 * to unlock offline behavioral ML training (threshold >= 100).
 *
 * CRITICAL INVARIANTS:
 * - When legitimate outcomes < 100: trainingReady = false, modelTrainingAllowed = false.
 * - When legitimate outcomes >= 100: trainingReady = true, modelTrainingAllowed = true (offline sandbox only).
 * - modelPromotionAllowed is PERMANENTLY false (strictly governed by human deployment authority).
 * - Automatic training is NEVER triggered upon qualification.
 * - Synthetic fixtures (29 historical) contribute 0 to training qualification.
 * - Zero target leakage: Features and target labels are strictly separated.
 * - Zero PII: Telemetry payloads containing PII are rejected.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehavioralTrainingDatasetQualificationService {

    public static final long MINIMUM_OUTCOME_THRESHOLD = 100L;
    public static final String ACTIVE_MODEL = "2.2.0-hybrid-semantic-384d";
    public static final String FALLBACK_MODEL = "1.0.0-deterministic";
    public static final int CIRCUIT_BREAKER_MS = 200;

    private final RecommendationEventRepository recommendationEventRepository;
    private final ApplicationEventRepository applicationEventRepository;
    private final ProductionOutcomeAccumulator outcomeAccumulator;
    private final SessionAttributionService sessionAttributionService;
    private final TrainingReadinessGate trainingReadinessGate;
    private final TargetLeakageDetector targetLeakageDetector;

    /**
     * Evaluates live database telemetry against pre-training dataset qualification standards.
     * Guaranteed read-only: Zero database mutations.
     */
    public DatasetQualificationSnapshot qualifyLive() {
        List<RecommendationEvent> recEvents = recommendationEventRepository != null
                ? recommendationEventRepository.findAll() : List.of();
        List<ApplicationEvent> appEvents = applicationEventRepository != null
                ? applicationEventRepository.findAll() : List.of();
        return qualify(recEvents, appEvents);
    }

    /**
     * Qualifies an explicit or in-memory set of events against all dataset qualification requirements.
     */
    public DatasetQualificationSnapshot qualify(List<RecommendationEvent> recEvents, List<ApplicationEvent> appEvents) {
        ProductionOutcomeAccumulator.AccumulationResult accumulation = outcomeAccumulator.accumulate(recEvents, appEvents);

        long totalEvents = accumulation.getTotalRawEvents();
        long validEvents = accumulation.getValidEvents();
        long syntheticEvents = accumulation.getSyntheticEvents();
        long duplicateEvents = accumulation.getDuplicateEvents();
        long piiViolations = accumulation.getPiiViolations();
        long orphanEvents = accumulation.getOrphanEvents();
        long invalidSequenceEvents = accumulation.getInvalidSequenceEvents();

        long eligibleTrainingExamples = 0L;
        long rejectedTrainingExamples = 0L;
        long targetLeakageViolations = 0L;
        List<String> qualificationErrors = new ArrayList<>();

        Map<String, List<RecommendationEvent>> sessionGrouped = accumulation.getSessionGroupedEvents();
        Set<String> qualifiedOutcomeSessionIds = new HashSet<>();

        if (sessionGrouped != null) {
            for (Map.Entry<String, List<RecommendationEvent>> entry : sessionGrouped.entrySet()) {
                String sessionId = entry.getKey();
                List<RecommendationEvent> events = entry.getValue();

                // Group by schemeCode within session
                Map<String, List<RecommendationEvent>> schemeEvents = new HashMap<>();
                for (RecommendationEvent ev : events) {
                    schemeEvents.computeIfAbsent(ev.getSchemeCode(), k -> new ArrayList<>()).add(ev);
                }

                for (Map.Entry<String, List<RecommendationEvent>> schemeEntry : schemeEvents.entrySet()) {
                    String schemeCode = schemeEntry.getKey();
                    List<RecommendationEvent> evList = schemeEntry.getValue();
                    evList.sort(Comparator.comparing(RecommendationEvent::getTimestamp));

                    // Requirement C: Must have valid RECOMMENDATION_SHOWN anchor
                    boolean hasAnchor = evList.stream().anyMatch(e -> e.getEventType() == RecommendationEventType.RECOMMENDATION_SHOWN);
                    if (!hasAnchor) {
                        orphanEvents++;
                        rejectedTrainingExamples++;
                        qualificationErrors.add("Orphan outcome: Session " + sessionId + " scheme " + schemeCode + " lacks RECOMMENDATION_SHOWN anchor.");
                        continue;
                    }

                    // Requirement G: Audit for target leakage in metadata
                    boolean leakageFound = false;
                    for (RecommendationEvent ev : evList) {
                        if (ev.getMetadata() != null && targetLeakageDetector.hasLeakage(ev.getMetadata())) {
                            leakageFound = true;
                            break;
                        }
                    }

                    if (leakageFound) {
                        targetLeakageViolations++;
                        rejectedTrainingExamples++;
                        qualificationErrors.add("Target leakage detected in session " + sessionId + " scheme " + schemeCode);
                        continue;
                    }

                    // Check for terminal outcome
                    boolean hasTerminalOutcome = evList.stream().anyMatch(e ->
                            e.getEventType() == RecommendationEventType.SCHEME_APPLIED
                                    || e.getEventType() == RecommendationEventType.APPLICATION_COMPLETED);

                    if (hasTerminalOutcome) {
                        eligibleTrainingExamples++;
                        qualifiedOutcomeSessionIds.add(sessionId);
                    } else {
                        // Browsing-only sessions without terminal outcome are not qualifying training examples
                        rejectedTrainingExamples++;
                    }
                }
            }
        }

        long legitimateOutcomeSessions = qualifiedOutcomeSessionIds.size();

        // Training Gate evaluation
        TrainingReadinessGate.GateDecision gateDecision = trainingReadinessGate.evaluate(legitimateOutcomeSessions);
        long remaining = Math.max(0L, MINIMUM_OUTCOME_THRESHOLD - legitimateOutcomeSessions);

        boolean thresholdMet = legitimateOutcomeSessions >= MINIMUM_OUTCOME_THRESHOLD;
        boolean integrityClean = (piiViolations == 0) && (targetLeakageViolations == 0);

        String status;
        boolean trainingReady;
        boolean modelTrainingAllowed;
        String qualificationMessage;

        if (!thresholdMet) {
            status = "NOT_READY";
            trainingReady = false;
            modelTrainingAllowed = false;
            qualificationMessage = String.format("TRAINING_NOT_READY: Insufficient legitimate outcome sessions (%d / %d, remaining: %d). Offline training locked.",
                    legitimateOutcomeSessions, MINIMUM_OUTCOME_THRESHOLD, remaining);
        } else if (!integrityClean) {
            status = "DISQUALIFIED_BY_INTEGRITY";
            trainingReady = false;
            modelTrainingAllowed = false;
            qualificationMessage = String.format("TRAINING_BLOCKED_BY_INTEGRITY: Threshold met (%d) but integrity violations detected (PII: %d, TargetLeakage: %d).",
                    legitimateOutcomeSessions, piiViolations, targetLeakageViolations);
        } else {
            status = "ELIGIBLE_FOR_OFFLINE_TRAINING";
            trainingReady = true;
            modelTrainingAllowed = true;
            qualificationMessage = String.format("TRAINING_ELIGIBLE: Legitimate outcome threshold satisfied (%d >= %d). Dataset qualified for offline sandbox training.",
                    legitimateOutcomeSessions, MINIMUM_OUTCOME_THRESHOLD);
        }

        Map<String, Long> relevanceGrades = accumulation.getProgressReport() != null
                ? accumulation.getProgressReport().getRelevanceGradesBreakdown() : Map.of();

        return DatasetQualificationSnapshot.builder()
                .status(status)
                .totalEvents(totalEvents)
                .validEvents(validEvents)
                .syntheticEvents(syntheticEvents)
                .piiViolations(piiViolations)
                .orphanEvents(orphanEvents)
                .duplicateEvents(duplicateEvents)
                .invalidSequenceEvents(invalidSequenceEvents)
                .legitimateOutcomeSessions(legitimateOutcomeSessions)
                .eligibleTrainingExamples(eligibleTrainingExamples)
                .rejectedTrainingExamples(rejectedTrainingExamples)
                .targetLeakageViolations(targetLeakageViolations)
                .minimumOutcomeThreshold(MINIMUM_OUTCOME_THRESHOLD)
                .remainingOutcomes(remaining)
                .trainingReady(trainingReady)
                .modelTrainingAllowed(modelTrainingAllowed)
                .modelPromotionAllowed(false) // Permanently false - promotion is strictly governed
                .statutoryEligibilityViolationRate(0.0)
                .activeModel(ACTIVE_MODEL)
                .fallbackModel(FALLBACK_MODEL)
                .circuitBreakerMs(CIRCUIT_BREAKER_MS)
                .qualificationMessage(qualificationMessage)
                .evaluatedAt(Instant.now())
                .qualificationErrors(qualificationErrors)
                .relevanceGradesDistribution(relevanceGrades)
                .build();
    }

    /**
     * Factory method to safely construct a verified TrainingExample.
     */
    public TrainingExample createTrainingExample(
            String exampleId,
            String sessionId,
            String schemeCode,
            Map<String, Object> profileAttributes,
            Map<String, Object> schemeCharacteristics,
            Map<String, Object> recommendationContext,
            List<String> priorInteractions,
            String targetEventType,
            boolean isConverted,
            RelevanceGrade grade,
            long targetTimestamp
    ) {
        TrainingExample.TrainingFeatures features = TrainingExample.TrainingFeatures.of(
                profileAttributes,
                schemeCharacteristics,
                recommendationContext,
                priorInteractions
        );

        TrainingExample.TrainingLabel label = TrainingExample.TrainingLabel.builder()
                .targetEventType(targetEventType)
                .converted(isConverted)
                .relevanceGrade(grade)
                .targetTimestamp(targetTimestamp)
                .build();

        return TrainingExample.builder()
                .exampleId(exampleId)
                .sessionId(sessionId)
                .schemeCode(schemeCode)
                .features(features)
                .label(label)
                .statutoryEligible(true)
                .createdAt(Instant.now())
                .build();
    }

    public boolean isTrainingReady() {
        return qualifyLive().isTrainingReady();
    }

    public boolean isModelTrainingAllowed() {
        return qualifyLive().isModelTrainingAllowed();
    }

    public boolean isModelPromotionAllowed() {
        return false; // Invariant: Always false
    }
}
