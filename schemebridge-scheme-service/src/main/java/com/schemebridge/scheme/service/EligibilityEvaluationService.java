package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.document.SchemeLevel;
import com.schemebridge.scheme.document.SchemeStatus;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.CitizenEligibilityEvaluationResponse;
import com.schemebridge.scheme.dto.response.CitizenEligibilityEvaluationResponse.EvaluationSummary;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResult;
import com.schemebridge.scheme.dto.response.EligibilityStatus;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service orchestrating deterministic eligibility evaluation across the master scheme catalog.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EligibilityEvaluationService {

    private final SchemeRepository schemeRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final CitizenProfileService citizenProfileService;
    private final EligibilityEngineContract eligibilityEngine;
    private final MongoTemplate mongoTemplate;

    /**
     * Evaluates a citizen's profile against all candidate schemes in the 4,682 catalog.
     * Uses efficient indexed candidate retrieval followed by in-memory deterministic rule evaluation.
     */
    public CitizenEligibilityEvaluationResponse evaluateCitizenAgainstAllSchemes(String userId) {
        log.info("Starting deterministic eligibility evaluation for citizen userId={}", userId);

        CitizenProfile profile = citizenProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen profile not found for user: " + userId));

        CitizenEligibilityProfile eligibilityProfile = citizenProfileService.toCitizenEligibilityProfile(profile);

        // Pre-filter candidate schemes from MongoDB:
        // Match active schemes that are either CENTRAL or match the citizen's state
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(SchemeStatus.ACTIVE.name()));

        if (profile.getState() != null && !profile.getState().isBlank()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("schemeLevel").is(SchemeLevel.CENTRAL.name()),
                    Criteria.where("stateOrUt").regex("^" + profile.getState().trim() + "$", "i"),
                    Criteria.where("stateOrUt").is(null),
                    Criteria.where("stateOrUt").is("ALL")
            ));
        }

        List<Scheme> candidateSchemes = mongoTemplate.find(query, Scheme.class);
        log.info("Candidate schemes retrieved from MongoDB: count={} (citizenState={})",
                candidateSchemes.size(), profile.getState());

        List<EligibilityEvaluationResult> eligible = new ArrayList<>();
        List<EligibilityEvaluationResult> insufficient = new ArrayList<>();
        List<EligibilityEvaluationResult> notEligible = new ArrayList<>();

        for (Scheme scheme : candidateSchemes) {
            EligibilityEvaluationResult result = eligibilityEngine.evaluate(eligibilityProfile, scheme);

            if (result.getStatus() == EligibilityStatus.ELIGIBLE) {
                eligible.add(result);
            } else if (result.getStatus() == EligibilityStatus.INSUFFICIENT_DATA) {
                insufficient.add(result);
            } else {
                notEligible.add(result);
            }
        }

        EvaluationSummary summary = EvaluationSummary.builder()
                .totalEvaluated(candidateSchemes.size())
                .eligibleCount(eligible.size())
                .insufficientDataCount(insufficient.size())
                .notEligibleCount(notEligible.size())
                .build();

        log.info("Eligibility evaluation completed for userId={}: Total={}, Eligible={}, Insufficient={}, NotEligible={}",
                userId, summary.getTotalEvaluated(), summary.getEligibleCount(),
                summary.getInsufficientDataCount(), summary.getNotEligibleCount());

        return CitizenEligibilityEvaluationResponse.builder()
                .userId(userId)
                .citizenState(profile.getState())
                .evaluatedAt(Instant.now())
                .summary(summary)
                .eligibleSchemes(eligible)
                .insufficientDataSchemes(insufficient)
                .notEligibleSchemes(notEligible)
                .build();
    }

    /**
     * Evaluates a citizen's profile against a specific scheme by code.
     */
    public EligibilityEvaluationResult evaluateCitizenAgainstScheme(String userId, String schemeCode) {
        log.info("Evaluating eligibility for userId={} against schemeCode={}", userId, schemeCode);

        CitizenProfile profile = citizenProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen profile not found for user: " + userId));

        Scheme scheme = schemeRepository.findBySchemeCode(schemeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with code: " + schemeCode));

        CitizenEligibilityProfile eligibilityProfile = citizenProfileService.toCitizenEligibilityProfile(profile);

        return eligibilityEngine.evaluate(eligibilityProfile, scheme);
    }
}
