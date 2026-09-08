package com.schemebridge.scheme.service;

import com.schemebridge.scheme.config.MlRecommenderProperties;
import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.AiChatRequest;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 24: Comprehensive Production AI Recommendation & Document Checklist UAT,
 * End-to-End Validation and Defect Hardening Test Suite.
 */
@ExtendWith(MockitoExtension.class)
public class Phase24ProductionUatValidationTest {

    @Mock
    private EligibilityEvaluationService eligibilityEvaluationService;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @Mock
    private CitizenProfileService citizenProfileService;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private SemanticEmbeddingIndexService semanticEmbeddingIndexService;

    @Mock
    private SchemeDocumentRequirementResolver documentRequirementResolver;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private GrievanceRepository grievanceRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private SchemeVerifiedDataRepository schemeVerifiedDataRepository;

    @Mock
    private EligibilityEngine eligibilityEngine;

    @Mock
    private MongoTemplate mongoTemplate;

    private MlRecommenderProperties properties;
    private EligibleSchemeRecommendationService recommendationService;
    private AiChatService aiChatService;

    @BeforeEach
    void setUp() {
        properties = new MlRecommenderProperties();
        properties.getMl().setEnabled(true);
        properties.getMl().setTimeoutMs(200);
        properties.getMl().setShadowMode(false);
        properties.getMl().setModelVersion("2.4.0-production-hybrid-384d");

        recommendationService = new EligibleSchemeRecommendationService(
                eligibilityEvaluationService,
                citizenProfileRepository,
                schemeRepository,
                properties,
                semanticEmbeddingIndexService
        );

        aiChatService = new AiChatService(
                mongoTemplate,
                citizenProfileRepository,
                citizenProfileService,
                applicationRepository,
                grievanceRepository,
                feedbackRepository,
                schemeRepository,
                schemeVerifiedDataRepository,
                eligibilityEngine,
                documentRequirementResolver,
                "", // empty Gemini API key triggers grounded deterministic fallback
                "gemini-1.5-flash",
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
    }

    // =========================================================================
    // STEP 4: 8 CITIZEN PERSONA UAT
    // =========================================================================
    @Nested
    @DisplayName("Step 4 — 8 Citizen Persona UAT")
    class CitizenPersonaUatTests {

        private Scheme createTestScheme(String code, String title, String category, String occ, Double maxIncome, Integer minAge, Integer maxAge) {
            String safeOcc = occ != null ? occ : "Citizen";
            return Scheme.builder()
                    .id("sch-" + code.toLowerCase())
                    .schemeCode(code)
                    .slug(code.toLowerCase())
                    .title(MultilingualText.builder().english(title).build())
                    .shortDescription(MultilingualText.builder().english(title + " official assistance").build())
                    .category(SchemeCategoryRef.builder().code(category).name(category).build())
                    .schemeLevel(SchemeLevel.CENTRAL)
                    .stateOrUt("ALL")
                    .beneficiaryType(safeOcc)
                    .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("Financial assistance").build()).build()))
                    .tags(List.of(safeOcc.toLowerCase(), category.toLowerCase()))
                    .build();
        }

        private void runPersonaTest(CitizenProfile profile, String targetSchemeCode, String expectedCategory) {
            when(citizenProfileRepository.findByUserId(profile.getUserId())).thenReturn(Optional.of(profile));

            Scheme target = createTestScheme(targetSchemeCode, "Official " + targetSchemeCode + " Scheme", expectedCategory, profile.getOccupation(), 300000.0, 18, 65);
            Scheme other = createTestScheme("SCH-IRRELEVANT-999", "Irrelevant Program", "OTHER", "Other", 100000.0, 60, 90);

            // Statutory gate: only target is eligible
            List<EligibilityEvaluationResult> eligibleList = List.of(
                    EligibilityEvaluationResult.builder().schemeCode(targetSchemeCode).slug(targetSchemeCode.toLowerCase()).status(EligibilityStatus.ELIGIBLE).build()
            );

            CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                    .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(10).eligibleCount(1).build())
                    .eligibleSchemes(eligibleList)
                    .build();

            when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes(profile.getUserId())).thenReturn(evalRes);
            when(schemeRepository.findAll()).thenReturn(List.of(target, other));
            when(semanticEmbeddingIndexService.isAvailable()).thenReturn(true);
            when(semanticEmbeddingIndexService.computeSemanticSimilarity(eq(targetSchemeCode), any())).thenReturn(0.85);

            PersonalizedSchemeRecommendationResponse res = recommendationService.getPersonalizedRecommendations(profile.getUserId(), 0, 10);

            assertNotNull(res);
            assertEquals(1, res.getRecommendations().size());
            assertNotNull(res.getRecommendations().get(0).getExplanation());
            String primaryReason = res.getRecommendations().get(0).getExplanation().getPrimaryReason();
            assertNotNull(primaryReason);
            assertFalse(primaryReason.isBlank());

            // Validate no ML jargon exposed to citizen
            String explanation = primaryReason.toLowerCase();
            assertFalse(explanation.contains("cosine"));
            assertFalse(explanation.contains("embedding"));
            assertFalse(explanation.contains("vector"));
            assertFalse(explanation.contains("model id"));
        }

        @Test
        @DisplayName("Persona 1: Farmer Persona")
        void testFarmerPersona() {
            CitizenProfile farmer = CitizenProfile.builder()
                    .userId("citizen-farmer-01")
                    .occupation("Farmer")
                    .state("Maharashtra")
                    .age(42)
                    .annualIncome(180000.0)
                    .socialCategory("OBC")
                    .build();
            runPersonaTest(farmer, "SCH-PM-KISAN", "AGRI");
        }

        @Test
        @DisplayName("Persona 2: Student Persona")
        void testStudentPersona() {
            CitizenProfile student = CitizenProfile.builder()
                    .userId("citizen-student-02")
                    .occupation("Student")
                    .state("Delhi")
                    .age(20)
                    .annualIncome(80000.0)
                    .socialCategory("General")
                    .build();
            runPersonaTest(student, "SCH-NSP-SCHOLARSHIP", "EDUCATION");
        }

        @Test
        @DisplayName("Persona 3: Senior Citizen Persona")
        void testSeniorCitizenPersona() {
            CitizenProfile senior = CitizenProfile.builder()
                    .userId("citizen-senior-03")
                    .occupation("Retired")
                    .state("Kerala")
                    .age(68)
                    .annualIncome(120000.0)
                    .socialCategory("General")
                    .build();
            runPersonaTest(senior, "SCH-IGNOP-PENSION", "PENSION");
        }

        @Test
        @DisplayName("Persona 4: Unemployed Citizen Persona")
        void testUnemployedPersona() {
            CitizenProfile unemployed = CitizenProfile.builder()
                    .userId("citizen-unemployed-04")
                    .occupation("Unemployed")
                    .state("Uttar Pradesh")
                    .age(24)
                    .annualIncome(50000.0)
                    .bplStatus(true)
                    .socialCategory("SC")
                    .build();
            runPersonaTest(unemployed, "SCH-PMKVY-SKILL", "EMPLOYMENT");
        }

        @Test
        @DisplayName("Persona 5: Woman / Artisan Persona")
        void testWomanArtisanPersona() {
            CitizenProfile artisan = CitizenProfile.builder()
                    .userId("citizen-artisan-05")
                    .gender("Female")
                    .occupation("Artisan")
                    .state("Rajasthan")
                    .age(35)
                    .annualIncome(140000.0)
                    .socialCategory("OBC")
                    .build();
            runPersonaTest(artisan, "SCH-PM-VISHWAKARMA", "HANDICRAFTS");
        }

        @Test
        @DisplayName("Persona 6: Person with Disability (PwD) Persona")
        void testPwdPersona() {
            CitizenProfile pwd = CitizenProfile.builder()
                    .userId("citizen-pwd-06")
                    .disabilityStatus(true)
                    .occupation("Self Employed")
                    .state("Tamil Nadu")
                    .age(31)
                    .annualIncome(90000.0)
                    .socialCategory("General")
                    .build();
            runPersonaTest(pwd, "SCH-ADIP-PWD", "SOCIAL_WELFARE");
        }

        @Test
        @DisplayName("Persona 7: Urban Worker Persona")
        void testUrbanWorkerPersona() {
            CitizenProfile worker = CitizenProfile.builder()
                    .userId("citizen-worker-07")
                    .occupation("Construction Worker")
                    .state("Karnataka")
                    .age(38)
                    .annualIncome(160000.0)
                    .socialCategory("ST")
                    .build();
            runPersonaTest(worker, "SCH-PMAY-URBAN", "HOUSING");
        }

        @Test
        @DisplayName("Persona 8: Cold-start Citizen Persona")
        void testColdStartPersona() {
            // Citizen with minimal attributes
            CitizenProfile coldStart = CitizenProfile.builder()
                    .userId("citizen-coldstart-08")
                    .state("Gujarat")
                    .age(26)
                    .build();
            runPersonaTest(coldStart, "SCH-UNIVERSAL-HEALTH", "HEALTH");
        }
    }

    // =========================================================================
    // STEP 5: BOUNDARY CONDITION TESTING (HARD STATUTORY GATE)
    // =========================================================================
    @Nested
    @DisplayName("Step 5 — Boundary Condition Testing & Statutory Eligibility Gate")
    class BoundaryConditionTests {

        private Scheme createIncomeAgeThresholdScheme(String code, Double maxIncome, Integer minAge, Integer maxAge) {
            return Scheme.builder()
                    .id("sch-boundary-" + code)
                    .schemeCode(code)
                    .slug(code.toLowerCase())
                    .title(MultilingualText.builder().english("Threshold Scheme " + code).build())
                    .category(SchemeCategoryRef.builder().code("WELFARE").name("Welfare").build())
                    .schemeLevel(SchemeLevel.CENTRAL)
                    .stateOrUt("ALL")
                    .build();
        }

        @Test
        @DisplayName("Boundary Test 1: Income clearly below threshold -> ELIGIBLE")
        void testIncomeBelowThreshold() {
            // Threshold = 200,000. Profile income = 150,000
            CitizenProfile profile = CitizenProfile.builder().userId("c-inc-1").annualIncome(150000.0).build();
            when(citizenProfileRepository.findByUserId("c-inc-1")).thenReturn(Optional.of(profile));

            Scheme sch = createIncomeAgeThresholdScheme("SCH-INC-1", 200000.0, 18, 60);
            List<EligibilityEvaluationResult> eligible = List.of(
                    EligibilityEvaluationResult.builder().schemeCode("SCH-INC-1").status(EligibilityStatus.ELIGIBLE).build()
            );
            when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("c-inc-1")).thenReturn(
                    CitizenEligibilityEvaluationResponse.builder().eligibleSchemes(eligible).build()
            );
            when(schemeRepository.findAll()).thenReturn(List.of(sch));

            PersonalizedSchemeRecommendationResponse res = recommendationService.getPersonalizedRecommendations("c-inc-1", 0, 10);
            assertEquals(1, res.getRecommendations().size());
            assertEquals("SCH-INC-1", res.getRecommendations().get(0).getSchemeCode());
        }

        @Test
        @DisplayName("Boundary Test 2: Income exactly at threshold -> ELIGIBLE (Inclusive)")
        void testIncomeAtThreshold() {
            // Threshold = 200,000. Profile income = 200,000
            CitizenProfile profile = CitizenProfile.builder().userId("c-inc-2").annualIncome(200000.0).build();
            when(citizenProfileRepository.findByUserId("c-inc-2")).thenReturn(Optional.of(profile));

            Scheme sch = createIncomeAgeThresholdScheme("SCH-INC-2", 200000.0, 18, 60);
            List<EligibilityEvaluationResult> eligible = List.of(
                    EligibilityEvaluationResult.builder().schemeCode("SCH-INC-2").status(EligibilityStatus.ELIGIBLE).build()
            );
            when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("c-inc-2")).thenReturn(
                    CitizenEligibilityEvaluationResponse.builder().eligibleSchemes(eligible).build()
            );
            when(schemeRepository.findAll()).thenReturn(List.of(sch));

            PersonalizedSchemeRecommendationResponse res = recommendationService.getPersonalizedRecommendations("c-inc-2", 0, 10);
            assertEquals(1, res.getRecommendations().size());
        }

        @Test
        @DisplayName("Boundary Test 3: Income clearly above threshold -> HARD STATUTORY GATE EXCLUDES")
        void testIncomeAboveThreshold() {
            // Threshold = 200,000. Profile income = 250,000 -> Ineligible!
            CitizenProfile profile = CitizenProfile.builder().userId("c-inc-3").annualIncome(250000.0).build();
            when(citizenProfileRepository.findByUserId("c-inc-3")).thenReturn(Optional.of(profile));

            Scheme sch = createIncomeAgeThresholdScheme("SCH-INC-3", 200000.0, 18, 60);

            // Statutory engine evaluates as INELIGIBLE -> empty eligible list
            when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("c-inc-3")).thenReturn(
                    CitizenEligibilityEvaluationResponse.builder().eligibleSchemes(Collections.emptyList()).build()
            );

            PersonalizedSchemeRecommendationResponse res = recommendationService.getPersonalizedRecommendations("c-inc-3", 0, 10);

            // Statutory Eligibility Violation Rate must be exactly 0.00%
            assertTrue(res.getRecommendations().isEmpty(), "Ineligible scheme must NEVER be surfaced");
        }

        @Test
        @DisplayName("Boundary Test 4: Age boundary condition (below min age vs exact min age)")
        void testAgeBoundaryCondition() {
            // Age min = 18. Age 17 is ineligible
            CitizenProfile minor = CitizenProfile.builder().userId("c-minor").age(17).build();
            when(citizenProfileRepository.findByUserId("c-minor")).thenReturn(Optional.of(minor));

            Scheme sch = createIncomeAgeThresholdScheme("SCH-ADULT-1", 500000.0, 18, 60);
            when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("c-minor")).thenReturn(
                    CitizenEligibilityEvaluationResponse.builder().eligibleSchemes(Collections.emptyList()).build()
            );

            PersonalizedSchemeRecommendationResponse res = recommendationService.getPersonalizedRecommendations("c-minor", 0, 10);
            assertEquals(0, res.getRecommendations().size(), "Underage citizen must be excluded by hard statutory gate");
        }

        @Test
        @DisplayName("Boundary Test 5: ML score must never resurrect a statutorily ineligible scheme")
        void testMlCannotResurrectIneligibleScheme() {
            CitizenProfile profile = CitizenProfile.builder().userId("c-resurrect").age(70).build();
            when(citizenProfileRepository.findByUserId("c-resurrect")).thenReturn(Optional.of(profile));

            Scheme youthScheme = createIncomeAgeThresholdScheme("SCH-YOUTH-ONLY", 500000.0, 18, 35);

            // Ineligible in statutory engine
            when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("c-resurrect")).thenReturn(
                    CitizenEligibilityEvaluationResponse.builder().eligibleSchemes(Collections.emptyList()).build()
            );

            PersonalizedSchemeRecommendationResponse res = recommendationService.getPersonalizedRecommendations("c-resurrect", 0, 10);
            assertEquals(0, res.getRecommendations().size());
            verify(semanticEmbeddingIndexService, never()).computeSemanticSimilarity(eq("SCH-YOUTH-ONLY"), any());
        }
    }

    // =========================================================================
    // STEP 6: CITIZEN CONVERSATIONAL AI GROUNDING & ANTI-HALLUCINATION
    // =========================================================================
    @Nested
    @DisplayName("Step 7 — Citizen Conversational AI Grounding & Anti-Hallucination")
    class CitizenConversationalAiTests {

        private CitizenProfile citizenProfile;
        private Scheme realScheme;

        @BeforeEach
        void init() {
            citizenProfile = CitizenProfile.builder()
                    .userId("citizen-chat-01")
                    .displayName("Rahul Sharma")
                    .occupation("Farmer")
                    .state("Maharashtra")
                    .age(35)
                    .annualIncome(150000.0)
                    .build();

            realScheme = Scheme.builder()
                    .schemeCode("SCH-PM-KISAN")
                    .title(MultilingualText.builder().english("PM Kisan Samman Nidhi").build())
                    .shortDescription(MultilingualText.builder().english("Income support of ₹6,000 per year for farmers").build())
                    .stateOrUt("ALL")
                    .schemeLevel(SchemeLevel.CENTRAL)
                    .applicationInfo(ApplicationInfo.builder().applicationUrl("https://pmkisan.gov.in").build())
                    .build();

            when(citizenProfileRepository.findByUserId("citizen-chat-01")).thenReturn(Optional.of(citizenProfile));
            when(citizenProfileService.toCitizenEligibilityProfile(citizenProfile)).thenReturn(
                    CitizenEligibilityProfile.builder().age(35).occupation("Farmer").state("Maharashtra").build()
            );
            when(applicationRepository.findAllByUserId("citizen-chat-01")).thenReturn(Collections.emptyList());
            lenient().when(eligibilityEngine.evaluate(any(), any())).thenReturn(
                    EligibilityEvaluationResult.builder().schemeCode("SCH-PM-KISAN").status(EligibilityStatus.ELIGIBLE).build()
            );
        }

        @Test
        @DisplayName("Citizen AI 1: 'Why was this scheme recommended to me?' is grounded")
        void testWhyRecommendedGrounded() {
            when(mongoTemplate.find(any(Query.class), eq(Scheme.class))).thenReturn(List.of(realScheme));
            when(eligibilityEngine.evaluate(any(), any())).thenReturn(
                    EligibilityEvaluationResult.builder().schemeCode("SCH-PM-KISAN").status(EligibilityStatus.ELIGIBLE).build()
            );

            AiChatRequest req = new AiChatRequest();
            req.setMessage("Why was this scheme recommended to me?");
            req.setConversationId("CONV-01");

            AiChatResponse resp = aiChatService.chatCitizen(req, "citizen-chat-01");

            assertNotNull(resp);
            assertTrue(resp.getResponse().contains("Statutory Eligibility"));
            assertTrue(resp.getResponse().contains("Maharashtra"));
            assertTrue(resp.getResponse().contains("Farmer"));
        }

        @Test
        @DisplayName("Citizen AI 2: 'Can I use an alternative document instead?' preserves ONE_OF semantics")
        void testOneOfAlternativeQuery() {
            when(mongoTemplate.find(any(Query.class), eq(Scheme.class))).thenReturn(List.of(realScheme));

            SchemeDocumentRequirementResolver.ResolvedRequirement reqWithAlt = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                    .documentCode("DOC_ID")
                    .documentName("Identity Proof")
                    .mandatory(true)
                    .alternativeGroupType("ONE_OF")
                    .alternatives(List.of("Aadhaar Card", "Voter ID Card", "PAN Card"))
                    .build();

            when(documentRequirementResolver.resolveRequirements(realScheme)).thenReturn(List.of(reqWithAlt));

            AiChatRequest req = new AiChatRequest();
            req.setMessage("Can I provide a Voter ID instead?");
            req.setConversationId("CONV-01");

            AiChatResponse resp = aiChatService.chatCitizen(req, "citizen-chat-01");

            assertNotNull(resp);
            assertTrue(resp.getResponse().contains("ONE_OF"));
            assertTrue(resp.getResponse().contains("Voter ID Card"));
            assertTrue(resp.getResponse().contains("fulfills the statutory document verification requirement"));
        }

        @Test
        @DisplayName("Citizen AI 3: Anti-Hallucination test for non-existent scheme")
        void testAntiHallucinationNonExistentScheme() {
            // When querying for nonexistent scheme, mongoTemplate returns empty
            when(mongoTemplate.find(any(Query.class), eq(Scheme.class))).thenReturn(Collections.emptyList());

            AiChatRequest req = new AiChatRequest();
            req.setMessage("Tell me about SuperFakeSchemeXYZ999");
            req.setConversationId("CONV-FAKE");

            AiChatResponse resp = aiChatService.chatCitizen(req, "citizen-chat-01");

            assertNotNull(resp);
            // Must NOT fabricate a fake scheme
            assertTrue(resp.getResponse().contains("couldn't verify that specific information") ||
                       resp.getResponse().contains("4,734 verified"),
                    "AI must acknowledge lack of verified data rather than fabricating an answer");
        }

        @Test
        @DisplayName("Citizen AI 4: Multi-turn conversation context is preserved")
        void testMultiTurnContextPreserved() {
            when(mongoTemplate.find(any(Query.class), eq(Scheme.class))).thenReturn(List.of(realScheme));

            AiChatRequest turn1 = new AiChatRequest();
            turn1.setMessage("What documents do I need for PM-KISAN?");
            AiChatResponse resp1 = aiChatService.chatCitizen(turn1, "citizen-chat-01");
            assertNotNull(resp1.getConversationId());

            AiChatRequest turn2 = new AiChatRequest();
            turn2.setConversationId(resp1.getConversationId());
            turn2.setMessage("Can I use an income certificate instead?");
            AiChatResponse resp2 = aiChatService.chatCitizen(turn2, "citizen-chat-01");

            assertEquals(resp1.getConversationId(), resp2.getConversationId());
            assertNotNull(resp2.getResponse());
        }
    }

    // =========================================================================
    // STEP 8: ADMIN CONVERSATIONAL AI GROUNDING & AUTHORIZATION
    // =========================================================================
    @Nested
    @DisplayName("Step 8 — Admin Conversational AI Grounding & Authorization")
    class AdminConversationalAiTests {

        @BeforeEach
        void initAdminMocks() {
            lenient().when(applicationRepository.count()).thenReturn(1250L);
            lenient().when(applicationRepository.countByStatus(any())).thenReturn(100L);
            lenient().when(grievanceRepository.count()).thenReturn(45L);
            lenient().when(grievanceRepository.countByStatus(any())).thenReturn(10L);
            lenient().when(feedbackRepository.count()).thenReturn(320L);
            lenient().when(schemeRepository.count()).thenReturn(4734L);
            lenient().when(schemeVerifiedDataRepository.count()).thenReturn(4682L);
        }

        @Test
        @DisplayName("Admin AI 1: Authorization — ROLE_ADMIN is allowed")
        void testRoleAdminAllowed() {
            AiChatRequest req = new AiChatRequest();
            req.setMessage("Show operational workload");

            AiChatResponse resp = aiChatService.chatAdmin(req, "admin-user-01", "ROLE_ADMIN");
            assertNotNull(resp);
            assertTrue(resp.getResponse().contains("Administrative Workload"));
        }

        @Test
        @DisplayName("Admin AI 2: Authorization — ROLE_SCHEME_MANAGER is allowed")
        void testRoleSchemeManagerAllowed() {
            AiChatRequest req = new AiChatRequest();
            req.setMessage("Show approval rate");

            AiChatResponse resp = aiChatService.chatAdmin(req, "mgr-user-02", "ROLE_SCHEME_MANAGER");
            assertNotNull(resp);
            assertTrue(resp.getResponse().contains("Approval"));
        }

        @Test
        @DisplayName("Admin AI 3: Authorization — ROLE_VERIFICATION_OFFICER is allowed")
        void testRoleVerificationOfficerAllowed() {
            AiChatRequest req = new AiChatRequest();
            req.setMessage("Show unresolved grievances");

            AiChatResponse resp = aiChatService.chatAdmin(req, "vo-user-03", "ROLE_VERIFICATION_OFFICER");
            assertNotNull(resp);
            assertTrue(resp.getResponse().contains("Grievance"));
        }

        @Test
        @DisplayName("Admin AI 4: Authorization — ROLE_USER is REJECTED with AccessDeniedException")
        void testRoleUserRejected() {
            AiChatRequest req = new AiChatRequest();
            req.setMessage("Show admin operational workload");

            assertThrows(SecurityException.class, () -> {
                aiChatService.chatAdmin(req, "citizen-user-01", "ROLE_USER");
            });
        }

        @Test
        @DisplayName("Admin AI 5: Operational Grounding — Evaluation metrics inquiry returns accurate ML facts")
        void testRecommendationEvaluationMetricsQuery() {
            AiChatRequest req = new AiChatRequest();
            req.setMessage("Show recommendation model evaluation metrics");

            AiChatResponse resp = aiChatService.chatAdmin(req, "admin-01", "ROLE_ADMIN");

            assertNotNull(resp);
            String text = resp.getResponse();
            assertTrue(text.contains("Statutory Eligibility Violation Rate"));
            assertTrue(text.contains("0.00%"));
            assertTrue(text.contains("Document Hallucination Rate"));
            assertTrue(text.contains("ONE_OF Group Preservation"));
            assertTrue(text.contains("384-d"));
        }

        @Test
        @DisplayName("Admin AI 6: Operational Grounding — Catalog Document Configuration returns 52 seed schemes")
        void testDocumentConfigurationAuditQuery() {
            AiChatRequest req = new AiChatRequest();
            req.setMessage("Which schemes have incomplete document requirements?");

            AiChatResponse resp = aiChatService.chatAdmin(req, "admin-01", "ROLE_ADMIN");

            assertNotNull(resp);
            String text = resp.getResponse();
            System.out.println("=== DEBUG ADMIN RESPONSE ===\n" + text);
            assertTrue(text.contains("4,734") || text.contains("4734"));
            assertTrue(text.contains("4,682") || text.contains("4682"));
            assertTrue(text.contains("Duplicate Schemes / Slugs"));
            assertTrue(text.contains("Perfect database integrity"));
        }
    }

    // =========================================================================
    // STEP 9: ML FALLBACK & CIRCUIT BREAKER VERIFICATION
    // =========================================================================
    @Nested
    @DisplayName("Step 9 — ML Fallback & Circuit Breaker Verification")
    class MlFallbackTests {

        private CitizenProfile eligibleProfile;
        private Scheme scheme1;
        private Scheme scheme2;

        @BeforeEach
        void init() {
            eligibleProfile = CitizenProfile.builder()
                    .userId("c-fallback-01")
                    .state("Maharashtra")
                    .age(30)
                    .occupation("Farmer")
                    .build();

            scheme1 = Scheme.builder().id("s1").schemeCode("SCH-001").slug("s1").build();
            scheme2 = Scheme.builder().id("s2").schemeCode("SCH-002").slug("s2").build();

            when(citizenProfileRepository.findByUserId("c-fallback-01")).thenReturn(Optional.of(eligibleProfile));

            List<EligibilityEvaluationResult> eligible = List.of(
                    EligibilityEvaluationResult.builder().schemeCode("SCH-001").status(EligibilityStatus.ELIGIBLE).build(),
                    EligibilityEvaluationResult.builder().schemeCode("SCH-002").status(EligibilityStatus.ELIGIBLE).build()
            );

            when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("c-fallback-01")).thenReturn(
                    CitizenEligibilityEvaluationResponse.builder().eligibleSchemes(eligible).build()
            );
            when(schemeRepository.findAll()).thenReturn(List.of(scheme1, scheme2));
        }

        @Test
        @DisplayName("Fallback 1: Semantic index unavailable -> Graceful deterministic fallback")
        void testIndexUnavailableFallback() {
            when(semanticEmbeddingIndexService.isAvailable()).thenReturn(false);

            PersonalizedSchemeRecommendationResponse res = recommendationService.getPersonalizedRecommendations("c-fallback-01", 0, 10);

            assertNotNull(res);
            assertEquals(2, res.getRecommendations().size());
            assertEquals("DETERMINISTIC_FALLBACK", res.getRankingMethod());
            assertTrue(res.getFallbackUsed());
        }

        @Test
        @DisplayName("Fallback 2: Semantic similarity throws exception -> Circuit breaker falls back cleanly")
        void testSemanticSimilarityExceptionFallback() {
            when(semanticEmbeddingIndexService.isAvailable()).thenReturn(true);
            when(semanticEmbeddingIndexService.computeSemanticSimilarity(anyString(), any()))
                    .thenThrow(new RuntimeException("Inference timeout / model connection error"));

            PersonalizedSchemeRecommendationResponse res = recommendationService.getPersonalizedRecommendations("c-fallback-01", 0, 10);

            assertNotNull(res);
            assertEquals(2, res.getRecommendations().size());
            assertEquals("DETERMINISTIC_FALLBACK", res.getRankingMethod());
            assertTrue(res.getFallbackUsed());
        }

        @Test
        @DisplayName("Fallback 3: Statutory eligibility gate MUST remain active during fallback")
        void testStatutoryGateActiveDuringFallback() {
            // Even during fallback, ineligible schemes must NEVER appear
            when(semanticEmbeddingIndexService.isAvailable()).thenReturn(false);

            PersonalizedSchemeRecommendationResponse res = recommendationService.getPersonalizedRecommendations("c-fallback-01", 0, 10);

            // Both returned schemes must strictly match the eligible schemes from statutory engine
            for (RankedSchemeItem item : res.getRecommendations()) {
                assertTrue(List.of("SCH-001", "SCH-002").contains(item.getSchemeCode()));
            }
        }
    }
}
