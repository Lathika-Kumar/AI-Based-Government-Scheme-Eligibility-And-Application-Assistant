package com.schemebridge.scheme.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 25: Safe AI/ML Model Registry & Governance Service.
 * Tracks recommendation model versions through strict lifecycle gates:
 * CANDIDATE -> SHADOW -> APPROVED -> ACTIVE -> REJECTED / ROLLED_BACK.
 * Enforces non-negotiable safety gates: 0.00% eligibility violations, <200ms latency.
 */
@Service
@Slf4j
public class ModelRegistryService {

    public enum ModelStatus {
        CANDIDATE,
        SHADOW,
        APPROVED,
        ACTIVE,
        REJECTED,
        ROLLED_BACK
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelRecord {
        private String modelVersion;
        private String modelType;
        private Instant createdAt;
        private String trainingDatasetVersion;
        private Map<String, Double> metrics;
        private Double statutoryEligibilityViolationRate;
        private Double avgLatencyMs;
        private ModelStatus status;
        private String statusReason;
        private boolean deterministicFallbackAvailable;
        private boolean artifactLoadable;
    }

    private final Map<String, ModelRecord> registry = new ConcurrentHashMap<>();
    private volatile String activeModelVersion;
    private volatile String previousActiveModelVersion;
    private volatile String shadowModelVersion;

    public ModelRegistryService() {
        initializeDefaultRegistry();
    }

    private void initializeDefaultRegistry() {
        // Active Baseline: Production Hybrid Semantic Model (2.2.0)
        ModelRecord activeModel = ModelRecord.builder()
                .modelVersion("2.2.0-hybrid-semantic-384d")
                .modelType("HYBRID_SEMANTIC_MADM")
                .createdAt(Instant.parse("2026-09-04T21:47:01Z"))
                .trainingDatasetVersion("phase22b-master-schemes-4734")
                .metrics(Map.of("ndcg@5", 0.6384, "precision@5", 0.7200, "mrr", 0.8120))
                .statutoryEligibilityViolationRate(0.00)
                .avgLatencyMs(8.23)
                .status(ModelStatus.ACTIVE)
                .statusReason("Production Active Hybrid Elastic + Semantic Embedding Model")
                .deterministicFallbackAvailable(true)
                .artifactLoadable(true)
                .build();

        // Baseline Deterministic Fallback
        ModelRecord fallbackModel = ModelRecord.builder()
                .modelVersion("1.0.0-deterministic")
                .modelType("DETERMINISTIC_MADM")
                .createdAt(Instant.parse("2026-08-30T00:00:00Z"))
                .trainingDatasetVersion("static-rules")
                .metrics(Map.of("ndcg@5", 0.5883, "precision@5", 0.6500, "mrr", 0.7500))
                .statutoryEligibilityViolationRate(0.00)
                .avgLatencyMs(1.09)
                .status(ModelStatus.APPROVED)
                .statusReason("Authoritative deterministic fallback ranker")
                .deterministicFallbackAvailable(true)
                .artifactLoadable(true)
                .build();

        registry.put(activeModel.getModelVersion(), activeModel);
        registry.put(fallbackModel.getModelVersion(), fallbackModel);
        this.activeModelVersion = activeModel.getModelVersion();
        this.previousActiveModelVersion = fallbackModel.getModelVersion();
    }

    public synchronized ModelRecord registerCandidate(ModelRecord candidate) {
        if (candidate.getModelVersion() == null || candidate.getModelVersion().isBlank()) {
            throw new IllegalArgumentException("modelVersion is required for candidate registration");
        }
        candidate.setStatus(ModelStatus.CANDIDATE);
        candidate.setCreatedAt(Instant.now());
        registry.put(candidate.getModelVersion(), candidate);
        log.info("Registered candidate model: version={}, type={}", candidate.getModelVersion(), candidate.getModelType());
        return candidate;
    }

    public synchronized boolean promoteToShadow(String modelVersion) {
        ModelRecord record = registry.get(modelVersion);
        if (record == null) {
            log.warn("Cannot promote to shadow: model {} not found", modelVersion);
            return false;
        }
        if (record.getStatus() != ModelStatus.CANDIDATE && record.getStatus() != ModelStatus.APPROVED) {
            log.warn("Cannot promote to shadow: model {} in status {}", modelVersion, record.getStatus());
            return false;
        }
        record.setStatus(ModelStatus.SHADOW);
        this.shadowModelVersion = modelVersion;
        log.info("Model promoted to SHADOW mode: {}", modelVersion);
        return true;
    }

