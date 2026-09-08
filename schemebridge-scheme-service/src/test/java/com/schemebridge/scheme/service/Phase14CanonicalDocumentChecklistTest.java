package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.DocumentChecklistItemResponse;
import com.schemebridge.scheme.dto.response.SchemeDocumentChecklistResponse;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.enums.RequirementProvenance;
import com.schemebridge.scheme.repository.ApplicationDocumentRepository;
import com.schemebridge.scheme.repository.ApplicationRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import com.schemebridge.scheme.repository.SchemeVerifiedDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Phase14CanonicalDocumentChecklistTest {

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Mock
    private SchemeVerifiedDataRepository schemeVerifiedDataRepository;

    @Mock
    private SchemeDocumentRequirementResolver schemeDocumentRequirementResolver;

    @InjectMocks
    private ApplicationService applicationService;

    private Scheme testScheme;
    private SchemeVerifiedData testCanonicalData;
    private Application testApp;

    @BeforeEach
    void setUp() {
        testScheme = Scheme.builder()
                .id("sch-001")
                .schemeCode("SO2YT5YLM")
                .slug("so2yt5ylm")
                .title(MultilingualText.builder().english("Fisheries SC Welfare Scheme").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Haryana")
                .status(SchemeStatus.ACTIVE)
                .build();

        testCanonicalData = SchemeVerifiedData.builder()
                .schemeCode("SO2YT5YLM")
                .slug("so2yt5ylm")
                .documentStatus("DOCUMENTS_FOUND")
                .overallProvenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .sourceMetadata(SchemeVerifiedData.CanonicalSourceMetadata.builder()
                        .sourceType("MYSCHEME_OFFICIAL_API")
                        .sourceUrl("https://www.myscheme.gov.in/schemes/so2yt5ylm")
                        .lastVerifiedAt(Instant.now())
                        .build())
                .reconciliationStatus("MATCHED")
                .build();

        testApp = Application.builder()
                .id("app-123")
                .applicationNumber("SB-APP-2026-000100")
                .userId("citizen-1")
                .schemeCode("SO2YT5YLM")
                .status(ApplicationStatus.READY_FOR_SUBMISSION)
                .build();

        applicationService.setSchemeDocumentRequirementResolver(schemeDocumentRequirementResolver);
        applicationService.setSchemeVerifiedDataRepository(schemeVerifiedDataRepository);
    }

    @Test
    @DisplayName("Test 1: Scheme checklist returns canonical verified documents with official source URL")
    void testGetSchemeDocumentChecklist() {
        when(schemeRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testScheme));
        when(schemeVerifiedDataRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testCanonicalData));

        SchemeDocumentRequirementResolver.ResolvedRequirement req1 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("AGREEMENT_01")
                .documentName("Agreement deed with Fisheries Dept")
                .mandatory(true)
                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .officialSourceUrl("https://www.myscheme.gov.in/schemes/so2yt5ylm")
                .build();

        SchemeDocumentRequirementResolver.ResolvedRequirement req2 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("IDENTITY_PROOF")
                .documentName("Identity Proof")
                .mandatory(true)
                .alternativeGroupId("ALT_IDENTITY")
                .alternativeGroupType("ONE_OF")
                .alternatives(List.of("Ration Card", "Aadhaar Card", "PAN Card", "Voter Card"))
                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .officialSourceUrl("https://www.myscheme.gov.in/schemes/so2yt5ylm")
                .build();

        when(schemeDocumentRequirementResolver.resolveRequirements(testScheme)).thenReturn(List.of(req1, req2));

        SchemeDocumentChecklistResponse response = applicationService.getSchemeDocumentChecklist("SO2YT5YLM");

        assertThat(response).isNotNull();
        assertThat(response.getSchemeCode()).isEqualTo("SO2YT5YLM");
        assertThat(response.getDocumentStatus()).isEqualTo("DOCUMENTS_FOUND");
        assertThat(response.getOverallProvenance()).isEqualTo("VERIFIED_OFFICIAL");
        assertThat(response.getOfficialSourceUrl()).isEqualTo("https://www.myscheme.gov.in/schemes/so2yt5ylm");
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(1).getAlternativeGroupType()).isEqualTo("ONE_OF");
        assertThat(response.getItems().get(1).getAlternatives()).contains("Aadhaar Card", "PAN Card");
    }

    @Test
    @DisplayName("Test 2: Application checklist satisfies submission gate when ONE_OF alternative is uploaded")
    void testApplicationChecklistWithOneOfAlternativeSatisfied() {
        when(applicationRepository.findById("app-123")).thenReturn(Optional.of(testApp));
        when(schemeRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testScheme));
        when(schemeVerifiedDataRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testCanonicalData));

        SchemeDocumentRequirementResolver.ResolvedRequirement req1 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("AGREEMENT_01")
                .documentName("Agreement deed")
                .mandatory(true)
                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .build();

        SchemeDocumentRequirementResolver.ResolvedRequirement req2 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("IDENTITY_PROOF")
                .documentName("Identity Proof")
                .mandatory(true)
                .alternativeGroupId("ALT_IDENTITY")
                .alternativeGroupType("ONE_OF")
                .alternatives(List.of("Aadhaar Card", "PAN Card"))
                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .build();

        when(schemeDocumentRequirementResolver.resolveRequirements(testScheme)).thenReturn(List.of(req1, req2));

        ApplicationDocument doc1 = ApplicationDocument.builder()
                .applicationId("app-123")
                .documentCode("AGREEMENT_01")
                .uploaded(true)
                .detailedStatus(DetailedDocumentStatus.VERIFIED)
                .verificationStatus(DocumentVerificationStatus.VERIFIED)
                .build();

        ApplicationDocument doc2 = ApplicationDocument.builder()
                .applicationId("app-123")
                .documentCode("IDENTITY_PROOF")
                .uploaded(true)
                .detailedStatus(DetailedDocumentStatus.VERIFIED)
                .verificationStatus(DocumentVerificationStatus.VERIFIED)
                .build();

        when(applicationDocumentRepository.findAllByApplicationId("app-123")).thenReturn(List.of(doc1, doc2));

        SchemeDocumentChecklistResponse response = applicationService.getDocumentChecklist("app-123", "citizen-1", false);

        assertThat(response).isNotNull();
        assertThat(response.isCanSubmit()).isTrue();
        assertThat(response.getMissingRequirements()).isEmpty();
    }

    @Test
    @DisplayName("Test 3: Application checklist blocks submission when ONE_OF alternative is not uploaded")
    void testApplicationChecklistWithOneOfAlternativeUnsatisfied() {
        when(applicationRepository.findById("app-123")).thenReturn(Optional.of(testApp));
        when(schemeRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testScheme));
        when(schemeVerifiedDataRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testCanonicalData));

        SchemeDocumentRequirementResolver.ResolvedRequirement req1 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("AGREEMENT_01")
                .documentName("Agreement deed")
                .mandatory(true)
                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .build();

        SchemeDocumentRequirementResolver.ResolvedRequirement req2 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("IDENTITY_PROOF")
                .documentName("Identity Proof")
                .mandatory(true)
                .alternativeGroupId("ALT_IDENTITY")
                .alternativeGroupType("ONE_OF")
                .alternatives(List.of("Aadhaar Card", "PAN Card"))
                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .build();

        when(schemeDocumentRequirementResolver.resolveRequirements(testScheme)).thenReturn(List.of(req1, req2));

        // Only doc1 is uploaded, alternative group is not uploaded
        ApplicationDocument doc1 = ApplicationDocument.builder()
                .applicationId("app-123")
                .documentCode("AGREEMENT_01")
                .uploaded(true)
                .detailedStatus(DetailedDocumentStatus.OCR_COMPLETED)
                .build();

        when(applicationDocumentRepository.findAllByApplicationId("app-123")).thenReturn(List.of(doc1));

        SchemeDocumentChecklistResponse response = applicationService.getDocumentChecklist("app-123", "citizen-1", false);

        assertThat(response).isNotNull();
        assertThat(response.isCanSubmit()).isFalse();
        assertThat(response.getMissingRequirements()).isNotEmpty();
    }

    @Test
    @DisplayName("Test 4: Citizen IDOR protection prevents unauthorized access to application checklist")
    void testIdorProtection() {
        when(applicationRepository.findById("app-123")).thenReturn(Optional.of(testApp));

        assertThatThrownBy(() -> applicationService.getDocumentChecklist("app-123", "unauthorized_user_99", false))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("permission");
    }
}
