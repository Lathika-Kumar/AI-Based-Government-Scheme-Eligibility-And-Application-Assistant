package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResponse.ConditionEvaluationDetail;
import com.schemebridge.scheme.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchemeRecommendationService {

    private final SchemeRepository schemeRepository;
    private final EligibilityEngine eligibilityEngine;
    private final SchemeService schemeService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private SemanticEmbeddingIndexService semanticEmbeddingIndexService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.schemebridge.scheme.config.MlRecommenderProperties mlRecommenderProperties;

    public PersonalizedRecommendationResponse getRecommendations(
            CitizenEligibilityProfile profile,
            String status,
            int page,
            int size
    ) {
        // 1. Validation
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero.");
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("Page size must be between 1 and 50.");
        }

        // Enforce role constraints on status filtering
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isPrivileged = false;
        if (auth != null) {
            isPrivileged = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") 
                                || a.getAuthority().equals("ROLE_SCHEME_MANAGER"));
        }

        List<SchemeStatus> targetStatuses = new ArrayList<>();
        if (!isPrivileged) {
            if (status != null && !status.trim().isEmpty()) {
                List<SchemeStatus> requested = parseSchemeStatuses(status);
                for (SchemeStatus s : requested) {
                    if (s != SchemeStatus.ACTIVE) {
                        throw new SecurityException("Access denied to requested status: " + s);
                    }
                }
            }
            targetStatuses.add(SchemeStatus.ACTIVE);
        } else {
            if (status != null && !status.trim().isEmpty()) {
                targetStatuses.addAll(parseSchemeStatuses(status));
            } else {
                targetStatuses.add(SchemeStatus.ACTIVE); // default to ACTIVE
            }
        }

        // 2. Candidate Retrieval
        // Retrieve candidate schemes from MongoDB based on statuses
        List<Scheme> candidates = new ArrayList<>();
        for (SchemeStatus s : targetStatuses) {
            candidates.addAll(schemeRepository.findAllByStatus(s));
        }

        int totalSchemesEvaluated = candidates.size();
        int eligibleCount = 0;
        int nearMatchCount = 0;
        int indeterminateCount = 0;

        List<RecommendationResponseItem> recommendations = new ArrayList<>();

        // 3. Evaluation & Classification
        for (Scheme scheme : candidates) {
            EligibilityEvaluationResponse evalRes = eligibilityEngine.evaluateScheme(scheme, profile);
            List<ConditionEvaluationDetail> details = evalRes.getDetails();

            int totalConditions = details.size();
            int passedCount = 0;
            int failedCount = 0;
            int indeterminateCountInScheme = 0;

            for (ConditionEvaluationDetail detail : details) {
                if (detail.getStatus() == EvaluationStatus.ELIGIBLE) {
                    passedCount++;
                } else if (detail.getStatus() == EvaluationStatus.NOT_ELIGIBLE) {
                    failedCount++;
                } else if (detail.getStatus() == EvaluationStatus.INDETERMINATE) {
                    indeterminateCountInScheme++;
                }
            }

            double matchScore;
            RecommendationCategory recCategory;

            if (totalConditions == 0) {
                matchScore = 100.0;
                recCategory = RecommendationCategory.ELIGIBLE;
            } else {
                if (failedCount == 0 && indeterminateCountInScheme == 0) {
                    matchScore = 100.0;
                    recCategory = RecommendationCategory.ELIGIBLE;
                } else if (indeterminateCountInScheme > 0 && failedCount == 0) {
                    matchScore = ((double) passedCount / totalConditions) * 100.0;
                    recCategory = RecommendationCategory.INDETERMINATE;
                } else {
                    // failedCount > 0
                    double evaluatedCount = passedCount + failedCount;
                    double passRate = (passedCount / evaluatedCount) * 100.0;
                    matchScore = ((double) passedCount / totalConditions) * 100.0;

                    if (passRate >= 50.0) {
                        recCategory = RecommendationCategory.NEAR_MATCH;
                    } else {
                        recCategory = null; // Excluded (NOT_ELIGIBLE)
                    }
                }
            }

            // Update global counts
            if (recCategory == RecommendationCategory.ELIGIBLE) {
                eligibleCount++;
            } else if (recCategory == RecommendationCategory.NEAR_MATCH) {
                nearMatchCount++;
            } else if (recCategory == RecommendationCategory.INDETERMINATE) {
                indeterminateCount++;
            }

            // Exclude from recommendation list if NOT_ELIGIBLE
            if (recCategory == null) {
                continue;
            }

            // Generate explanations/reasons
            List<String> reasons = new ArrayList<>();
            if (recCategory == RecommendationCategory.ELIGIBLE) {
                reasons.add("Meets all required eligibility conditions.");
                for (ConditionEvaluationDetail detail : details) {
                    reasons.add(detail.getExplanation() + " (satisfied)");
                }
            } else if (recCategory == RecommendationCategory.NEAR_MATCH) {
                double evaluatedCount = passedCount + failedCount;
                reasons.add(String.format("Meets %d of %d evaluated eligibility conditions.", passedCount, (int) evaluatedCount));
                for (ConditionEvaluationDetail detail : details) {
                    if (detail.getStatus() == EvaluationStatus.NOT_ELIGIBLE) {
                        reasons.add("Failed requirement: " + detail.getExplanation());
                    }
                }
            } else if (recCategory == RecommendationCategory.INDETERMINATE) {
                reasons.add("Eligibility status is incomplete due to missing information or manual verification rules.");
                for (ConditionEvaluationDetail detail : details) {
                    if (detail.getStatus() == EvaluationStatus.INDETERMINATE) {
                        if ("LEGACY_FREE_TEXT".equalsIgnoreCase(detail.getField())) {
                            String exp = detail.getExplanation();
                            if (exp.startsWith("Eligibility requires manual verification: ")) {
                                exp = exp.substring("Eligibility requires manual verification: ".length());
                            }
                            reasons.add("Requires manual verification: " + exp);
                        } else {
                            reasons.add("Missing required information: " + detail.getField());
                        }
                    }
                }
            }

            Double semanticScore = null;
            String rankingMethod = "DETERMINISTIC_FALLBACK";
            String modelVersion = (mlRecommenderProperties != null && mlRecommenderProperties.getMl() != null)
                    ? mlRecommenderProperties.getMl().getModelVersion() : "2.2.0-hybrid-semantic-384d";

            if (recCategory == RecommendationCategory.ELIGIBLE) {
                if (profile.getState() != null && scheme.getStateOrUt() != null && scheme.getStateOrUt().equalsIgnoreCase(profile.getState())) {
                    reasons.add("Matches your state (" + scheme.getStateOrUt() + ").");
                } else if (scheme.getSchemeLevel() == SchemeLevel.CENTRAL) {
                    reasons.add("National Central Scheme available across India.");
                }
                if (profile.getOccupation() != null && !profile.getOccupation().isBlank()) {
                    reasons.add("Matches your occupation category (" + profile.getOccupation() + ").");
                }
                if (profile.getAnnualIncome() != null && profile.getAnnualIncome() <= 250000) {
                    reasons.add("Income is within applicable welfare criteria.");
                }

                if (semanticEmbeddingIndexService != null && semanticEmbeddingIndexService.isAvailable()) {
                    String query = (profile.getOccupation() != null ? profile.getOccupation() : "") + " "
                            + (profile.getState() != null ? profile.getState() : "") + " "
                            + (profile.getSocialCategory() != null ? profile.getSocialCategory() : "");
                    double sim = semanticEmbeddingIndexService.computeCosineSimilarity(scheme.getSchemeCode(), query);
                    semanticScore = Math.round(sim * 10000.0) / 10000.0;
                    rankingMethod = "HYBRID_SEMANTIC";
                }
            }

            java.time.Instant deadline = scheme.getApplicationInfo() != null ?
                    (scheme.getApplicationInfo().getDeadline() != null ? scheme.getApplicationInfo().getDeadline() : scheme.getApplicationInfo().getApplicationEndDate())
                    : null;

            // Build item DTO
            RecommendationResponseItem item = RecommendationResponseItem.builder()
                    .schemeId(scheme.getId())
                    .schemeCode(scheme.getSchemeCode())
                    .slug(scheme.getSlug())
                    .title(scheme.getTitle())
                    .shortDescription(scheme.getShortDescription())
                    .schemeLevel(scheme.getSchemeLevel())
                    .stateOrUt(scheme.getStateOrUt())
                    .beneficiaryType(scheme.getBeneficiaryType())
                    .schemeType(scheme.getSchemeType())
                    .matchScore(matchScore)
                    .recommendationCategory(recCategory)
                    .eligibilityStatus(evalRes.getStatus().name())
                    .matchedConditions(evalRes.getMatchedConditions())
                    .failedConditions(evalRes.getFailedConditions())
                    .missingInformation(evalRes.getMissingInformation())
                    .reasons(reasons)
                    .details(details)
                    .semanticScore(semanticScore)
                    .rankingMethod(rankingMethod)
                    .modelVersion(modelVersion)
                    .deadline(deadline)
                    .build();

            recommendations.add(item);
        }

        // 4. Ranking
        recommendations.sort(new RecommendationResponseItemComparator());

        // 5. Pagination
        int fromIndex = page * size;
        List<RecommendationResponseItem> paginatedList = new ArrayList<>();
        if (fromIndex < recommendations.size()) {
            int toIndex = Math.min(fromIndex + size, recommendations.size());
            paginatedList = recommendations.subList(fromIndex, toIndex);
        }

        return PersonalizedRecommendationResponse.builder()
                .totalSchemesEvaluated(totalSchemesEvaluated)
                .eligibleCount(eligibleCount)
                .nearMatchCount(nearMatchCount)
                .indeterminateCount(indeterminateCount)
                .recommendations(paginatedList)
                .build();
    }

    private List<SchemeStatus> parseSchemeStatuses(String input) {
        if (input == null || input.trim().isEmpty()) return List.of();
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return SchemeStatus.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid status value: " + s);
                    }
                })
                .collect(Collectors.toList());
    }

    private static class RecommendationResponseItemComparator implements Comparator<RecommendationResponseItem> {
        @Override
        public int compare(RecommendationResponseItem o1, RecommendationResponseItem o2) {
            // 1. Recommendation Category ordering (ELIGIBLE < NEAR_MATCH < INDETERMINATE in order)
            int cat1 = getCategoryOrder(o1.getRecommendationCategory());
            int cat2 = getCategoryOrder(o2.getRecommendationCategory());
            if (cat1 != cat2) {
                return Integer.compare(cat1, cat2);
            }

            // 2. matchScore descending
            int scoreCompare = Double.compare(o2.getMatchScore(), o1.getMatchScore());
            if (scoreCompare != 0) {
                return scoreCompare;
            }

            // 3. schemeCode ascending
            return o1.getSchemeCode().compareTo(o2.getSchemeCode());
        }

        private int getCategoryOrder(RecommendationCategory cat) {
            if (cat == RecommendationCategory.ELIGIBLE) return 1;
            if (cat == RecommendationCategory.NEAR_MATCH) return 2;
            if (cat == RecommendationCategory.INDETERMINATE) return 3;
            return 4;
        }
    }
}
