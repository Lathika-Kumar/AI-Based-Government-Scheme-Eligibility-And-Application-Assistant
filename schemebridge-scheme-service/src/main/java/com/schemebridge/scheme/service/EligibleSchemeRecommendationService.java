package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.Application;
import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.document.SchemeBenefit;
import com.schemebridge.scheme.document.SchemeLevel;
import com.schemebridge.scheme.document.SchemeVerifiedData;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.ml.feature.ComparisonFeatures;
import com.schemebridge.scheme.ml.feature.FeatureMatchStatus;
import com.schemebridge.scheme.ml.feature.FeatureVectorBuilder;
import com.schemebridge.scheme.ml.feature.SchemeFeatureExtractor;
import com.schemebridge.scheme.ml.feature.SchemeFeatures;
import com.schemebridge.scheme.ml.feature.UserFeatureExtractor;
import com.schemebridge.scheme.ml.feature.UserFeatures;
import com.schemebridge.scheme.ml.feature.UserSchemeComparisonService;
import com.schemebridge.scheme.ml.feature.UserSchemeFeatureVector;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import com.schemebridge.scheme.repository.SchemeVerifiedDataRepository;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 8 / Phase 26 Production AI/ML Hybrid Recommendation Service.
 * Evaluates candidate schemes downstream of the deterministic eligibility engine,
 * applying multi-criteria utility scoring and content-based semantic token matching.
 * Invariant: EligibilityEngine.evaluate() is the sole statutory authority.
 * ML/ranking only prioritizes already-eligible schemes.
 */
@Service
@Slf4j
public class EligibleSchemeRecommendationService {

    private final EligibilityEvaluationService eligibilityEvaluationService;
    private final CitizenProfileRepository citizenProfileRepository;
    private final SchemeRepository schemeRepository;
    private final com.schemebridge.scheme.config.MlRecommenderProperties properties;
    private final SemanticEmbeddingIndexService semanticEmbeddingIndexService;
    private final ModelRegistryService modelRegistryService;
    private final UserFeatureExtractor userFeatureExtractor;
    private final SchemeFeatureExtractor schemeFeatureExtractor;
    private final UserSchemeComparisonService userSchemeComparisonService;
    private final FeatureVectorBuilder featureVectorBuilder;
    private final SchemeVerifiedDataRepository schemeVerifiedDataRepository;
    private final SchemeDocumentRequirementResolver schemeDocumentRequirementResolver;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private DocumentChecklistGenerator documentChecklistGenerator;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.schemebridge.scheme.repository.ApplicationRepository applicationRepository;

    public static final String MODEL_FAMILY = "Hybrid Eligibility-Gated + Semantic Vector Space Model";
    public static final String MODEL_VERSION = "2.2.0-hybrid-semantic-384d";
    public static final String FALLBACK_MODEL_VERSION = "1.0.0-deterministic";

