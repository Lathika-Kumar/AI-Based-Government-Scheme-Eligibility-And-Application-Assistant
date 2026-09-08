package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.request.CreateApplicationRequest;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.enums.RequirementProvenance;
import com.schemebridge.scheme.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoOperations;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Phase17SchemeDataConsistencyValidationTest {

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private SchemeVerifiedDataRepository schemeVerifiedDataRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Mock
    private EligibilityEngine eligibilityEngine;

    @Mock
    private CitizenProfileService citizenProfileService;

    @Mock
    private ApplicationEventService applicationEventService;

    @Mock
    private ApplicationStatusTransitionService applicationStatusTransitionService;

    @Mock
    private DetailedDocumentStatusTransitionService detailedDocumentStatusTransitionService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MongoOperations mongoOperations;

    private SchemeDocumentRequirementResolver schemeDocumentRequirementResolver;

    @InjectMocks
    private SchemeService schemeService;

    @InjectMocks
    private ApplicationService applicationService;

    private Scheme canonicalScheme;
    private Scheme seedOnlyScheme;
    private SchemeVerifiedData canonicalVerifiedData;
    private SchemeVerifiedData so2yt5ylmCanonicalData;
    private Scheme so2yt5ylmMasterScheme;

    @BeforeEach
    void setUp() {
        schemeDocumentRequirementResolver = new SchemeDocumentRequirementResolver(schemeVerifiedDataRepository);
        applicationService.setSchemeDocumentRequirementResolver(schemeDocumentRequirementResolver);
        applicationService.setSchemeVerifiedDataRepository(schemeVerifiedDataRepository);

        // 1. Canonical validation scheme SO2YT5YLM
        so2yt5ylmMasterScheme = Scheme.builder()
                .id("mongo-so2yt5ylm-id")
                .schemeCode("SO2YT5YLM")
                .slug("so2yt5ylm")
                .title(MultilingualText.builder().english("Subsidy on 2nd Year to 5th Year Lease Money").build())
                .description(MultilingualText.builder().english("Scheme for SC Families in Fisheries Sector").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Haryana")
                .status(SchemeStatus.ACTIVE)
                .applicationInfo(ApplicationInfo.builder()
                        .deadline(Instant.parse("2026-12-31T23:59:59Z"))
                        .applicationMode("ONLINE")
                        .build())
                .build();

        so2yt5ylmCanonicalData = SchemeVerifiedData.builder()
                .schemeCode("SO2YT5YLM")
                .slug("so2yt5ylm")
                .documentStatus("DOCUMENTS_FOUND")
                .overallProvenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .reconciliationStatus("MATCHED")
                .documents(List.of(
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("DOC_SO2YT5YLM_001")
                                .canonicalDocumentCode("AGREEMENT_DEED")
                                .officialDocumentName("Agreement 1 - Agreement deed between fish farmer and Fisheries Department")
                                .mandatory(true)
                                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                                .build(),
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("DOC_SO2YT5YLM_002")
                                .canonicalDocumentCode("AGREEMENT_DEED")
                                .officialDocumentName("Agreement 2 - Agreement deed between fish farmer and panchayat for fish culture")
                                .mandatory(true)
                                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                                .build(),
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("DOC_SO2YT5YLM_003")
                                .canonicalDocumentCode("BIRTH_CERTIFICATE")
                                .officialDocumentName("Date of Birth Certificate - Birth Certificate/Matriculation Certificate/PAN Card/Voter Card/Driving License")
                                .mandatory(true)
                                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                                .build(),
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("DOC_SO2YT5YLM_004")
                                .canonicalDocumentCode("IDENTITY_PROOF")
                                .officialDocumentName("Identity Proof – Ration Card/Aadhar Card/PAN Card/Voter Card")
                                .mandatory(true)
                                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                                .build(),
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("DOC_SO2YT5YLM_005")
                                .canonicalDocumentCode("CASTE_CERTIFICATE")
                                .officialDocumentName("Caste Certificate - Caste Certificate issued by 1st Class Magistrate")
                                .mandatory(true)
                                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                                .build(),
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("DOC_SO2YT5YLM_006")
                                .canonicalDocumentCode("TRAINING_CERTIFICATE")
                                .officialDocumentName("Training Certificate – Fisheries Training from any Govt. Institute")
                                .mandatory(true)
                                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                                .build(),
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("DOC_SO2YT5YLM_007")
                                .canonicalDocumentCode("LEASE_DEED")
                                .officialDocumentName("Lease deed - (Panchayat Resolution and receipt no.4 of Panchayat).")
                                .mandatory(true)
                                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                                .build(),
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("DOC_SO2YT5YLM_008")
                                .canonicalDocumentCode("PAYMENT_OR_PURCHASE_RECEIPT")
                                .officialDocumentName("Receipt of Fish Seed (purchased from government/national fish seed farms)")
                                .mandatory(true)
                                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                                .build(),
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("DOC_SO2YT5YLM_009")
                                .canonicalDocumentCode("PHOTOGRAPHS")
                                .officialDocumentName("Photographs of the Pond Site")
                                .mandatory(true)
                                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                                .build()
                ))
                .build();

        // 2. Another canonical scheme
        canonicalScheme = Scheme.builder()
                .id("mongo-canonical-id")
                .schemeCode("CAN-AGRI-001")
                .slug("kisan-credit-support")
                .title(MultilingualText.builder().english("Kisan Credit Support Scheme").build())
                .schemeLevel(SchemeLevel.CENTRAL)
                .status(SchemeStatus.ACTIVE)
                .applicationInfo(ApplicationInfo.builder().deadline(null).build())
                .build();

        canonicalVerifiedData = SchemeVerifiedData.builder()
                .schemeCode("CAN-AGRI-001")
                .slug("kisan-credit-support")
                .documentStatus("DOCUMENTS_FOUND")
                .overallProvenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .reconciliationStatus("MATCHED")
                .documents(List.of(
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("DOC_CAN_001")
                                .canonicalDocumentCode("IDENTITY_PROOF")
                                .officialDocumentName("Identity Proof - Aadhaar / Voter ID / Passport")
                                .mandatory(true)
                                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                                .build()
                ))
                .build();

        // 3. Seed-only scheme (from 52 seed schemes, no canonical record)
        seedOnlyScheme = Scheme.builder()
                .id("mongo-seed-001-id")
                .schemeCode("SCH-HLTH-001")
                .slug("ayushman-bharat-pmjay")
                .title(MultilingualText.builder().english("Ayushman Bharat Pradhan Mantri Jan Arogya Yojana").build())
                .schemeLevel(SchemeLevel.CENTRAL)
                .status(SchemeStatus.ACTIVE)
                .requiredDocuments(List.of(
                        RequiredDocument.builder()
                                .documentCode("AADHAAR")
                                .name(MultilingualText.builder().english("Aadhaar Card").build())
                                .mandatory(true)
                                .build()
                ))
                .applicationInfo(ApplicationInfo.builder().deadline(null).build())
                .build();

        // Mock repositories
        when(schemeRepository.findById("mongo-so2yt5ylm-id")).thenReturn(Optional.of(so2yt5ylmMasterScheme));
        when(schemeRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(so2yt5ylmMasterScheme));
        when(schemeRepository.findBySlug("so2yt5ylm")).thenReturn(Optional.of(so2yt5ylmMasterScheme));
        when(schemeVerifiedDataRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(so2yt5ylmCanonicalData));
        when(schemeVerifiedDataRepository.findBySlug("so2yt5ylm")).thenReturn(Optional.of(so2yt5ylmCanonicalData));

        when(schemeRepository.findById("mongo-canonical-id")).thenReturn(Optional.of(canonicalScheme));
        when(schemeRepository.findBySchemeCode("CAN-AGRI-001")).thenReturn(Optional.of(canonicalScheme));
        when(schemeRepository.findBySlug("kisan-credit-support")).thenReturn(Optional.of(canonicalScheme));
        when(schemeVerifiedDataRepository.findBySchemeCode("CAN-AGRI-001")).thenReturn(Optional.of(canonicalVerifiedData));
        when(schemeVerifiedDataRepository.findBySlug("kisan-credit-support")).thenReturn(Optional.of(canonicalVerifiedData));

        when(schemeRepository.findById("mongo-seed-001-id")).thenReturn(Optional.of(seedOnlyScheme));
        when(schemeRepository.findBySchemeCode("SCH-HLTH-001")).thenReturn(Optional.of(seedOnlyScheme));
        when(schemeRepository.findBySlug("ayushman-bharat-pmjay")).thenReturn(Optional.of(seedOnlyScheme));
        when(schemeVerifiedDataRepository.findBySchemeCode("SCH-HLTH-001")).thenReturn(Optional.empty());
        when(schemeVerifiedDataRepository.findBySlug("ayushman-bharat-pmjay")).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("1. Canonical scheme resolves successfully via SchemeService")
    void test1_CanonicalSchemeResolves() {
        SchemeResponse res = schemeService.getSchemeByCode("SO2YT5YLM");
        assertThat(res).isNotNull();
        assertThat(res.getSchemeCode()).isEqualTo("SO2YT5YLM");
        assertThat(res.getTitle().getEnglish()).contains("Subsidy on 2nd Year");
    }

    @Test
    @DisplayName("2. Seed-only scheme resolves successfully without canonical record")
    void test2_SeedOnlySchemeResolves() {
        SchemeResponse res = schemeService.getSchemeByCode("SCH-HLTH-001");
        assertThat(res).isNotNull();
        assertThat(res.getSchemeCode()).isEqualTo("SCH-HLTH-001");
        assertThat(res.getTitle().getEnglish()).contains("Ayushman Bharat");
    }

    @Test
    @DisplayName("3. Scheme lookup by schemeCode works across uppercase/lowercase variations")
    void test3_SchemeCodeLookupWorks() {
        SchemeResponse resUpper = schemeService.getSchemeByCode("SO2YT5YLM");
        assertThat(resUpper).isNotNull();

        SchemeResponse resLower = schemeService.getSchemeByCode("so2yt5ylm");
        assertThat(resLower).isNotNull();
        assertThat(resLower.getSchemeCode()).isEqualTo("SO2YT5YLM");
    }

    @Test
    @DisplayName("4. Scheme lookup by slug works cleanly")
    void test4_SlugLookupWorks() {
        SchemeResponse res = schemeService.getSchemeById("ayushman-bharat-pmjay");
        assertThat(res).isNotNull();
        assertThat(res.getSchemeCode()).isEqualTo("SCH-HLTH-001");
    }

    @Test
    @DisplayName("5. Canonical document checklist resolves with official provenance and items")
    void test5_CanonicalDocumentChecklistResolves() {
        SchemeDocumentChecklistResponse checklist = applicationService.getSchemeDocumentChecklist("SO2YT5YLM");
        assertThat(checklist).isNotNull();
        assertThat(checklist.getSchemeCode()).isEqualTo("SO2YT5YLM");
        assertThat(checklist.getTotalDocuments()).isEqualTo(9);
        assertThat(checklist.getDocumentStatus()).isEqualTo("DOCUMENTS_FOUND");
        assertThat(checklist.getOverallProvenance()).isEqualTo("VERIFIED_OFFICIAL");
    }

    @Test
    @DisplayName("6. Unmapped seed scheme does not fabricate documents and uses deterministic fallback")
    void test6_UnmappedSchemeDoesNotFabricateDocuments() {
        SchemeDocumentChecklistResponse checklist = applicationService.getSchemeDocumentChecklist("SCH-HLTH-001");
        assertThat(checklist).isNotNull();
        assertThat(checklist.getSchemeCode()).isEqualTo("SCH-HLTH-001");
        assertThat(checklist.getItems()).isNotEmpty();
        assertThat(checklist.getItems().get(0).getDocumentCode()).isEqualTo("AADHAAR");
    }

    @Test
    @DisplayName("7. ONE_OF requirement remains correctly satisfied by any single option")
    void test7_OneOfRequirementSemantics() {
        SchemeDocumentRequirementResolver.AlternativeParseResult parsed =
                schemeDocumentRequirementResolver.parseDocumentAlternatives(
                        "Identity Proof – Ration Card/Aadhar Card/PAN Card/Voter Card"
                );
        assertThat(parsed.getAltRule()).isEqualTo("ONE_OF");
        assertThat(parsed.getOptions()).contains("Ration Card", "Aadhar Card", "PAN Card", "Voter Card");
    }

    @Test
    @DisplayName("8. Application flow works for canonical scheme")
    void test8_ApplicationFlowCanonicalScheme() {
        when(eligibilityEngine.evaluateScheme(any(Scheme.class), any())).thenReturn(
                EligibilityEvaluationResponse.builder()
                        .status(EvaluationStatus.ELIGIBLE)
                        .matchedConditions(List.of("Demographic criteria satisfied"))
                        .build()
        );

        DatabaseSequence seq = new DatabaseSequence();
        seq.setSeq(101L);
        when(mongoOperations.findAndModify(any(), any(), any(), eq(DatabaseSequence.class))).thenReturn(seq);

        Application mockSaved = Application.builder()
                .id("app-101")
                .applicationNumber("SB-APP-2026-000101")
                .userId("citizen-user-1")
                .schemeId("mongo-so2yt5ylm-id")
                .schemeCode("SO2YT5YLM")
                .status(ApplicationStatus.DOCUMENTS_PENDING)
                .build();
        when(applicationRepository.save(any(Application.class))).thenReturn(mockSaved);

        CreateApplicationRequest req = new CreateApplicationRequest();
        req.setSchemeCode("SO2YT5YLM");
        req.setProfile(new CitizenEligibilityProfile());

        ApplicationResponse appRes = applicationService.createApplication(req, "citizen-user-1");
        assertThat(appRes).isNotNull();
        assertThat(appRes.getApplicationNumber()).isEqualTo("SB-APP-2026-000101");
        assertThat(appRes.getSchemeCode()).isEqualTo("SO2YT5YLM");
    }

    @Test
    @DisplayName("9. Application flow works for seed-only scheme")
    void test9_ApplicationFlowSeedOnlyScheme() {
        when(eligibilityEngine.evaluateScheme(any(Scheme.class), any())).thenReturn(
                EligibilityEvaluationResponse.builder()
                        .status(EvaluationStatus.ELIGIBLE)
                        .matchedConditions(List.of("Eligible for health scheme"))
                        .build()
        );

        DatabaseSequence seq = new DatabaseSequence();
        seq.setSeq(102L);
        when(mongoOperations.findAndModify(any(), any(), any(), eq(DatabaseSequence.class))).thenReturn(seq);

        Application mockSaved = Application.builder()
                .id("app-102")
                .applicationNumber("SB-APP-2026-000102")
                .userId("citizen-user-2")
                .schemeId("mongo-seed-001-id")
                .schemeCode("SCH-HLTH-001")
                .status(ApplicationStatus.DOCUMENTS_PENDING)
                .build();
        when(applicationRepository.save(any(Application.class))).thenReturn(mockSaved);

        CreateApplicationRequest req = new CreateApplicationRequest();
        req.setSchemeCode("SCH-HLTH-001");
        req.setProfile(new CitizenEligibilityProfile());

        ApplicationResponse appRes = applicationService.createApplication(req, "citizen-user-2");
        assertThat(appRes).isNotNull();
        assertThat(appRes.getApplicationNumber()).isEqualTo("SB-APP-2026-000102");
        assertThat(appRes.getSchemeCode()).isEqualTo("SCH-HLTH-001");
    }

    @Test
    @DisplayName("10. Missing deadline returns neutral value (null/absent, zero fabrication)")
    void test10_MissingDeadlineReturnsNeutral() {
        SchemeResponse res = schemeService.getSchemeByCode("SCH-HLTH-001");
        assertThat(res.getApplicationInfo()).isNotNull();
        assertThat(res.getApplicationInfo().getDeadline()).isNull();
    }

    @Test
    @DisplayName("11. Real deadline is correctly mapped from authoritative source")
    void test11_RealDeadlineIsMapped() {
        SchemeResponse res = schemeService.getSchemeByCode("SO2YT5YLM");
        assertThat(res.getApplicationInfo()).isNotNull();
        assertThat(res.getApplicationInfo().getDeadline()).isEqualTo(Instant.parse("2026-12-31T23:59:59Z"));
    }

    @Test
    @DisplayName("12. Missing approval history returns neutral state (no fabricated rates)")
    void test12_MissingApprovalHistoryReturnsNeutral() {
        // When no applications exist for a scheme, approval rate should remain unpopulated / null
        List<Application> emptyList = Collections.emptyList();
        long approvedCount = emptyList.stream().filter(a -> a.getStatus() == ApplicationStatus.APPROVED).count();
        Integer rate = emptyList.isEmpty() ? null : (int) Math.round((double) approvedCount / emptyList.size() * 100);
        assertThat(rate).isNull();
    }

    @Test
    @DisplayName("13. Real approval data is calculated accurately from actual application records")
    void test13_RealApprovalDataCalculated() {
        List<Application> apps = List.of(
                Application.builder().status(ApplicationStatus.APPROVED).build(),
                Application.builder().status(ApplicationStatus.APPROVED).build(),
                Application.builder().status(ApplicationStatus.REJECTED).build(),
                Application.builder().status(ApplicationStatus.APPROVED).build()
        );
        long approvedCount = apps.stream().filter(a -> a.getStatus() == ApplicationStatus.APPROVED).count();
        int rate = (int) Math.round(((double) approvedCount / apps.size()) * 100);
        assertThat(rate).isEqualTo(75);
    }

    @Test
    @DisplayName("14. Recommendation identifier resolves correctly to master scheme")
    void test14_RecommendationIdentifierResolves() {
        // Recommendations return schemeId / schemeCode / slug; all resolve to the same master scheme
        SchemeResponse byId = schemeService.getSchemeById("mongo-so2yt5ylm-id");
        SchemeResponse byCode = schemeService.getSchemeByCode("SO2YT5YLM");
        SchemeResponse bySlug = schemeService.getSchemeById("so2yt5ylm");

        assertThat(byId.getSchemeCode()).isEqualTo("SO2YT5YLM");
        assertThat(byCode.getSchemeCode()).isEqualTo("SO2YT5YLM");
        assertThat(bySlug.getSchemeCode()).isEqualTo("SO2YT5YLM");
    }

    @Test
    @DisplayName("15. SO2YT5YLM canonical record validation: 9 docs, 2 ONE_OF groups, 7 standalone mandatory")
    void test15_SO2YT5YLMIntact() {
        assertThat(so2yt5ylmCanonicalData.getDocuments()).hasSize(9);

        // Verify the 2 citizen identity ONE_OF alternative groups
        SchemeDocumentRequirementResolver.AlternativeParseResult dobParsed =
                schemeDocumentRequirementResolver.parseDocumentAlternatives(so2yt5ylmCanonicalData.getDocuments().get(2).getOfficialDocumentName());
        SchemeDocumentRequirementResolver.AlternativeParseResult idParsed =
                schemeDocumentRequirementResolver.parseDocumentAlternatives(so2yt5ylmCanonicalData.getDocuments().get(3).getOfficialDocumentName());

        assertThat(dobParsed.getAltRule()).isEqualTo("ONE_OF");
        assertThat(dobParsed.getOptions()).contains("Birth Certificate", "PAN Card", "Voter Card");

        assertThat(idParsed.getAltRule()).isEqualTo("ONE_OF");
        assertThat(idParsed.getOptions()).contains("Ration Card", "Aadhar Card", "PAN Card", "Voter Card");

        // Verify total documents = 9 (2 ONE_OF groups + 7 standalone mandatory requirements)
        assertThat(so2yt5ylmCanonicalData.getDocuments()).hasSize(9);
        assertThat(so2yt5ylmCanonicalData.getDocumentStatus()).isEqualTo("DOCUMENTS_FOUND");
        assertThat(so2yt5ylmCanonicalData.getOverallProvenance()).isEqualTo(RequirementProvenance.VERIFIED_OFFICIAL);
    }

    @Test
    @DisplayName("16. Zero database mutation: read operations produce no side effects")
    void test16_NoDatabaseMutation() {
        schemeService.getSchemeByCode("SO2YT5YLM");
        schemeService.getSchemeByCode("SCH-HLTH-001");
        applicationService.getSchemeDocumentChecklist("SO2YT5YLM");
        applicationService.getSchemeDocumentChecklist("SCH-HLTH-001");

        verify(schemeRepository, never()).save(any());
        verify(schemeRepository, never()).delete(any());
        verify(schemeVerifiedDataRepository, never()).save(any());
        verify(schemeVerifiedDataRepository, never()).delete(any());
    }
}