    public synchronized PromotionResult evaluateAndPromote(String modelVersion) {
        ModelRecord candidate = registry.get(modelVersion);
        if (candidate == null) {
            return new PromotionResult(false, modelVersion, "Model not found in registry: " + modelVersion);
        }

        List<String> failureReasons = new ArrayList<>();

        // Gate 1: Legitimate training dataset must exist
        if (candidate.getTrainingDatasetVersion() == null || candidate.getTrainingDatasetVersion().isBlank()
                || "NONE".equalsIgnoreCase(candidate.getTrainingDatasetVersion())) {
            failureReasons.add("Missing legitimate training dataset (TRAINING_NOT_READY).");
        }

        // Gate 2: Candidate must have been evaluated
        if (candidate.getMetrics() == null || candidate.getMetrics().isEmpty()) {
            failureReasons.add("No offline test evaluation metrics found.");
        }

        // Gate 3: Statutory eligibility violation rate MUST be 0.00%
        if (candidate.getStatutoryEligibilityViolationRate() == null
                || candidate.getStatutoryEligibilityViolationRate() > 0.0001) {
            failureReasons.add("Statutory eligibility violation rate exceeded target 0.00% (found: "
                    + candidate.getStatutoryEligibilityViolationRate() + ").");
        }

        // Gate 4: Latency budget <= 200ms
        if (candidate.getAvgLatencyMs() == null || candidate.getAvgLatencyMs() > 200.0) {
            failureReasons.add("Average inference latency exceeded circuit breaker budget of 200ms.");
        }

        // Gate 5: Deterministic fallback must remain available
        if (!candidate.isDeterministicFallbackAvailable()) {
            failureReasons.add("Deterministic fallback must be available.");
        }

        // Gate 6: Artifact must load successfully
        if (!candidate.isArtifactLoadable()) {
            failureReasons.add("Model artifact failed to load into memory.");
        }

        // Gate 7: Regression check against active model
        ModelRecord activeModel = registry.get(activeModelVersion);
        if (activeModel != null && activeModel.getMetrics() != null && candidate.getMetrics() != null) {
            Double activeNdcg = activeModel.getMetrics().getOrDefault("ndcg@5", 0.0);
            Double candidateNdcg = candidate.getMetrics().getOrDefault("ndcg@5", 0.0);
            if (candidateNdcg < activeNdcg) {
                failureReasons.add("NDCG@5 regression: candidate (" + candidateNdcg + ") < active baseline (" + activeNdcg + ").");
            }
        }

        if (!failureReasons.isEmpty()) {
            candidate.setStatus(ModelStatus.REJECTED);
            String combinedReason = String.join("; ", failureReasons);
            candidate.setStatusReason("Promotion Rejected: " + combinedReason);
            log.warn("Model promotion REJECTED for {}: {}", modelVersion, combinedReason);
            return new PromotionResult(false, modelVersion, candidate.getStatusReason());
        }

        // All gates passed: Activate candidate model
        if (activeModel != null) {
            activeModel.setStatus(ModelStatus.APPROVED);
            this.previousActiveModelVersion = this.activeModelVersion;
        }

        candidate.setStatus(ModelStatus.ACTIVE);
        candidate.setStatusReason("Promoted to ACTIVE after passing all statutory, metric, and latency gates.");
        this.activeModelVersion = modelVersion;
        if (modelVersion.equals(this.shadowModelVersion)) {
            this.shadowModelVersion = null;
        }

        log.info("Model {} successfully PROMOTED to ACTIVE production model.", modelVersion);
        return new PromotionResult(true, modelVersion, candidate.getStatusReason());
    }

    public synchronized boolean rollbackActiveModel() {
        if (previousActiveModelVersion == null || !registry.containsKey(previousActiveModelVersion)) {
            log.warn("Cannot rollback: no previous approved model available.");
            return false;
        }

        ModelRecord currentActive = registry.get(activeModelVersion);
        if (currentActive != null) {
            currentActive.setStatus(ModelStatus.ROLLED_BACK);
            currentActive.setStatusReason("Rolled back to previous safe model: " + previousActiveModelVersion);
        }

        ModelRecord previousModel = registry.get(previousActiveModelVersion);
        previousModel.setStatus(ModelStatus.ACTIVE);
        previousModel.setStatusReason("Restored to ACTIVE via safe rollback operation.");

        String rolledBack = this.activeModelVersion;
        this.activeModelVersion = this.previousActiveModelVersion;
        this.previousActiveModelVersion = "1.0.0-deterministic";

        log.info("Model rollback executed: {} -> ACTIVE {}", rolledBack, activeModelVersion);
        return true;
    }

    public ModelRecord getActiveModel() {
        return registry.get(activeModelVersion);
    }

    public String getActiveModelVersion() {
        return activeModelVersion;
    }

    public String getShadowModelVersion() {
        return shadowModelVersion;
    }

    public ModelRecord getModel(String modelVersion) {
        return registry.get(modelVersion);
    }

    public List<ModelRecord> getAllModels() {
        return new ArrayList<>(registry.values());
    }

    public record PromotionResult(boolean promoted, String modelVersion, String message) {}
}
