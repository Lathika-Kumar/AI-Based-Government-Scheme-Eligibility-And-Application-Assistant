package com.schemebridge.scheme.ml.dataset;

import com.schemebridge.scheme.document.RecommendationEvent;
import com.schemebridge.scheme.document.RecommendationEventType;
import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResult;
import com.schemebridge.scheme.dto.response.EligibilityStatus;
import com.schemebridge.scheme.repository.SchemeRepository;
import com.schemebridge.scheme.service.EligibilityEngine;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Phase 31: Production Telemetry Ingestion & Controlled Processing Boundary.
 * Implements the 11 validation gates and the deterministic 7-way classification
 * for incoming citizen interaction telemetry without mutating production records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionTelemetryIngestionService {

    private final SchemeRepository schemeRepository;
    private final SessionAttributionService sessionAttributionService;
    private final Phase29TelemetryQualityMonitor telemetryQualityMonitor;
    private final EligibilityEngine eligibilityEngine;

    @Getter
    @ToString
    @Builder
    public static class IngestionDecision {
        private final TelemetryQualityStatus status;
        private final String rejectionCategory;
        private final String sessionId;
        private final String schemeCode;
        private final String eventType;
        private final boolean eligible;
        private final boolean passedAllGates;
    }

    /**
     * Ingests and classifies a telemetry event according to the strict 11-gate pipeline.
     * Guaranteed: PII values are NEVER logged.
     */
    public IngestionDecision ingestEvent(
            String userId,
            String sessionId,
            String schemeCode,
            String eventType,
            Instant timestamp,
            Map<String, Object> metadata,
            Set<String> seenEventsInSession,
            Set<String> sessionAnchoredSchemes,
            CitizenEligibilityProfile profile
    ) {
        // Gate 1: Structural & Parameter Validation
        // Gate 2: SessionId Validation
        // Gate 3: SchemeCode Validation
        // Gate 4: EventType Validation
        if (sessionId == null || sessionId.isBlank()
                || schemeCode == null || schemeCode.isBlank()
                || eventType == null || eventType.isBlank()) {
            return buildDecision(TelemetryQualityStatus.MALFORMED, "MISSING_REQUIRED_FIELDS", sessionId, schemeCode, eventType, false, false);
        }

        // Gate 5: Timestamp Validation (positive epoch, not in future > 3600 seconds)
        if (timestamp == null || timestamp.toEpochMilli() <= 0
                || timestamp.isAfter(Instant.now().plusSeconds(3600))) {
            return buildDecision(TelemetryQualityStatus.MALFORMED, "INVALID_TIMESTAMP", sessionId, schemeCode, eventType, false, false);
        }

        // Gate 6: Synthetic Fixture Detection
        if (sessionAttributionService.isSyntheticFixture(userId, sessionId, null)) {
            return buildDecision(TelemetryQualityStatus.SYNTHETIC, "SYNTHETIC_FIXTURE", sessionId, schemeCode, eventType, false, false);
        }

        // Gate 7: Recursive Zero-PII Validation
        if (telemetryQualityMonitor.hasPiiRecursively(metadata)) {
            // Log only safe metadata, NEVER log PII values
            log.warn("PII violation detected in telemetry event for session {}. Event quarantined.", sessionId);
            return buildDecision(TelemetryQualityStatus.PII_VIOLATION, "PII_KEY_DETECTED", sessionId, schemeCode, eventType, false, false);
        }

        // Gate 8: Scheme Existence Check (Master Repository)
        Optional<Scheme> schemeOpt = schemeRepository != null ? schemeRepository.findBySchemeCode(schemeCode) : Optional.empty();
        if (schemeOpt.isEmpty()) {
            return buildDecision(TelemetryQualityStatus.ORPHAN, "SCHEME_NOT_FOUND", sessionId, schemeCode, eventType, false, false);
        }

        // Gate 9: Recommendation Anchor Validation
        // If event is not RECOMMENDATION_SHOWN, the session must have an anchor for this scheme
        boolean isAnchorEvent = "RECOMMENDATION_SHOWN".equalsIgnoreCase(eventType);
        if (isAnchorEvent) {
            if (sessionAnchoredSchemes != null) {
                sessionAnchoredSchemes.add(schemeCode);
            }
        } else {
            if (sessionAnchoredSchemes == null || !sessionAnchoredSchemes.contains(schemeCode)) {
                return buildDecision(TelemetryQualityStatus.ORPHAN, "MISSING_RECOMMENDATION_ANCHOR", sessionId, schemeCode, eventType, false, false);
            }
        }

        // Gate 10: Duplicate Detection
        if (seenEventsInSession != null) {
            String eventKey = sessionId + ":" + schemeCode + ":" + eventType;
            if (seenEventsInSession.contains(eventKey)) {
                return buildDecision(TelemetryQualityStatus.DUPLICATE, "DUPLICATE_IN_SESSION", sessionId, schemeCode, eventType, false, false);
            }
            seenEventsInSession.add(eventKey);
        }

        // Gate 11: Statutory Eligibility Validation (When profile is provided)
        boolean isEligible = true;
        if (profile != null) {
            EligibilityEvaluationResult eval = eligibilityEngine.evaluate(profile, schemeOpt.get());
            isEligible = eval != null && eval.getStatus() == EligibilityStatus.ELIGIBLE;
        }

        return buildDecision(TelemetryQualityStatus.VALID, null, sessionId, schemeCode, eventType, isEligible, isEligible);
    }

    private IngestionDecision buildDecision(
            TelemetryQualityStatus status,
            String rejectionCategory,
            String sessionId,
            String schemeCode,
            String eventType,
            boolean eligible,
            boolean passedAllGates
    ) {
        return IngestionDecision.builder()
                .status(status)
                .rejectionCategory(rejectionCategory)
                .sessionId(sessionId)
                .schemeCode(schemeCode)
                .eventType(eventType)
                .eligible(eligible)
                .passedAllGates(passedAllGates)
                .build();
    }
}
