package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.request.RecommendationEventRequest;
import com.schemebridge.scheme.dto.response.RecommendationEventMetricsResponse;
import com.schemebridge.scheme.dto.response.RecommendationEventResponse;
import com.schemebridge.scheme.ml.dataset.*;
import com.schemebridge.scheme.service.RecommendationEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for recording and querying genuine citizen recommendation interaction events.
 */
@RestController
@RequestMapping("/api/recommendations/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Recommendation Event Tracking", description = "Endpoints for logging citizen scheme interactions for Learning-to-Rank")
public class RecommendationEventController {

    private final RecommendationEventService eventService;
    private final Phase31ContinuousReadinessMonitor readinessMonitor;
    private final Phase34TrainingReadinessValidator phase34ReadinessValidator;
    private final Phase34ModelTrainingService phase34TrainingService;
    private final Phase34ModelEvaluationService phase34EvaluationService;

    @PostMapping
    @Operation(summary = "Record genuine citizen interaction event on a scheme", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<RecommendationEventResponse> recordEvent(
            @Valid @RequestBody RecommendationEventRequest request
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUserId = (auth != null && auth.getName() != null && !"anonymousUser".equalsIgnoreCase(auth.getName()))
                ? auth.getName()
                : "anonymous_session";

        log.info("REST: Record event for authenticated user '{}', schemeCode='{}', type='{}'",
                authenticatedUserId, request.getSchemeCode(), request.getEventType());

        RecommendationEventResponse response = eventService.recordEvent(authenticatedUserId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get recommendation interaction tracking metrics", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<RecommendationEventMetricsResponse> getMetrics() {
        RecommendationEventMetricsResponse response = eventService.getMetrics();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/readiness")
    @Operation(summary = "Get continuous ML training readiness snapshot (Phase 31/32)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot> getReadinessSnapshot() {
        Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot = readinessMonitor.evaluateLiveTelemetry();
        return ResponseEntity.ok(snapshot);
    }

    @GetMapping("/training-readiness")
    @Operation(summary = "Get Phase 34 Training Readiness Handoff state (Read-Only)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Phase34TrainingHandoff> getTrainingReadiness() {
        Phase34TrainingHandoff handoff = phase34ReadinessValidator.buildHandoff();
        return ResponseEntity.ok(handoff);
    }

    @GetMapping("/training-report")
    @Operation(summary = "Get Phase 34 Model Training Report (Read-Only verification)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Phase34TrainingReport> getTrainingReport() {
        Phase34TrainingReport report = phase34TrainingService.executeLiveTraining();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/model-evaluation")
    @Operation(summary = "Get Phase 34 Model Evaluation Report (Read-Only verification)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Phase34EvaluationReport> getModelEvaluationReport() {
        Phase34EvaluationReport report = phase34EvaluationService.evaluateLive();
        return ResponseEntity.ok(report);
    }
}

