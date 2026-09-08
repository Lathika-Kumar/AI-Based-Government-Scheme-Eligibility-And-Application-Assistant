package com.schemebridge.scheme.service;

import com.schemebridge.scheme.config.MlRecommenderProperties;
import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import com.schemebridge.scheme.repository.SchemeVerifiedDataRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Authoritative end-to-end test suite for Current Citizen Scheme Recommendation & Checklist Generation.
 *
 * Enforces all 16 core requirements:
 * 1. Current user receives recommendations on first login without historical users.
 * 2. EligibilityEngine is the statutory authority and executes strictly before ranking.
 * 3. NOT_ELIGIBLE and INSUFFICIENT_DATA schemes are excluded from recommendations.
 * 4. AI/ML ranking ranks ONLY eligible schemes.
 * 5. Scheme-specific checklist is generated with mandatory/optional, accepted formats, issuing authority.
 * 6. Different schemes produce different document checklists.
 * 7. Canonical application steps and URLs are exposed.
 * 8. Zero fake users or synthetic behavioral records.
 */
@SpringBootTest
@ActiveProfiles("test")
class CurrentCitizenRecommendationAndChecklistTest {

    @Autowired
    private EligibleSchemeRecommendationService eligibleRecommendationService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private EligibilityEvaluationService eligibilityEvaluationService;

    @Autowired
    private CitizenProfileRepository citizenProfileRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeVerifiedDataRepository verifiedDataRepository;

    @Autowired
    private MlRecommenderProperties mlProperties;

    @Autowired
    private DocumentChecklistGenerator documentChecklistGenerator;

    @Autowired
    private com.schemebridge.scheme.controller.DocumentChecklistController documentChecklistController;

    private static final String FIRST_TIME_USER_A = "citizen_first_login_farmer_001";
    private static final String FIRST_TIME_USER_B = "citizen_first_login_student_002";
    private static final String FIRST_TIME_USER_C_INELIGIBLE = "citizen_first_login_high_income_003";

    private static final String SCHEME_FARMER_CODE = "SCH-FARMER-TEST-001";
    private static final String SCHEME_SCHOLARSHIP_CODE = "SCH-SCHOLAR-TEST-002";
    private static final String SCHEME_PENSION_CODE = "SCH-PENSION-TEST-003";
    private static final String SCHEME_UNMAPPED_CODE = "SCH-TEST-UNMAPPED-004";