    @org.springframework.beans.factory.annotation.Autowired
    public EligibleSchemeRecommendationService(
            EligibilityEvaluationService eligibilityEvaluationService,
            CitizenProfileRepository citizenProfileRepository,
            SchemeRepository schemeRepository,
            com.schemebridge.scheme.config.MlRecommenderProperties properties,
            SemanticEmbeddingIndexService semanticEmbeddingIndexService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ModelRegistryService modelRegistryService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) UserFeatureExtractor userFeatureExtractor,
            @org.springframework.beans.factory.annotation.Autowired(required = false) SchemeFeatureExtractor schemeFeatureExtractor,
            @org.springframework.beans.factory.annotation.Autowired(required = false) UserSchemeComparisonService userSchemeComparisonService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) FeatureVectorBuilder featureVectorBuilder,
            @org.springframework.beans.factory.annotation.Autowired(required = false) SchemeVerifiedDataRepository schemeVerifiedDataRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) SchemeDocumentRequirementResolver schemeDocumentRequirementResolver
    ) {
        this.eligibilityEvaluationService = eligibilityEvaluationService;
        this.citizenProfileRepository = citizenProfileRepository;
        this.schemeRepository = schemeRepository;
        this.properties = properties != null ? properties : new com.schemebridge.scheme.config.MlRecommenderProperties();
        this.semanticEmbeddingIndexService = semanticEmbeddingIndexService;
        this.modelRegistryService = modelRegistryService;
        this.userFeatureExtractor = userFeatureExtractor != null ? userFeatureExtractor : new UserFeatureExtractor();
        this.schemeFeatureExtractor = schemeFeatureExtractor != null ? schemeFeatureExtractor : new SchemeFeatureExtractor();
        this.userSchemeComparisonService = userSchemeComparisonService != null ? userSchemeComparisonService : new UserSchemeComparisonService();
        this.featureVectorBuilder = featureVectorBuilder != null ? featureVectorBuilder : new FeatureVectorBuilder();
        this.schemeVerifiedDataRepository = schemeVerifiedDataRepository;
        this.schemeDocumentRequirementResolver = schemeDocumentRequirementResolver != null ? schemeDocumentRequirementResolver : new SchemeDocumentRequirementResolver(schemeVerifiedDataRepository);
    }

    public EligibleSchemeRecommendationService(
            EligibilityEvaluationService eligibilityEvaluationService,
            CitizenProfileRepository citizenProfileRepository,
            SchemeRepository schemeRepository,
            com.schemebridge.scheme.config.MlRecommenderProperties properties,
            SemanticEmbeddingIndexService semanticEmbeddingIndexService,
            ModelRegistryService modelRegistryService,
            UserFeatureExtractor userFeatureExtractor,
            SchemeFeatureExtractor schemeFeatureExtractor,
            UserSchemeComparisonService userSchemeComparisonService,
            FeatureVectorBuilder featureVectorBuilder,
            SchemeVerifiedDataRepository schemeVerifiedDataRepository
    ) {
        this(eligibilityEvaluationService, citizenProfileRepository, schemeRepository, properties, semanticEmbeddingIndexService, modelRegistryService, userFeatureExtractor, schemeFeatureExtractor, userSchemeComparisonService, featureVectorBuilder, schemeVerifiedDataRepository, null);
    }

    public EligibleSchemeRecommendationService(
            EligibilityEvaluationService eligibilityEvaluationService,
            CitizenProfileRepository citizenProfileRepository,
            SchemeRepository schemeRepository,
            com.schemebridge.scheme.config.MlRecommenderProperties properties,
            SemanticEmbeddingIndexService semanticEmbeddingIndexService,
            ModelRegistryService modelRegistryService
    ) {
        this(eligibilityEvaluationService, citizenProfileRepository, schemeRepository, properties, semanticEmbeddingIndexService, modelRegistryService, null, null, null, null, null, null);
    }

    public EligibleSchemeRecommendationService(
            EligibilityEvaluationService eligibilityEvaluationService,
            CitizenProfileRepository citizenProfileRepository,
            SchemeRepository schemeRepository,
            com.schemebridge.scheme.config.MlRecommenderProperties properties,
            SemanticEmbeddingIndexService semanticEmbeddingIndexService
    ) {
        this(eligibilityEvaluationService, citizenProfileRepository, schemeRepository, properties, semanticEmbeddingIndexService, null);
    }

    public EligibleSchemeRecommendationService(
            EligibilityEvaluationService eligibilityEvaluationService,
            CitizenProfileRepository citizenProfileRepository,
            SchemeRepository schemeRepository
    ) {
        this(eligibilityEvaluationService, citizenProfileRepository, schemeRepository, new com.schemebridge.scheme.config.MlRecommenderProperties(), null, null);
    }

    public PersonalizedSchemeRecommendationResponse getPersonalizedRecommendations(
            com.schemebridge.scheme.dto.request.CitizenEligibilityProfile eligibilityProfile,
            int page,
            int size
    ) {
        String userId = "citizen";
        CitizenProfile profile = toCitizenProfile(eligibilityProfile, userId);
        CitizenEligibilityEvaluationResponse evalResponse =
                eligibilityEvaluationService.evaluateProfileAgainstAllSchemes(eligibilityProfile, userId);
        return generateRecommendationsForEvaluatedProfile(profile, evalResponse, page, size);
    }

    public PersonalizedSchemeRecommendationResponse getPersonalizedRecommendations(
            String userId,
            int page,
            int size
    ) {
        CitizenProfile profile = citizenProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen profile not found for userId: " + userId));

        CitizenEligibilityEvaluationResponse evalResponse =
                eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes(userId);
        return generateRecommendationsForEvaluatedProfile(profile, evalResponse, page, size);
    }

    private CitizenProfile toCitizenProfile(com.schemebridge.scheme.dto.request.CitizenEligibilityProfile ep, String userId) {
        if (ep == null) return CitizenProfile.builder().userId(userId).build();
        return CitizenProfile.builder()
                .userId(userId)
                .age(ep.getAge())
                .gender(ep.getGender())
                .annualIncome(ep.getAnnualIncome())
                .occupation(ep.getOccupation())
                .state(ep.getState())
                .socialCategory(ep.getSocialCategory())
                .disabilityStatus(ep.getDisabilityStatus())
                .isFarmer(ep.getIsFarmer())
                .isStudent(ep.getIsStudent())
                .bplStatus(ep.getBplStatus())
                .maritalStatus(ep.getMaritalStatus())
                .district(ep.getDistrict())
                .employmentStatus(ep.getEmploymentStatus())
                .residentialAreaType(ep.getResidentialAreaType())
                .rationCardType(ep.getRationCardType())
                .landholdingArea(ep.getLandholdingArea())
                .minorityStatus(ep.getMinorityStatus())
                .build();
    }

    public PersonalizedSchemeRecommendationResponse generateRecommendationsForEvaluatedProfile(
            CitizenProfile profile,
            CitizenEligibilityEvaluationResponse evalResponse,
            int page,
            int size
    ) {
        long startTimeMs = System.currentTimeMillis();
        String userId = profile.getUserId() != null ? profile.getUserId() : "citizen";
        log.info("Generating Phase 22B personalized recommendations for userId={}", userId);

        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero.");
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("Page size must be between 1 and 50.");
        }

        // 2. MANDATORY HARD ELIGIBILITY GATE: Deterministic Eligibility Engine
        // INVARIANT: EligibilityEngine.evaluate() executes strictly before feature extraction or ranking
        List<EligibilityEvaluationResult> eligibleCandidates = evalResponse != null && evalResponse.getEligibleSchemes() != null
                ? evalResponse.getEligibleSchemes() : List.of();
        int totalCatalogEvaluated = evalResponse != null && evalResponse.getSummary() != null
                ? evalResponse.getSummary().getTotalEvaluated() : 0;
        int eligibleCount = eligibleCandidates.size();

        log.info("Phase 22B Gate: {} evaluated, {} ELIGIBLE candidates entering ranking for userId={}",
                totalCatalogEvaluated, eligibleCount, userId);

        String activeModelVersion = (modelRegistryService != null && modelRegistryService.getActiveModelVersion() != null)
                ? modelRegistryService.getActiveModelVersion()
                : ((properties != null && properties.getMl() != null) ? properties.getMl().getModelVersion() : MODEL_VERSION);

        if (eligibleCandidates.isEmpty()) {
            return PersonalizedSchemeRecommendationResponse.builder()
                    .userId(userId)
                    .citizenState(profile.getState())
                    .totalCatalogEvaluated(totalCatalogEvaluated)
                    .eligibleCandidatesFound(0)
                    .totalRecommendationsReturned(0)
                    .page(page)
                    .size(size)
                    .generatedAt(Instant.now())
                    .modelFamily(MODEL_FAMILY)
                    .modelVersion(activeModelVersion)
                    .fallbackModel(FALLBACK_MODEL_VERSION)
                    .rankingMethod("DETERMINISTIC_FALLBACK")
                    .fallbackUsed(false)
                    .recommendations(List.of())
                    .build();
        }

        // 3. Batch fetch scheme documents and authoritative SchemeVerifiedData
        Set<String> eligibleCodes = eligibleCandidates.stream()
                .map(EligibilityEvaluationResult::getSchemeCode)
                .collect(Collectors.toSet());

        List<Scheme> schemeDocs = null;
        try {
            schemeDocs = schemeRepository.findBySchemeCodeIn(eligibleCodes);
        } catch (Exception e) {
            log.debug("Notice: findBySchemeCodeIn lookup error: {}", e.getMessage());
        }
        if (schemeDocs == null || schemeDocs.isEmpty()) {
            schemeDocs = schemeRepository.findAll();
        }
        Map<String, Scheme> schemeMap = (schemeDocs != null) ? schemeDocs.stream()
                .filter(s -> s.getSchemeCode() != null && eligibleCodes.contains(s.getSchemeCode()))
                .collect(Collectors.toMap(Scheme::getSchemeCode, s -> s, (a, b) -> a)) : Collections.emptyMap();

        Map<String, SchemeVerifiedData> verifiedMap = new HashMap<>();
        if (schemeVerifiedDataRepository != null && !eligibleCodes.isEmpty()) {
            try {
                List<SchemeVerifiedData> vList = schemeVerifiedDataRepository.findBySchemeCodeIn(eligibleCodes);
                if (vList != null) {
                    for (SchemeVerifiedData v : vList) {
                        if (v.getSchemeCode() != null) {
                            verifiedMap.put(v.getSchemeCode(), v);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Notice: SchemeVerifiedData repository batch lookup notice: {}", e.getMessage());
            }
        }

        boolean mlEnabled = (properties != null && properties.getMl() != null) && properties.getMl().isEnabled();
        boolean mlAvailable = mlEnabled && semanticEmbeddingIndexService != null && semanticEmbeddingIndexService.isAvailable();
        boolean fallbackUsed = !mlAvailable;
        String rankingMethod = mlAvailable ? "HYBRID_SEMANTIC" : "DETERMINISTIC_FALLBACK";
        int timeoutMs = (properties != null && properties.getMl() != null) ? properties.getMl().getTimeoutMs() : 200;

        List<RankedSchemeItem> scoredItems = new ArrayList<>();
        List<RankedSchemeItem> shadowDeterministicItems = new ArrayList<>();

        // Extract privacy-safe UserFeatures
        UserFeatures userFeatures = userFeatureExtractor.extractFromCitizenProfile(profile);

        // Precompute candidate features for explanation grounding and vector assignment
        Map<String, SchemeFeatures> schemeFeaturesMap = new HashMap<>();
        Map<String, ComparisonFeatures> compFeaturesMap = new HashMap<>();

        long mlStartTimeMs = System.currentTimeMillis();

        try {
            for (EligibilityEvaluationResult candidate : eligibleCandidates) {
                Scheme scheme = schemeMap.get(candidate.getSchemeCode());
                if (scheme == null) continue;

                // Check timeout limit on ML ranking latency
                if (!fallbackUsed && System.currentTimeMillis() - mlStartTimeMs > timeoutMs) {
                    log.warn("ML recommendation latency exceeded threshold ({} ms) for userId={}. Falling back to deterministic scoring.",
                            timeoutMs, userId);
                    fallbackUsed = true;
                    rankingMethod = "DETERMINISTIC_FALLBACK";
                    // If ML timeout trips mid-scoring, discard partial ML results so all items are uniformly scored
                    scoredItems.clear();
                    for (EligibilityEvaluationResult prevCandidate : eligibleCandidates) {
                        Scheme prevScheme = schemeMap.get(prevCandidate.getSchemeCode());
                        if (prevScheme == null) continue;
                        SchemeVerifiedData prevVData = verifiedMap.get(prevCandidate.getSchemeCode());
                        SchemeFeatures prevSF = schemeFeaturesMap.computeIfAbsent(prevCandidate.getSchemeCode(), k -> schemeFeatureExtractor.extractFromScheme(prevScheme, prevVData));
                        ComparisonFeatures prevCF = compFeaturesMap.computeIfAbsent(prevCandidate.getSchemeCode(), k -> userSchemeComparisonService.compare(userFeatures, prevSF));
                        ScoredFeatureResult fallbackScore = computeRecommendationScore(profile, prevScheme, prevVData, prevCF, false);
                        RankedSchemeItem fallbackItem = RankedSchemeItem.builder()
                                .schemeCode(prevCandidate.getSchemeCode())
                                .slug(prevCandidate.getSlug() != null ? prevCandidate.getSlug() : prevScheme.getSlug())
                                .schemeTitle(prevCandidate.getSchemeTitle() != null ? prevCandidate.getSchemeTitle() : (prevScheme.getTitle() != null ? prevScheme.getTitle().getEnglish() : ""))
                                .shortDescription(prevScheme.getShortDescription() != null ? prevScheme.getShortDescription().getEnglish() : "")
                                .schemeLevel(prevCandidate.getSchemeLevel() != null ? prevCandidate.getSchemeLevel() : (prevScheme.getSchemeLevel() != null ? prevScheme.getSchemeLevel().name() : "CENTRAL"))
                                .stateOrUt(prevScheme.getStateOrUt())
                                .categoryCode(prevCandidate.getCategoryCode() != null ? prevCandidate.getCategoryCode() : (prevScheme.getCategory() != null ? prevScheme.getCategory().getCode() : ""))
                                .categoryName(prevCandidate.getCategoryName() != null ? prevCandidate.getCategoryName() : (prevScheme.getCategory() != null ? prevScheme.getCategory().getName() : ""))
                                .beneficiaryType(prevScheme.getBeneficiaryType())
                                .schemeType(prevScheme.getSchemeType())
                                .recommendationScore(fallbackScore.finalScore())
                                .eligibilityStatus("ELIGIBLE")
                                .confidenceScore(prevCandidate.getConfidenceScore() != null ? prevCandidate.getConfidenceScore() : 1.0)
                                .passedConditions(prevCandidate.getPassedConditions() != null ? prevCandidate.getPassedConditions() : List.of())
                                .verifiedAttributesUsed(prevCandidate.getVerifiedAttributesUsed() != null ? prevCandidate.getVerifiedAttributesUsed() : List.of())
                                .explanation(buildExplanation(profile, prevScheme, fallbackScore, prevCandidate, prevCF))
                                .reasons(buildPlainReasons(profile, prevScheme, fallbackScore, prevCF))
                                .build();
                        scoredItems.add(fallbackItem);
                    }
                    break;
                }

                // Extract authoritative criteria (Priority 1: verified data, Priority 2: master scheme rules)
                SchemeVerifiedData verifiedData = verifiedMap.get(candidate.getSchemeCode());
                SchemeFeatures schemeFeatures = schemeFeatureExtractor.extractFromScheme(scheme, verifiedData);
                ComparisonFeatures compFeatures = userSchemeComparisonService.compare(userFeatures, schemeFeatures);

                schemeFeaturesMap.put(candidate.getSchemeCode(), schemeFeatures);
                compFeaturesMap.put(candidate.getSchemeCode(), compFeatures);

                ScoredFeatureResult scoreResult = computeRecommendationScore(profile, scheme, verifiedData, compFeatures, !fallbackUsed);

                RecommendationExplanation explanation = buildExplanation(profile, scheme, scoreResult, candidate, compFeatures);
                List<String> plainReasons = buildPlainReasons(profile, scheme, scoreResult, compFeatures);

                RankedSchemeItem item = RankedSchemeItem.builder()
                        .schemeCode(candidate.getSchemeCode())
                        .slug(candidate.getSlug() != null ? candidate.getSlug() : scheme.getSlug())
                        .schemeTitle(candidate.getSchemeTitle() != null ? candidate.getSchemeTitle() : (scheme.getTitle() != null ? scheme.getTitle().getEnglish() : ""))
                        .shortDescription(scheme.getShortDescription() != null ? scheme.getShortDescription().getEnglish() : "")
                        .schemeLevel(candidate.getSchemeLevel() != null ? candidate.getSchemeLevel() : (scheme.getSchemeLevel() != null ? scheme.getSchemeLevel().name() : "CENTRAL"))
                        .stateOrUt(scheme.getStateOrUt())
                        .categoryCode(candidate.getCategoryCode() != null ? candidate.getCategoryCode() : (scheme.getCategory() != null ? scheme.getCategory().getCode() : ""))
                        .categoryName(candidate.getCategoryName() != null ? candidate.getCategoryName() : (scheme.getCategory() != null ? scheme.getCategory().getName() : ""))
                        .beneficiaryType(scheme.getBeneficiaryType())
                        .schemeType(scheme.getSchemeType())
                        .recommendationScore(scoreResult.finalScore())
                        .eligibilityStatus("ELIGIBLE")
                        .confidenceScore(candidate.getConfidenceScore() != null ? candidate.getConfidenceScore() : 1.0)
                        .passedConditions(candidate.getPassedConditions() != null ? candidate.getPassedConditions() : List.of())
                        .verifiedAttributesUsed(candidate.getVerifiedAttributesUsed() != null ? candidate.getVerifiedAttributesUsed() : List.of())
                        .explanation(explanation)
                        .reasons(plainReasons)
                        .build();

                scoredItems.add(item);

                // Compute shadow baseline if enabled
                if (properties.getMl().isShadowMode() && !fallbackUsed) {
                    ScoredFeatureResult detResult = computeRecommendationScore(profile, scheme, verifiedData, compFeatures, false);
                    RankedSchemeItem detItem = RankedSchemeItem.builder()
                            .schemeCode(item.getSchemeCode())
                            .recommendationScore(detResult.finalScore())
                            .build();
                    shadowDeterministicItems.add(detItem);
                }
            }
        } catch (Exception e) {
            log.error("Exception during ML ranking evaluation for userId={}. Falling back to deterministic scoring.", userId, e);
            fallbackUsed = true;
            rankingMethod = "DETERMINISTIC_FALLBACK";
            scoredItems.clear();
            for (EligibilityEvaluationResult candidate : eligibleCandidates) {
                Scheme scheme = schemeMap.get(candidate.getSchemeCode());
                if (scheme == null) continue;
                SchemeVerifiedData verifiedData = verifiedMap.get(candidate.getSchemeCode());
                SchemeFeatures schemeFeatures = schemeFeatureExtractor.extractFromScheme(scheme, verifiedData);
                ComparisonFeatures compFeatures = userSchemeComparisonService.compare(userFeatures, schemeFeatures);
                schemeFeaturesMap.put(candidate.getSchemeCode(), schemeFeatures);
                compFeaturesMap.put(candidate.getSchemeCode(), compFeatures);

                ScoredFeatureResult scoreResult = computeRecommendationScore(profile, scheme, verifiedData, compFeatures, false);
                RankedSchemeItem fallbackItem = RankedSchemeItem.builder()
                        .schemeCode(candidate.getSchemeCode())
                        .slug(candidate.getSlug() != null ? candidate.getSlug() : scheme.getSlug())
                        .schemeTitle(candidate.getSchemeTitle() != null ? candidate.getSchemeTitle() : (scheme.getTitle() != null ? scheme.getTitle().getEnglish() : candidate.getSchemeCode()))
                        .shortDescription(scheme.getShortDescription() != null ? scheme.getShortDescription().getEnglish() : "")
                        .schemeLevel(candidate.getSchemeLevel() != null ? candidate.getSchemeLevel() : (scheme.getSchemeLevel() != null ? scheme.getSchemeLevel().name() : "CENTRAL"))
                        .stateOrUt(scheme.getStateOrUt())
                        .categoryCode(candidate.getCategoryCode() != null ? candidate.getCategoryCode() : (scheme.getCategory() != null ? scheme.getCategory().getCode() : ""))
                        .categoryName(candidate.getCategoryName() != null ? candidate.getCategoryName() : (scheme.getCategory() != null ? scheme.getCategory().getName() : ""))
                        .beneficiaryType(scheme.getBeneficiaryType())
                        .schemeType(scheme.getSchemeType())
                        .recommendationScore(scoreResult.finalScore())
                        .eligibilityStatus("ELIGIBLE")
                        .confidenceScore(1.0)
                        .passedConditions(candidate.getPassedConditions() != null ? candidate.getPassedConditions() : List.of())
                        .verifiedAttributesUsed(candidate.getVerifiedAttributesUsed() != null ? candidate.getVerifiedAttributesUsed() : List.of())
                        .explanation(buildExplanation(profile, scheme, scoreResult, candidate, compFeatures))
                        .reasons(buildPlainReasons(profile, scheme, scoreResult, compFeatures))
                        .build();
                scoredItems.add(fallbackItem);
            }
        }

        // 5. Deterministic Sort: Score Descending, then Confidence Descending, then SchemeCode Ascending
        scoredItems.sort((a, b) -> {
            int scoreCmp = Double.compare(b.getRecommendationScore(), a.getRecommendationScore());
            if (scoreCmp != 0) {
                return scoreCmp;
            }
            if (a.getConfidenceScore() != null && b.getConfidenceScore() != null) {
                int confCmp = Double.compare(b.getConfidenceScore(), a.getConfidenceScore());
                if (confCmp != 0) return confCmp;
            }
            String codeA = a.getSchemeCode() != null ? a.getSchemeCode() : "";
            String codeB = b.getSchemeCode() != null ? b.getSchemeCode() : "";
            return codeA.compareTo(codeB);
        });

        // 6. Assign Continuous Sequential Rank (1-indexed) and attach UserSchemeFeatureVector
        for (int i = 0; i < scoredItems.size(); i++) {
            RankedSchemeItem item = scoredItems.get(i);
            int rank = i + 1;
            item.setRank(rank);

            SchemeFeatures sf = schemeFeaturesMap.get(item.getSchemeCode());
            ComparisonFeatures cf = compFeaturesMap.get(item.getSchemeCode());
            if (sf != null && cf != null) {
                UserSchemeFeatureVector vector = featureVectorBuilder.buildVector(
                        userFeatures,
                        sf,
                        cf,
                        rank,
                        item.getRecommendationScore(),
                        activeModelVersion
                );
                item.setFeatureVector(vector);
            }
        }

        // Shadow Mode Comparison Logging
        if (properties.getMl().isShadowMode() && !fallbackUsed && !shadowDeterministicItems.isEmpty()) {
            shadowDeterministicItems.sort((a, b) -> Double.compare(b.getRecommendationScore(), a.getRecommendationScore()));
            String topMl = scoredItems.stream().limit(3).map(RankedSchemeItem::getSchemeCode).collect(Collectors.joining(", "));
            String topDet = shadowDeterministicItems.stream().limit(3).map(RankedSchemeItem::getSchemeCode).collect(Collectors.joining(", "));
            log.info("SHADOW MODE COMPARISON: userId={}, Top3 ML=[{}], Top3 Deterministic=[{}]", userId, topMl, topDet);
        }

        // 7. Paginate
        int fromIndex = page * size;
        List<RankedSchemeItem> paginated;
        if (fromIndex >= scoredItems.size()) {
            paginated = List.of();
        } else {
            int toIndex = Math.min(fromIndex + size, scoredItems.size());
            paginated = scoredItems.subList(fromIndex, toIndex);
        }

        // Bulk-lookup user's existing applications for efficient O(1) matching on paginated items
        Map<String, Application> userAppsMap = new HashMap<>();
        if (applicationRepository != null && profile != null && profile.getUserId() != null) {
            try {
                List<Application> userApps = applicationRepository.findAllByUserId(profile.getUserId());
                if (userApps != null) {
                    for (Application app : userApps) {
                        if (app.getSchemeCode() != null) {
                            userAppsMap.putIfAbsent(app.getSchemeCode(), app);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Notice: Bulk application lookup notice: {}", e.getMessage());
            }
        }

        // Populate checklist and application details ONLY for the paginated slice
        for (RankedSchemeItem item : paginated) {
            Scheme scheme = schemeMap.get(item.getSchemeCode());
            SchemeVerifiedData verifiedData = verifiedMap.get(item.getSchemeCode());
            populateChecklistAndApplicationDetails(item, scheme, verifiedData, profile, userAppsMap);
        }

        long totalElapsedMs = System.currentTimeMillis() - startTimeMs;
        log.info("Phase 22B Recommendation complete: userId={}, returned={}, elapsed={}ms, method={}, fallback={}",
                userId, paginated.size(), totalElapsedMs, rankingMethod, fallbackUsed);

        return PersonalizedSchemeRecommendationResponse.builder()
                .userId(userId)
                .citizenState(profile.getState())
                .totalCatalogEvaluated(totalCatalogEvaluated)
                .eligibleCandidatesFound(eligibleCount)
                .totalRecommendationsReturned(paginated.size())
                .page(page)
                .size(size)
                .generatedAt(Instant.now())
                .modelFamily(MODEL_FAMILY)
                .modelVersion(activeModelVersion)
                .fallbackModel(FALLBACK_MODEL_VERSION)
                .rankingMethod(rankingMethod)
                .fallbackUsed(fallbackUsed)
                .recommendations(paginated)
                .build();
    }

    private ScoredFeatureResult computeRecommendationScore(
            CitizenProfile profile,
            Scheme scheme,
            boolean useSemanticMl
    ) {
        return computeRecommendationScore(profile, scheme, null, null, useSemanticMl);
    }

    private ScoredFeatureResult computeRecommendationScore(
            CitizenProfile profile,
            Scheme scheme,
            SchemeVerifiedData verifiedData,
            ComparisonFeatures compFeatures,
            boolean useSemanticMl
    ) {
        // Feature 1: Occupational Affinity (0.0 to 1.0)
        double fOcc = scoreOccupationalAffinity(profile, scheme, verifiedData, compFeatures);

        // Feature 2: Economic Need / Poverty Level (0.0 to 1.0)
        double fEcon = scoreEconomicNeed(profile, scheme, verifiedData, compFeatures);

        // Feature 3: Geographic Specificity (0.0 to 1.0)
        double fGeo = scoreGeographicSpecificity(profile, scheme);

        // Feature 4: Benefit Impact / Generosity (0.0 to 1.0)
        double fBenefit = scoreBenefitImpact(scheme);

        // Feature 5: Semantic Similarity (0.0 to 1.0)
        double fSemantic;
        if (useSemanticMl && semanticEmbeddingIndexService != null && semanticEmbeddingIndexService.isAvailable()) {
            fSemantic = semanticEmbeddingIndexService.computeSemanticSimilarity(scheme.getSchemeCode(), profile);
        } else {
            fSemantic = scoreSemanticRelevance(profile, scheme);
        }

        // Feature 6: Demographic Alignment (0.0 to 1.0)
        double fDemo = scoreDemographicAlignment(profile, scheme, verifiedData, compFeatures);

        // Configuration-driven weighted sum (Exact MADM Vector)
        double wOcc = (properties != null && properties.getWeights() != null) ? properties.getWeights().getOccupation() : 0.25;
        double wEcon = (properties != null && properties.getWeights() != null) ? properties.getWeights().getEconomic() : 0.25;
        double wGeo = (properties != null && properties.getWeights() != null) ? properties.getWeights().getGeographic() : 0.20;
        double wBenefit = (properties != null && properties.getWeights() != null) ? properties.getWeights().getBenefit() : 0.15;
        double wSemantic = (properties != null && properties.getWeights() != null) ? properties.getWeights().getSemantic() : 0.15;
        double wDemo = (properties != null && properties.getWeights() != null) ? properties.getWeights().getDemographic() : 0.0;
        if (properties != null && properties.getRanking() != null && properties.getRanking().isConfigured()) {
            wOcc = properties.getRanking().getOccupationWeight();
            wEcon = properties.getRanking().getIncomeWeight();
            wGeo = properties.getRanking().getStateWeight();
            wBenefit = properties.getRanking().getBenefitWeight();
            wSemantic = properties.getRanking().getSemanticWeight();
            wDemo = properties.getRanking().getAgeWeight() + properties.getRanking().getCategoryWeight() + properties.getRanking().getGenderWeight();
        }
        double totalW = wOcc + wEcon + wGeo + wBenefit + wSemantic + wDemo;
        if (totalW <= 0.0) totalW = 1.0;

        double rawScore = ((wOcc * fOcc) + (wEcon * fEcon) + (wGeo * fGeo) + (wBenefit * fBenefit) + (wSemantic * fSemantic) + (wDemo * fDemo)) / totalW;

        double clamped = Math.max(0.0, Math.min(1.0, rawScore));
        double rounded = BigDecimal.valueOf(clamped).setScale(4, RoundingMode.HALF_UP).doubleValue();

        return new ScoredFeatureResult(rounded, fOcc, fEcon, fGeo, fBenefit, fSemantic, fDemo);
    }

    private double scoreDemographicAlignment(
            CitizenProfile profile,
            Scheme scheme,
            SchemeVerifiedData verifiedData,
            ComparisonFeatures compFeatures
    ) {
        if (profile == null) return 0.0;

        double demoScore = 0.0;
        int evaluatedDimensions = 0;

        String benType = (scheme.getBeneficiaryType() != null ? scheme.getBeneficiaryType() : "").toUpperCase();
        String titleText = (scheme.getTitle() != null && scheme.getTitle().getEnglish() != null ? scheme.getTitle().getEnglish() : "").toUpperCase();
        String catName = (scheme.getCategory() != null && scheme.getCategory().getName() != null ? scheme.getCategory().getName() : "").toUpperCase();

        // 1. Gender dimension
        if (profile.getGender() != null && !profile.getGender().isBlank()) {
            evaluatedDimensions++;
            if (compFeatures != null && compFeatures.getGenderMatch() == FeatureMatchStatus.MATCH) {
                demoScore += 0.35;
            } else if ("FEMALE".equalsIgnoreCase(profile.getGender().trim()) &&
                    (benType.contains("WOMEN") || benType.contains("GIRL") || benType.contains("MAHILA") || titleText.contains("MAHILA") || titleText.contains("GIRL"))) {
                demoScore += 0.35;
            } else if (!benType.contains("WOMEN") && !benType.contains("GIRL") && !titleText.contains("MAHILA") && !titleText.contains("GIRL")) {
                demoScore += 0.35; // Compatible with gender-neutral scheme
            }
        }

        // 2. Social Category dimension
        if (profile.getSocialCategory() != null && !profile.getSocialCategory().isBlank()) {
            evaluatedDimensions++;
            String cat = profile.getSocialCategory().trim().toUpperCase();
            if (compFeatures != null && compFeatures.getCategoryMatch() == FeatureMatchStatus.MATCH) {
                demoScore += 0.35;
            } else if (catName.contains(cat) || benType.contains(cat) || titleText.contains(cat)) {
                demoScore += 0.35;
            } else if (!catName.contains("SC") && !catName.contains("ST") && !catName.contains("TRIBAL") && !titleText.contains("SC/") && !titleText.contains("ST/")) {
                demoScore += 0.35; // Compatible with general non-reserved scheme
            }
        }

        // 3. Age dimension
        if (profile.getAge() != null && profile.getAge() > 0) {
            evaluatedDimensions++;
            int age = profile.getAge();
            if (compFeatures != null && compFeatures.getAgeMatch() == FeatureMatchStatus.MATCH) {
                demoScore += 0.30;
            } else if (age >= 60 && (benType.contains("SENIOR") || benType.contains("PENSION") || titleText.contains("OLD AGE") || titleText.contains("SENIOR"))) {
                demoScore += 0.30;
            } else if (age <= 25 && (benType.contains("YOUTH") || benType.contains("STUDENT") || titleText.contains("YOUTH") || titleText.contains("SCHOLARSHIP"))) {
                demoScore += 0.30;
            } else if (age >= 18 && age < 60 && !benType.contains("SENIOR") && !titleText.contains("OLD AGE")) {
                demoScore += 0.30; // Working age citizen compatible with general adult scheme
            }
        }

        if (evaluatedDimensions == 0) {
            return 0.0; // Missing demographic data receives 0.0
        }

        return Math.min(1.0, demoScore);
    }

    private List<String> buildPlainReasons(
            CitizenProfile profile,
            Scheme scheme,
            ScoredFeatureResult score,
            ComparisonFeatures compFeatures
    ) {
        List<String> reasons = new ArrayList<>();
        // Invariant: Mandatory statutory eligibility provenance statement
        reasons.add("Statutory eligibility was confirmed by the EligibilityEngine; the recommendation model is used only for ranking among eligible schemes.");

        if (score.fGeo() >= 0.90 && scheme.getStateOrUt() != null && profile.getState() != null
                && scheme.getStateOrUt().equalsIgnoreCase(profile.getState().trim())) {
            reasons.add("Recommended because this scheme is specifically tailored for residents of " + scheme.getStateOrUt() + ".");
        } else if (scheme.getSchemeLevel() == SchemeLevel.CENTRAL) {
            reasons.add("Recommended as a National Central Government welfare scheme open to eligible citizens across India.");
        }
        if (score.fOcc() >= 0.80 && profile.getOccupation() != null && !profile.getOccupation().isBlank()) {
            reasons.add("Recommended because the scheme provides targeted welfare benefits aligned with your occupation as " + profile.getOccupation() + ".");
        }
        if (score.fEcon() >= 0.70 && (profile.getAnnualIncome() != null || Boolean.TRUE.equals(profile.getBplStatus()))) {
            reasons.add("Recommended because the scheme prioritizes financial assistance aligned with your income and welfare bracket.");
        }

        if (compFeatures != null) {
            if (compFeatures.getAgeMatch() == FeatureMatchStatus.MATCH && profile.getAge() != null) {
                reasons.add("Age satisfies the statutory scheme age criteria.");
            }
            if (compFeatures.getIncomeMatch() == FeatureMatchStatus.MATCH && profile.getAnnualIncome() != null) {
                reasons.add("Income falls within the statutory scheme threshold.");
            }
            if (compFeatures.getOccupationMatch() == FeatureMatchStatus.MATCH && profile.getOccupation() != null && !profile.getOccupation().isBlank()) {
                reasons.add("Occupation matches targeted welfare criteria for " + profile.getOccupation() + ".");
            }
        }

        return reasons;
    }

    private double scoreOccupationalAffinity(
            CitizenProfile profile,
            Scheme scheme,
            SchemeVerifiedData verifiedData,
            ComparisonFeatures compFeatures
    ) {
        if (compFeatures != null && compFeatures.getOccupationMatch() == FeatureMatchStatus.MATCH) {
            return 1.0;
        }
        if (compFeatures != null && compFeatures.getOccupationMatch() == FeatureMatchStatus.MISMATCH) {
            return 0.0;
        }

        String occ = profile.getOccupation() != null ? profile.getOccupation().trim().toLowerCase() : "";
        boolean hasOccInfo = !occ.isEmpty() || Boolean.TRUE.equals(profile.getIsFarmer()) || Boolean.TRUE.equals(profile.getIsStudent());
        if (!hasOccInfo) {
            return 0.0; // Missing occupation receives 0.0 points
        }

        String cat = (scheme.getCategory() != null && scheme.getCategory().getCode() != null) ?
                scheme.getCategory().getCode().toLowerCase() : "";
        String text = (scheme.getTitle() != null && scheme.getTitle().getEnglish() != null ? scheme.getTitle().getEnglish() : "") + " " +
                      (scheme.getShortDescription() != null && scheme.getShortDescription().getEnglish() != null ? scheme.getShortDescription().getEnglish() : "");
        text = text.toLowerCase();

        Map<String, Object> struct = (verifiedData != null && verifiedData.getEligibility() != null)
                ? verifiedData.getEligibility().getStructuredEligibility() : null;

        // Farmer alignment
        boolean isFarmer = Boolean.TRUE.equals(profile.getIsFarmer()) || occ.contains("farmer") || occ.contains("agriculture") || occ.contains("cultivator") || occ.contains("kisan");
        if (isFarmer && ((struct != null && Boolean.TRUE.equals(struct.get("farmerStatus"))) || cat.contains("agri") || text.contains("kisan") || text.contains("farmer") || text.contains("crop") || text.contains("cultivator") || text.contains("irrigation"))) {
            return 1.0;
        }

        // Student alignment
        boolean isStudent = "student".equals(occ) || Boolean.TRUE.equals(profile.getIsStudent());
        if (isStudent && ((struct != null && Boolean.TRUE.equals(struct.get("studentStatus"))) || cat.contains("edu") || text.contains("scholarship") || text.contains("student") || text.contains("tuition") || text.contains("fellowship"))) {
            return 1.0;
        }

        // Artisan / Craftsperson alignment
        boolean isArtisan = occ.contains("artisan") || occ.contains("craft") || occ.contains("handicraft") || occ.contains("handloom") || occ.contains("weaver") || occ.contains("shg");
        if (isArtisan && (cat.contains("handicraft") || cat.contains("handloom") || cat.contains("craft") || text.contains("artisan") || text.contains("handicraft") || text.contains("handloom") || text.contains("weaver") || text.contains("khadi") || text.contains("shg") || text.contains("microfinance"))) {
            return 1.0;
        }

        // Unorganized Labour / Construction Worker alignment
        boolean isLabour = occ.contains("construction") || occ.contains("labour") || occ.contains("labor") || occ.contains("worker") || occ.contains("shramik");
        if (isLabour && (cat.contains("labour") || cat.contains("labor") || cat.contains("worker") || text.contains("shramik") || text.contains("construction") || text.contains("building and other") || text.contains("bocw") || text.contains("unorganized"))) {
            return 1.0;
        }

        // Self-Employed / Entrepreneur / MSME alignment
        boolean isSelfEmployed = occ.contains("self-employed") || occ.contains("business") || occ.contains("entrepreneur") || occ.contains("vendor") || occ.contains("unemployed");
        if (isSelfEmployed && (cat.contains("business") || cat.contains("entrepreneur") || text.contains("self-employed") || text.contains("self employment") || text.contains("msme") || text.contains("mudra") || text.contains("startup") || text.contains("svanidhi") || text.contains("micro enterprise"))) {
            return 1.0;
        }

        // Disability alignment
        if (Boolean.TRUE.equals(profile.getDisabilityStatus()) && (cat.contains("disab") || text.contains("pwd") || text.contains("divyang") || text.contains("assistive"))) {
            return 1.0;
        }

        // General occupation keyword match
        if (!occ.isEmpty() && (text.contains(occ) || cat.contains(occ))) {
            return 1.0;
        }

        // Social Category / EWS alignment
        if ("EWS".equalsIgnoreCase(profile.getSocialCategory()) || "BPL".equalsIgnoreCase(profile.getSocialCategory())) {
            return 0.85;
        }

        return 0.0;
    }

    private double scoreOccupationalAffinity(CitizenProfile profile, Scheme scheme) {
        return scoreOccupationalAffinity(profile, scheme, null, null);
    }

    private double scoreEconomicNeed(
            CitizenProfile profile,
            Scheme scheme,
            SchemeVerifiedData verifiedData,
            ComparisonFeatures compFeatures
    ) {
        // Missing income/BPL data produces 0.0 (do not award points for missing data)
        if (profile == null || (profile.getAnnualIncome() == null && profile.getBplStatus() == null)) {
            return 0.0;
        }

        if (compFeatures != null && compFeatures.getIncomeMatch() == FeatureMatchStatus.MISMATCH) {
            return 0.0; // Income violates scheme ceiling
        }

        double score = 0.50;

        // BPL Priority
        if (Boolean.TRUE.equals(profile.getBplStatus())) {
            score += 0.30;
        }

        // Lower income proportion
        Double income = profile.getAnnualIncome();
        if (income != null && income > 0) {
            double incomeRatio = Math.min(1.0, income / 300000.0);
            score += 0.20 * (1.0 - incomeRatio);
        } else {
            score += 0.10;
        }

        return Math.min(1.0, score);
    }

    private double scoreEconomicNeed(CitizenProfile profile, Scheme scheme) {
        return scoreEconomicNeed(profile, scheme, null, null);
    }

    private double scoreGeographicSpecificity(CitizenProfile profile, Scheme scheme) {
        if (scheme.getSchemeLevel() == SchemeLevel.STATE) {
            if (scheme.getStateOrUt() != null && profile.getState() != null) {
                String sState = scheme.getStateOrUt().trim().replace("_", " ").replaceAll("\\s+", " ");
                String cState = profile.getState().trim().replace("_", " ").replaceAll("\\s+", " ");
                if (sState.equalsIgnoreCase(cState) || sState.replace(" ", "").equalsIgnoreCase(cState.replace(" ", ""))) {
                    return 1.0; // State-tailored local scheme
                }
            }
            return 0.0; // State mismatch or missing citizen state receives 0.0
        }
        return 0.85; // Central National scheme
    }

    private double scoreBenefitImpact(Scheme scheme) {
        if (scheme.getBenefits() != null && !scheme.getBenefits().isEmpty()) {
            return 0.90; // Rich direct structured benefits
        }
        return 0.70;
    }

    private double scoreSemanticRelevance(CitizenProfile profile, Scheme scheme) {
        Set<String> queryTokens = new HashSet<>();
        if (profile.getOccupation() != null) queryTokens.addAll(tokenize(profile.getOccupation()));
        if (profile.getState() != null) queryTokens.addAll(tokenize(profile.getState()));
        if (profile.getSocialCategory() != null) queryTokens.addAll(tokenize(profile.getSocialCategory()));

        Set<String> docTokens = new HashSet<>();
        if (scheme.getTitle() != null && scheme.getTitle().getEnglish() != null) {
            docTokens.addAll(tokenize(scheme.getTitle().getEnglish()));
        }
        if (scheme.getShortDescription() != null && scheme.getShortDescription().getEnglish() != null) {
            docTokens.addAll(tokenize(scheme.getShortDescription().getEnglish()));
        }
        if (scheme.getTags() != null) {
            for (String tag : scheme.getTags()) {
                docTokens.addAll(tokenize(tag));
            }
        }

        if (queryTokens.isEmpty() || docTokens.isEmpty()) {
            return 0.0;
        }

        long matchedCount = queryTokens.stream().filter(docTokens::contains).count();
        if (matchedCount >= 2) return 1.0;
        if (matchedCount == 1) return 0.75;
        return 0.0;
    }

    private List<String> tokenize(String text) {
        if (text == null) return List.of();
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(s -> s.length() > 2)
                .toList();
    }

    private RecommendationExplanation buildExplanation(
            CitizenProfile profile,
            Scheme scheme,
            ScoredFeatureResult score,
            EligibilityEvaluationResult candidate,
            ComparisonFeatures compFeatures
    ) {
        List<String> factors = new ArrayList<>();
        // Invariant: Explicitly identify EligibilityEngine as statutory authority
        factors.add("Statutory eligibility was confirmed by the EligibilityEngine; the recommendation model is used only for ranking among eligible schemes.");

        if (profile.getOccupation() != null) factors.add("Occupation: " + profile.getOccupation());
        if (profile.getState() != null) factors.add("State: " + profile.getState());
        if (profile.getAnnualIncome() != null) factors.add(String.format("Annual Income: ₹%,.0f", profile.getAnnualIncome()));
        if (profile.getSocialCategory() != null) factors.add("Social Category: " + profile.getSocialCategory());
        if (Boolean.TRUE.equals(profile.getBplStatus())) factors.add("BPL Status: Priority Beneficiary");

        if (compFeatures != null) {
            if (compFeatures.getAgeMatch() == FeatureMatchStatus.MATCH && profile.getAge() != null) {
                factors.add("Age satisfies scheme age criteria (" + profile.getAge() + " years).");
            }
            if (compFeatures.getIncomeMatch() == FeatureMatchStatus.MATCH) {
                factors.add("Income falls within the statutory scheme threshold.");
            }
            if (compFeatures.getStateMatch() == FeatureMatchStatus.MATCH && profile.getState() != null) {
                factors.add("State matches scheme jurisdiction (" + profile.getState() + ").");
            }
            if (compFeatures.getOccupationMatch() == FeatureMatchStatus.MATCH && profile.getOccupation() != null) {
                factors.add("Occupation matches targeted welfare criteria (" + profile.getOccupation() + ").");
            }
            if (compFeatures.getCategoryMatch() == FeatureMatchStatus.MATCH && profile.getSocialCategory() != null) {
                factors.add("Category matches eligible reservation criteria (" + profile.getSocialCategory() + ").");
            }
            if (compFeatures.getGenderMatch() == FeatureMatchStatus.MATCH && profile.getGender() != null) {
                factors.add("Gender matches target beneficiary criteria (" + profile.getGender() + ").");
            }
            if (compFeatures.getDisabilityMatch() == FeatureMatchStatus.MATCH) {
                factors.add("Disability status satisfies scheme criteria.");
            }
        }

        String primaryReason = generatePrimaryReason(profile, scheme, score, compFeatures);
        String geoMatch = scheme.getSchemeLevel() == SchemeLevel.CENTRAL ?
                "National Central Scheme open across India." :
                "State Scheme specifically tailored for residents of " + scheme.getStateOrUt() + ".";

        String benefitRelevance = "Direct welfare assistance aligned with your verified citizen profile.";
        if (scheme.getBenefits() != null && !scheme.getBenefits().isEmpty()) {
            benefitRelevance = scheme.getBenefits().get(0).getDescription() != null && scheme.getBenefits().get(0).getDescription().getEnglish() != null ?
                    scheme.getBenefits().get(0).getDescription().getEnglish() : "Direct financial & welfare benefit.";
        }

        String semanticRelevance = score.fSemantic() >= 0.85 ?
                "High semantic relevance with profile keywords and welfare category." :
                "Standard semantic alignment with citizen profile domain.";

        return RecommendationExplanation.builder()
                .primaryReason(primaryReason)
                .matchedProfileFactors(factors)
                .benefitRelevance(benefitRelevance)
                .categoryMatch(candidate.getCategoryName() != null ? candidate.getCategoryName() : "General Welfare")
                .geographicMatch(geoMatch)
                .beneficiaryMatch(scheme.getBeneficiaryType() != null ? scheme.getBeneficiaryType() : "INDIVIDUAL")
                .semanticRelevance(semanticRelevance)
                .build();
    }

    private String generatePrimaryReason(
            CitizenProfile profile,
            Scheme scheme,
            ScoredFeatureResult score,
            ComparisonFeatures compFeatures
    ) {
        String statutoryPrefix = "Statutory eligibility was confirmed by the EligibilityEngine; ";
        if (score.fOcc() >= 0.90 && profile.getOccupation() != null) {
            return statutoryPrefix + "ranked highly based on occupational alignment as " + profile.getOccupation() + " and targeted welfare benefits.";
        }
        if (score.fEcon() >= 0.80) {
            return statutoryPrefix + "prioritized by ranking model due to economic need profile and income tier.";
        }
        if (score.fGeo() >= 0.95 && profile.getState() != null) {
            return statutoryPrefix + "tailored specifically for residents of " + profile.getState() + ".";
        }
        return statutoryPrefix + "the recommendation model is used only for ranking among eligible schemes based on demographic alignment.";
    }

    private void populateChecklistAndApplicationDetails(
            RankedSchemeItem item,
            Scheme scheme,
            SchemeVerifiedData verifiedData,
            CitizenProfile profile,
            Map<String, Application> userAppsMap
    ) {
        if (item == null || scheme == null) return;

        DocumentChecklistGenerator generator = this.documentChecklistGenerator;
        if (generator == null) {
            generator = new DocumentChecklistGenerator(schemeDocumentRequirementResolver, schemeVerifiedDataRepository, applicationRepository);
        }

        SchemeDocumentChecklistResponse checklist = generator.generateChecklist(profile, scheme, verifiedData, null);

        List<String> docNames = checklist.getItems() != null ?
                checklist.getItems().stream().map(DocumentChecklistItemResponse::getDocumentName).collect(Collectors.toList()) :
                List.of();

        item.setRequiredDocuments(docNames);
        item.setChecklist(checklist.getItems());
        item.setMissingDocuments(checklist.getMissingRequirements());
        item.setApplicationSteps(checklist.getApplicationSteps());
        item.setApplicationUrl(checklist.getOfficialApplicationUrl());
        item.setBenefits(checklist.getBenefits());
        item.setDepartment(scheme.getDepartment() != null ? scheme.getDepartment() : scheme.getStateOrUt());
        item.setMinistry(scheme.getMinistry() != null ? scheme.getMinistry() : "Government of India");

        String appStatus = "NOT_APPLIED";
        String appId = null;
        if (userAppsMap != null && scheme.getSchemeCode() != null && userAppsMap.containsKey(scheme.getSchemeCode())) {
            Application app = userAppsMap.get(scheme.getSchemeCode());
            if (app != null) {
                appStatus = app.getStatus() != null ? app.getStatus().name() : "APPLIED";
                appId = app.getId();
            }
        } else if (applicationRepository != null && profile != null && profile.getUserId() != null && scheme.getSchemeCode() != null) {
            try {
                var appOpt = applicationRepository.findByUserIdAndSchemeCode(profile.getUserId(), scheme.getSchemeCode());
                if (appOpt.isPresent()) {
                    appStatus = appOpt.get().getStatus() != null ? appOpt.get().getStatus().name() : "APPLIED";
                    appId = appOpt.get().getId();
                }
            } catch (Exception e) {
                log.debug("Notice: Application lookup notice: {}", e.getMessage());
            }
        }
        item.setApplicationStatus(appStatus);
        item.setApplicationId(appId);
    }

    private void populateChecklistAndApplicationDetails(RankedSchemeItem item, Scheme scheme, SchemeVerifiedData verifiedData, CitizenProfile profile) {
        populateChecklistAndApplicationDetails(item, scheme, verifiedData, profile, null);
    }

    private void populateChecklistAndApplicationDetails(RankedSchemeItem item, Scheme scheme, SchemeVerifiedData verifiedData) {
        populateChecklistAndApplicationDetails(item, scheme, verifiedData, null, null);
    }

    public record ScoredFeatureResult(double finalScore, double fOcc, double fEcon, double fGeo, double fBenefit, double fSemantic, double fDemo) {
        public ScoredFeatureResult(double finalScore, double fOcc, double fEcon, double fGeo, double fBenefit, double fSemantic) {
            this(finalScore, fOcc, fEcon, fGeo, fBenefit, fSemantic, 0.50);
        }
    }
}
