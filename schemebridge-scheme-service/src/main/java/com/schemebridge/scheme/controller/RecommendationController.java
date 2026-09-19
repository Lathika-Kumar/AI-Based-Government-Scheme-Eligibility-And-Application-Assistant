package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.response.PersonalizedSchemeRecommendationResponse;
import com.schemebridge.scheme.service.EligibleSchemeRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "Personalized Recommendations", description = "Phase 7 AI-Based Eligible Scheme Recommendation & Ranking APIs")
@SecurityRequirement(name = "BearerAuth")
public class RecommendationController {

    private final EligibleSchemeRecommendationService recommendationService;

    @GetMapping
    @Operation(summary = "Get ranked personalized recommendations for the authenticated citizen")
    public ResponseEntity<PersonalizedSchemeRecommendationResponse> getRecommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        log.info("GET /api/recommendations invoked for authenticated userId={}, page={}, size={}", userId, safePage, safeSize);
        PersonalizedSchemeRecommendationResponse response =
                recommendationService.getPersonalizedRecommendations(userId, safePage, safeSize);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Get ranked personalized recommendations for the authenticated citizen (POST method)")
    public ResponseEntity<PersonalizedSchemeRecommendationResponse> postRecommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        log.info("POST /api/recommendations invoked for authenticated userId={}, page={}, size={}", userId, safePage, safeSize);
        PersonalizedSchemeRecommendationResponse response =
                recommendationService.getPersonalizedRecommendations(userId, safePage, safeSize);
        return ResponseEntity.ok(response);
    }
}
