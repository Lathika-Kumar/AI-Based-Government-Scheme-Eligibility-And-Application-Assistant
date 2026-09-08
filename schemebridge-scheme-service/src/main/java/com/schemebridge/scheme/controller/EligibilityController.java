package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.response.CitizenEligibilityEvaluationResponse;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResult;
import com.schemebridge.scheme.service.EligibilityEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for deterministic citizen eligibility evaluation (Phase 6B).
 */
@RestController
@RequestMapping("/api/eligibility")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Eligibility Evaluation", description = "Deterministic citizen eligibility evaluation endpoints")
@SecurityRequirement(name = "BearerAuth")
public class EligibilityController {

    private final EligibilityEvaluationService eligibilityEvaluationService;

    private String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("Unauthorized request: missing authentication context");
        }
        return auth.getName();
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluates the authenticated citizen against all candidate government schemes in the catalog")
    public ResponseEntity<CitizenEligibilityEvaluationResponse> evaluateAll() {
        String userId = getAuthenticatedUserId();
        CitizenEligibilityEvaluationResponse response = eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes(userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/evaluate/{schemeCode}")
    @Operation(summary = "Evaluates the authenticated citizen against a specific scheme by scheme code")
    public ResponseEntity<EligibilityEvaluationResult> evaluateScheme(@PathVariable String schemeCode) {
        String userId = getAuthenticatedUserId();
        EligibilityEvaluationResult result = eligibilityEvaluationService.evaluateCitizenAgainstScheme(userId, schemeCode);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