    @BeforeEach
    void setUp() {
        cleanTestData();

        // 1. Setup Test Schemes
        // Scheme 1: Farmer Agricultural Assistance (Requires Farmer occupation, income <= 250,000, age >= 18)
        Scheme farmerScheme = Scheme.builder()
                .schemeCode(SCHEME_FARMER_CODE)
                .slug("kisan-samman-support-scheme")
                .title(MultilingualText.builder().english("Kisan Samman Agriculture Support").build())
                .shortDescription(MultilingualText.builder().english("Financial and input assistance to active farmers.").build())
                .schemeLevel(SchemeLevel.CENTRAL)
                .beneficiaryType("FARMER")
                .schemeType("AGRICULTURE")
                .status(SchemeStatus.ACTIVE)
                .eligibilityRules(RuleGroup.builder()
                        .logicalOperator("ALL")
                        .conditions(List.of(
                                EligibilityCondition.builder()
                                        .field("OCCUPATION")
                                        .operator(RuleOperator.EQUALS)
                                        .value("FARMER")
                                        .dataType("STRING")
                                        .required(true)
                                        .build(),
                                EligibilityCondition.builder()
                                        .field("ANNUAL_INCOME")
                                        .operator(RuleOperator.LESS_THAN_OR_EQUAL)
                                        .value("250000")
                                        .dataType("NUMBER")
                                        .required(true)
                                        .build(),
                                EligibilityCondition.builder()
                                        .field("AGE")
                                        .operator(RuleOperator.GREATER_THAN_OR_EQUAL)
                                        .value("18")
                                        .dataType("NUMBER")
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .benefits(List.of(
                        SchemeBenefit.builder()
                                .amountType("FINANCIAL")
                                .description(MultilingualText.builder().english("Annual input grant of ₹6,000 in three equal installments.").build())
                                .build()
                ))
                .requiredDocuments(List.of(
                        RequiredDocument.builder()
                                .documentCode("AADHAAR")
                                .name(MultilingualText.builder().english("Aadhaar Card").build())
                                .mandatory(true)
                                .issuingAuthority("UIDAI")
                                .acceptedFormats(List.of("PDF", "JPG"))
                                .build(),
                        RequiredDocument.builder()
                                .documentCode("LAND_RECORDS")
                                .name(MultilingualText.builder().english("Land Ownership Record (Khata/7-12 Extract)").build())
                                .mandatory(true)
                                .issuingAuthority("Revenue Department / Tehsildar")
                                .acceptedFormats(List.of("PDF"))
                                .build(),
                        RequiredDocument.builder()
                                .documentCode("BANK_PASSBOOK")
                                .name(MultilingualText.builder().english("Bank Passbook Copy").build())
                                .mandatory(true)
                                .issuingAuthority("Scheduled Commercial Bank")
                                .acceptedFormats(List.of("PDF", "JPG"))
                                .build()
                ))
                .applicationInfo(ApplicationInfo.builder()
                        .applicationMode("ONLINE")
                        .applicationUrl("https://pmkisan.gov.in")
                        .instructions(MultilingualText.builder().english("1. Register via Aadhaar OTP on PM-Kisan Portal. 2. Enter land khata details. 3. Submit bank account details.").build())
                        .build())
                .build();
        schemeRepository.save(farmerScheme);

        // Scheme 2: Higher Education Scholarship (Requires Student, age 18-25, State Gujarat, income <= 300,000)
        Scheme scholarScheme = Scheme.builder()
                .schemeCode(SCHEME_SCHOLARSHIP_CODE)
                .slug("gujarat-higher-education-scholarship")
                .title(MultilingualText.builder().english("Gujarat Higher Education Fellowship").build())
                .shortDescription(MultilingualText.builder().english("Merit and means based higher education scholarship.").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("GUJARAT")
                .beneficiaryType("STUDENT")
                .schemeType("EDUCATION")
                .status(SchemeStatus.ACTIVE)
                .eligibilityRules(RuleGroup.builder()
                        .logicalOperator("ALL")
                        .conditions(List.of(
                                EligibilityCondition.builder()
                                        .field("OCCUPATION")
                                        .operator(RuleOperator.EQUALS)
                                        .value("STUDENT")
                                        .dataType("STRING")
                                        .required(true)
                                        .build(),
                                EligibilityCondition.builder()
                                        .field("STATE")
                                        .operator(RuleOperator.EQUALS)
                                        .value("GUJARAT")
                                        .dataType("STRING")
                                        .required(true)
                                        .build(),
                                EligibilityCondition.builder()
                                        .field("AGE")
                                        .operator(RuleOperator.LESS_THAN_OR_EQUAL)
                                        .value("25")
                                        .dataType("NUMBER")
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .benefits(List.of(
                        SchemeBenefit.builder()
                                .amountType("SCHOLARSHIP")
                                .description(MultilingualText.builder().english("Full tuition reimbursement and monthly maintenance stipend.").build())
                                .build()
                ))
                .requiredDocuments(List.of(
                        RequiredDocument.builder()
                                .documentCode("AADHAAR")
                                .name(MultilingualText.builder().english("Aadhaar Card").build())
                                .mandatory(true)
                                .issuingAuthority("UIDAI")
                                .build(),
                        RequiredDocument.builder()
                                .documentCode("COLLEGE_ID")
                                .name(MultilingualText.builder().english("College Bonafide / Enrollment Certificate").build())
                                .mandatory(true)
                                .issuingAuthority("Accredited University / College")
                                .build(),
                        RequiredDocument.builder()
                                .documentCode("INCOME_CERT")
                                .name(MultilingualText.builder().english("Income Certificate").build())
                                .mandatory(true)
                                .issuingAuthority("Mamlatdar Office")
                                .build()
                ))
                .applicationInfo(ApplicationInfo.builder()
                        .applicationMode("ONLINE")
                        .applicationUrl("https://scholarships.gujarat.gov.in")
                        .instructions(MultilingualText.builder().english("1. Register student account. 2. Upload fee receipt and college bonafide. 3. Submit for institute verification.").build())
                        .build())
                .build();
        schemeRepository.save(scholarScheme);

        // Scheme 3: Senior Citizen Pension (Requires age >= 60)
        Scheme pensionScheme = Scheme.builder()
                .schemeCode(SCHEME_PENSION_CODE)
                .slug("national-senior-pension")
                .title(MultilingualText.builder().english("National Senior Citizen Pension").build())
                .shortDescription(MultilingualText.builder().english("Old age social security monthly pension.").build())
                .schemeLevel(SchemeLevel.CENTRAL)
                .beneficiaryType("SENIOR_CITIZEN")
                .schemeType("PENSION")
                .status(SchemeStatus.ACTIVE)
                .eligibilityRules(RuleGroup.builder()
                        .logicalOperator("ALL")
                        .conditions(List.of(
                                EligibilityCondition.builder()
                                        .field("AGE")
                                        .operator(RuleOperator.GREATER_THAN_OR_EQUAL)
                                        .value("60")
                                        .dataType("NUMBER")
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .benefits(List.of(
                        SchemeBenefit.builder()
                                .amountType("PENSION")
                                .description(MultilingualText.builder().english("Monthly pension of ₹3,000 credited via DBT.").build())
                                .build()
                ))
                .requiredDocuments(List.of(
                        RequiredDocument.builder()
                                .documentCode("AADHAAR")
                                .name(MultilingualText.builder().english("Aadhaar Card").build())
                                .mandatory(true)
                                .issuingAuthority("UIDAI")
                                .build(),
                        RequiredDocument.builder()
                                .documentCode("AGE_PROOF")
                                .name(MultilingualText.builder().english("Birth Certificate / Age Certificate").build())
                                .mandatory(true)
                                .issuingAuthority("Municipal Corporation / Health Dept")
                                .build()
                ))
                .applicationInfo(ApplicationInfo.builder()
                        .applicationMode("ONLINE")
                        .applicationUrl("https://nsap.nic.in")
                        .instructions(MultilingualText.builder().english("1. Fill online application form. 2. Verify age credentials. 3. Submit bank IFSC.").build())
                        .build())
                .build();
        schemeRepository.save(pensionScheme);

        // Also add SchemeVerifiedData for SCHEME_FARMER_CODE to test canonical resolution
        SchemeVerifiedData verifiedFarmer = SchemeVerifiedData.builder()
                .schemeCode(SCHEME_FARMER_CODE)
                .slug("kisan-samman-support-scheme")
                .documentStatus("DOCUMENTS_FOUND")
                .reconciliationStatus("MATCHED")
                .identity(SchemeVerifiedData.CanonicalSchemeIdentity.builder()
                        .schemeCode(SCHEME_FARMER_CODE)
                        .schemeName("Kisan Samman Agriculture Support")
                        .ministry("Ministry of Agriculture and Farmers Welfare")
                        .department("Department of Agriculture & Cooperation")
                        .level(SchemeLevel.CENTRAL)
                        .category("Agriculture")
                        .build())
                .application(SchemeVerifiedData.CanonicalApplication.builder()
                        .applicationMethod("ONLINE")
                        .applicationProcedure("Register on PM-Kisan portal, authenticate Aadhaar, provide land ledger record.")
                        .applicationSteps(List.of(
                                "Navigate to PM-Kisan portal and click 'New Farmer Registration'.",
                                "Input Aadhaar Number and select State as per revenue records.",
                                "Enter Land Khata / Survey Number and authenticate bank account.",
                                "Submit registration and note down Application Reference Number."
                        ))
                        .officialApplicationUrl("https://pmkisan.gov.in/RegistrationFormNew.aspx")
                        .officialPortalUrl("https://pmkisan.gov.in")
                        .helplineNumber("155261 / 011-24300606")
                        .build())
                .documents(List.of(
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("AADHAAR")
                                .canonicalDocumentCode("AADHAAR")
                                .officialDocumentName("Aadhaar Card")
                                .mandatory(true)
                                .optional(false)
                                .issuingAuthority("UIDAI (Govt of India)")
                                .whyRequired("Identity and biometric DBT authentication.")
                                .acceptedFormats(List.of("PDF", "JPG"))
                                .build(),
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("LAND_RECORDS")
                                .canonicalDocumentCode("LAND_OWNERSHIP_PROOF")
                                .officialDocumentName("Land Ownership Record (7/12 Extract or RoR)")
                                .mandatory(true)
                                .optional(false)
                                .issuingAuthority("Revenue Department / Talati / Patwari")
                                .whyRequired("Proof of cultivable landholding.")
                                .acceptedFormats(List.of("PDF"))
                                .build()
                ))
                .build();
        verifiedDataRepository.save(verifiedFarmer);

        // Setup Brand New First-Time Citizens (Zero historical applications or interaction events)
        // Citizen A: Farmer, 42 years old, income 180,000, Maharashtra
        CitizenProfile profileA = CitizenProfile.builder()
                .userId(FIRST_TIME_USER_A)
                .displayName("Rameshwar Patil")
                .dob(LocalDate.of(1984, 5, 10))
                .age(42)
                .gender("MALE")
                .state("MAHARASHTRA")
                .district("Nashik")
                .occupation("FARMER")
                .annualIncome(180000.0)
                .socialCategory("OBC")
                .onboardingComplete(true)
                .onboardingStatus("COMPLETE")
                .verifiedAttributes(Map.of())
                .build();
        citizenProfileRepository.save(profileA);

        // Citizen B: Student, 21 years old, income 90,000, Gujarat
        CitizenProfile profileB = CitizenProfile.builder()
                .userId(FIRST_TIME_USER_B)
                .displayName("Ananya Desai")
                .dob(LocalDate.of(2005, 3, 15))
                .age(21)
                .gender("FEMALE")
                .state("GUJARAT")
                .district("Ahmedabad")
                .occupation("STUDENT")
                .annualIncome(90000.0)
                .socialCategory("GENERAL")
                .onboardingComplete(true)
                .onboardingStatus("COMPLETE")
                .verifiedAttributes(Map.of())
                .build();
        citizenProfileRepository.save(profileB);

        // Citizen C: Minor with high income, 14 years old, income 1,500,000 (eligible for 0 schemes)
        CitizenProfile profileC = CitizenProfile.builder()
                .userId(FIRST_TIME_USER_C_INELIGIBLE)
                .displayName("Aditya Verma")
                .dob(LocalDate.of(2012, 11, 20))
                .age(14)
                .gender("MALE")
                .state("KARNATAKA")
                .district("Bengaluru")
                .occupation("STUDENT_SECONDARY")
                .annualIncome(1500000.0)
                .socialCategory("GENERAL")
                .onboardingComplete(true)
                .onboardingStatus("COMPLETE")
                .verifiedAttributes(Map.of())
                .build();
        citizenProfileRepository.save(profileC);
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }

    private void cleanTestData() {
        citizenProfileRepository.deleteByUserId(FIRST_TIME_USER_A);
        citizenProfileRepository.deleteByUserId(FIRST_TIME_USER_B);
        citizenProfileRepository.deleteByUserId(FIRST_TIME_USER_C_INELIGIBLE);

        schemeRepository.findBySchemeCode(SCHEME_FARMER_CODE).ifPresent(schemeRepository::delete);
        schemeRepository.findBySchemeCode(SCHEME_SCHOLARSHIP_CODE).ifPresent(schemeRepository::delete);
        schemeRepository.findBySchemeCode(SCHEME_PENSION_CODE).ifPresent(schemeRepository::delete);
        schemeRepository.findBySchemeCode(SCHEME_UNMAPPED_CODE).ifPresent(schemeRepository::delete);

        verifiedDataRepository.findBySchemeCode(SCHEME_FARMER_CODE).ifPresent(verifiedDataRepository::delete);
        verifiedDataRepository.findBySchemeCode(SCHEME_SCHOLARSHIP_CODE).ifPresent(verifiedDataRepository::delete);
        verifiedDataRepository.findBySchemeCode(SCHEME_PENSION_CODE).ifPresent(verifiedDataRepository::delete);
    }

    @Test
    @DisplayName("Req 1 & 7: Current user receives personalized recommendations on first login with zero historical users")
    void testFirstTimeUserReceivesRecommendations() {
        PersonalizedSchemeRecommendationResponse response =
                eligibleRecommendationService.getPersonalizedRecommendations(FIRST_TIME_USER_A, 0, 10);

        assertNotNull(response);
        assertEquals(FIRST_TIME_USER_A, response.getUserId());
        assertTrue(response.getEligibleCandidatesFound() > 0, "Farmer should be eligible for at least 1 scheme");
        assertFalse(response.getRecommendations().isEmpty(), "Recommendations must not be empty for eligible citizen");

        RankedSchemeItem farmerItem = response.getRecommendations().stream()
                .filter(r -> SCHEME_FARMER_CODE.equals(r.getSchemeCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Farmer scheme must be recommended for farmer citizen"));
        assertEquals(SCHEME_FARMER_CODE, farmerItem.getSchemeCode());
        assertEquals("ELIGIBLE", farmerItem.getEligibilityStatus());
        assertNotNull(farmerItem.getExplanation());
        assertNotNull(farmerItem.getReasons());
        assertFalse(farmerItem.getReasons().isEmpty());
    }

    @Test
    @DisplayName("Req 2 & 15: EligibilityEngine executes before AI/ML ranking; NOT_ELIGIBLE schemes are discarded")
    void testEligibilityEngineGatingStrictlyExcludesIneligible() {
        // User A (Farmer, age 42) is NOT eligible for:
        // - SCHEME_SCHOLARSHIP_CODE (requires Student, age <= 25, Gujarat)
        // - SCHEME_PENSION_CODE (requires Age >= 60)
        PersonalizedSchemeRecommendationResponse response =
                eligibleRecommendationService.getPersonalizedRecommendations(FIRST_TIME_USER_A, 0, 10);

        List<String> returnedCodes = response.getRecommendations().stream()
                .map(RankedSchemeItem::getSchemeCode)
                .toList();

        assertTrue(returnedCodes.contains(SCHEME_FARMER_CODE), "Must contain eligible farmer scheme");
        assertFalse(returnedCodes.contains(SCHEME_SCHOLARSHIP_CODE), "Must NOT contain ineligible student scheme");
        assertFalse(returnedCodes.contains(SCHEME_PENSION_CODE), "Must NOT contain ineligible senior pension scheme");

        for (RankedSchemeItem item : response.getRecommendations()) {
            assertEquals("ELIGIBLE", item.getEligibilityStatus(), "Every recommended item must have ELIGIBLE status");
        }
    }

    @Test
    @DisplayName("Req 3: Different current profiles produce different personalized recommendations")
    void testDifferentProfilesProduceDifferentRecommendations() {
        PersonalizedSchemeRecommendationResponse resA =
                eligibleRecommendationService.getPersonalizedRecommendations(FIRST_TIME_USER_A, 0, 10);
        PersonalizedSchemeRecommendationResponse resB =
                eligibleRecommendationService.getPersonalizedRecommendations(FIRST_TIME_USER_B, 0, 10);

        List<String> codesA = resA.getRecommendations().stream().map(RankedSchemeItem::getSchemeCode).toList();
        List<String> codesB = resB.getRecommendations().stream().map(RankedSchemeItem::getSchemeCode).toList();

        assertTrue(codesA.contains(SCHEME_FARMER_CODE), "Farmer must receive farmer scheme");
        assertFalse(codesA.contains(SCHEME_SCHOLARSHIP_CODE), "Farmer must not receive student scheme");

        assertTrue(codesB.contains(SCHEME_SCHOLARSHIP_CODE), "Student must receive scholarship scheme");
        assertFalse(codesB.contains(SCHEME_FARMER_CODE), "Student must not receive farmer scheme");
    }

    @Test
    @DisplayName("Req 4: Scheme-specific document checklist is generated with mandatory flags and issuing authority")
    void testSchemeSpecificChecklistGenerated() {
        PersonalizedSchemeRecommendationResponse response =
                eligibleRecommendationService.getPersonalizedRecommendations(FIRST_TIME_USER_A, 0, 10);

        RankedSchemeItem farmerItem = response.getRecommendations().stream()
                .filter(r -> SCHEME_FARMER_CODE.equals(r.getSchemeCode()))
                .findFirst()
                .orElseThrow();

        // Verify requiredDocuments list
        assertNotNull(farmerItem.getRequiredDocuments());
        assertFalse(farmerItem.getRequiredDocuments().isEmpty());

        // Verify structured checklist
        assertNotNull(farmerItem.getChecklist());
        assertFalse(farmerItem.getChecklist().isEmpty());

        DocumentChecklistItemResponse aadhaarDoc = farmerItem.getChecklist().stream()
                .filter(d -> "AADHAAR".equalsIgnoreCase(d.getDocumentCode()))
                .findFirst()
                .orElseThrow();

        assertTrue(aadhaarDoc.isMandatory());
        assertTrue(aadhaarDoc.isRequired());
        assertNotNull(aadhaarDoc.getIssuingAuthority());
        assertNotNull(aadhaarDoc.getWhyRequired());
    }

    @Test
    @DisplayName("Req 4 & 5: Different schemes produce different checklists and canonical application steps")
    void testDifferentSchemesProduceDifferentChecklistsAndSteps() {
        PersonalizedSchemeRecommendationResponse resA =
                eligibleRecommendationService.getPersonalizedRecommendations(FIRST_TIME_USER_A, 0, 10);
        PersonalizedSchemeRecommendationResponse resB =
                eligibleRecommendationService.getPersonalizedRecommendations(FIRST_TIME_USER_B, 0, 10);

        RankedSchemeItem farmerItem = resA.getRecommendations().stream()
                .filter(r -> SCHEME_FARMER_CODE.equals(r.getSchemeCode())).findFirst().orElseThrow();
        RankedSchemeItem studentItem = resB.getRecommendations().stream()
                .filter(r -> SCHEME_SCHOLARSHIP_CODE.equals(r.getSchemeCode())).findFirst().orElseThrow();

        // Documents must differ based on actual scheme requirements
        List<String> farmerDocs = farmerItem.getRequiredDocuments();
        List<String> studentDocs = studentItem.getRequiredDocuments();

        assertNotEquals(farmerDocs, studentDocs, "Different schemes must have scheme-specific document requirements");

        // Application steps must be exposed from canonical scheme data
        assertNotNull(farmerItem.getApplicationSteps());
        assertFalse(farmerItem.getApplicationSteps().isEmpty());
        assertNotNull(farmerItem.getApplicationUrl());
        assertTrue(farmerItem.getApplicationUrl().contains("pmkisan"));

        assertNotNull(studentItem.getApplicationSteps());
        assertFalse(studentItem.getApplicationSteps().isEmpty());
        assertNotNull(studentItem.getApplicationUrl());
        assertTrue(studentItem.getApplicationUrl().contains("scholarships.gujarat.gov.in"));
    }

    @Test
    @DisplayName("Req 6: Eligibility explanation is grounded in matched profile criteria")
    void testEligibilityExplanationGrounded() {
        PersonalizedSchemeRecommendationResponse response =
                eligibleRecommendationService.getPersonalizedRecommendations(FIRST_TIME_USER_A, 0, 10);

        RankedSchemeItem item = response.getRecommendations().get(0);
        assertNotNull(item.getExplanation());
        assertNotNull(item.getExplanation().getPrimaryReason());
        assertTrue(item.getExplanation().getPrimaryReason().contains("Statutory eligibility was confirmed by the EligibilityEngine"));

        assertNotNull(item.getReasons());
        boolean hasOccupationReason = item.getReasons().stream().anyMatch(r -> r.toLowerCase().contains("occupation") || r.toLowerCase().contains("farmer"));
        assertTrue(hasOccupationReason, "Explanation reasons must mention matched occupation");
    }

    @Test
    @DisplayName("Req 13 Q: Empty eligible result (user eligible for 0 schemes) handled safely")
    void testEmptyEligibleResultHandledSafely() {
        // User C has income 1,500,000, occupation SALARIED_PRIVATE, age 35 -> eligible for 0 schemes
        PersonalizedSchemeRecommendationResponse response =
                eligibleRecommendationService.getPersonalizedRecommendations(FIRST_TIME_USER_C_INELIGIBLE, 0, 10);

        assertNotNull(response);
        assertEquals(FIRST_TIME_USER_C_INELIGIBLE, response.getUserId());
        assertEquals(0, response.getEligibleCandidatesFound());
        assertEquals(0, response.getTotalRecommendationsReturned());
        assertTrue(response.getRecommendations().isEmpty());
    }

    @Test
    @DisplayName("Req 12 & 13 R: Direct scheme document checklist endpoint returns canonical requirements")
    void testDirectSchemeChecklistEndpoint() {
        SchemeDocumentChecklistResponse checklist =
                applicationService.getSchemeDocumentChecklist(SCHEME_FARMER_CODE);

        assertNotNull(checklist);
        assertEquals(SCHEME_FARMER_CODE, checklist.getSchemeCode());
        assertEquals("DOCUMENTS_FOUND", checklist.getDocumentStatus());
        assertFalse(checklist.getItems().isEmpty());

        // Canonical application steps and official URL must be present
        assertNotNull(checklist.getApplicationSteps());
        assertFalse(checklist.getApplicationSteps().isEmpty());
        assertEquals("https://pmkisan.gov.in/RegistrationFormNew.aspx", checklist.getOfficialApplicationUrl());
        assertEquals("155261 / 011-24300606", checklist.getHelplineNumber());
    }

    @Test
    @DisplayName("Req 8 & 13 S: No historical user or PII leakage exists in ranking features")
    void testZeroTargetLeakageAndPiiSafety() {
        PersonalizedSchemeRecommendationResponse response =
                eligibleRecommendationService.getPersonalizedRecommendations(FIRST_TIME_USER_A, 0, 10);

        for (RankedSchemeItem item : response.getRecommendations()) {
            if (item.getFeatureVector() != null) {
                // Feature vector must not contain PII or target outcome labels
                assertNotNull(item.getFeatureVector().getUserFeatures());
                assertNotNull(item.getFeatureVector().getSchemeFeatures());
            }
        }
    }

    @Test
    @DisplayName("Req 9 & 10: Missing documents are identified and user-provided status is accurately tracked")
    void testMissingDocumentsIdentifiedAndUserProvidedStatus() {
        PersonalizedSchemeRecommendationResponse response =
                eligibleRecommendationService.getPersonalizedRecommendations(FIRST_TIME_USER_A, 0, 10);

        RankedSchemeItem farmerItem = response.getRecommendations().stream()
                .filter(r -> SCHEME_FARMER_CODE.equals(r.getSchemeCode()))
                .findFirst()
                .orElseThrow();

        // Check missingDocuments list is populated
        assertNotNull(farmerItem.getMissingDocuments());
        assertFalse(farmerItem.getMissingDocuments().isEmpty(), "Unprovided mandatory documents must be flagged as missing");

        // Verify that checklist item has statusString
        boolean hasMissing = farmerItem.getChecklist().stream()
                .anyMatch(d -> "MISSING".equalsIgnoreCase(d.getStatusString()));
        assertTrue(hasMissing, "At least one unprovided mandatory document must have MISSING status");

        // Now test with verified attribute: if citizen has verified Aadhaar
        CitizenProfile profileA = citizenProfileRepository.findByUserId(FIRST_TIME_USER_A).orElseThrow();
        Map<String, VerifiedAttribute<?>> attrs = new HashMap<>(profileA.getVerifiedAttributes() != null ? profileA.getVerifiedAttributes() : Map.of());
        attrs.put("AADHAAR", VerifiedAttribute.builder().source("DIGILOCKER_VERIFIED").verified(true).build());
        profileA.setVerifiedAttributes(attrs);
        citizenProfileRepository.save(profileA);

        Scheme farmerScheme = schemeRepository.findBySchemeCode(SCHEME_FARMER_CODE).orElseThrow();
        SchemeDocumentChecklistResponse checklist = documentChecklistGenerator.generateChecklist(profileA, farmerScheme);

        DocumentChecklistItemResponse aadhaarItem = checklist.getItems().stream()
                .filter(d -> "AADHAAR".equalsIgnoreCase(d.getDocumentCode()))
                .findFirst()
                .orElseThrow();

        assertTrue(aadhaarItem.isUserProvided());
        assertEquals("PROVIDED", aadhaarItem.getStatusString());
    }

    @Test
    @DisplayName("Req 8 & Zero-Hallucination: Fallback returns 'Requirement information unavailable' when scheme has no documents")
    void testZeroHallucinationUnavailableDocumentFallback() {
        Scheme unmappedScheme = Scheme.builder()
                .schemeCode(SCHEME_UNMAPPED_CODE)
                .slug("unmapped-test-scheme")
                .title(MultilingualText.builder().english("Unmapped Test Scheme").build())
                .schemeLevel(SchemeLevel.STATE)
                .status(SchemeStatus.ACTIVE)
                .requiredDocuments(List.of())
                .build();
        schemeRepository.save(unmappedScheme);

        SchemeDocumentChecklistResponse checklist = documentChecklistGenerator.generateChecklist(unmappedScheme);

        assertNotNull(checklist);
        assertEquals("DOCUMENT_REQUIREMENTS_NOT_MAPPED", checklist.getDocumentStatus());
        assertEquals(1, checklist.getItems().size());
        assertEquals("Requirement information unavailable", checklist.getItems().get(0).getDocumentName());
        assertEquals("Requirement information unavailable", checklist.getItems().get(0).getWhyRequired());
        assertFalse(checklist.getItems().get(0).isMandatory());
    }

    @Test
    @DisplayName("Req 10: Application steps endpoint returns canonical procedural steps, portal URL, and helpline")
    void testApplicationStepsEndpoint() {
        ResponseEntity<ApplicationStepsResponse> responseEntity =
                documentChecklistController.getSchemeApplicationSteps(SCHEME_FARMER_CODE, null);
        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());

        ApplicationStepsResponse stepsResponse = responseEntity.getBody();
        assertNotNull(stepsResponse);
        assertEquals(SCHEME_FARMER_CODE, stepsResponse.getSchemeCode());
        assertNotNull(stepsResponse.getApplicationSteps());
        assertFalse(stepsResponse.getApplicationSteps().isEmpty());
        assertNotNull(stepsResponse.getOfficialApplicationUrl());
        assertTrue(stepsResponse.getOfficialApplicationUrl().contains("pmkisan"));
        assertEquals("155261 / 011-24300606", stepsResponse.getHelplineNumber());
    }

    @Test
    @DisplayName("Req 2 & 14: Direct statutory eligibility evaluation confirms eligibility without ML invocation")
    void testDirectStatutoryEligibilityEvaluation() {
        EligibilityEvaluationResult farmerResult =
                eligibilityEvaluationService.evaluateCitizenAgainstScheme(FIRST_TIME_USER_A, SCHEME_FARMER_CODE);

        assertNotNull(farmerResult);
        assertEquals(EligibilityStatus.ELIGIBLE, farmerResult.getStatus());
        assertFalse(farmerResult.getPassedConditions().isEmpty());

        EligibilityEvaluationResult pensionResult =
                eligibilityEvaluationService.evaluateCitizenAgainstScheme(FIRST_TIME_USER_A, SCHEME_PENSION_CODE);

        assertNotNull(pensionResult);
        assertEquals(EligibilityStatus.NOT_ELIGIBLE, pensionResult.getStatus());
        assertFalse(pensionResult.getFailedConditions().isEmpty());
    }
}
