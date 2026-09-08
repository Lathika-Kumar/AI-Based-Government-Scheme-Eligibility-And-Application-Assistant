package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.DocumentChecklistItemResponse;
import com.schemebridge.scheme.dto.response.SchemeDocumentChecklistResponse;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.enums.RequirementProvenance;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
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
class Phase15CanonicalDocumentValidationTest {

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
                .title(MultilingualText.builder().english("Establishment of Fish Culture in Ponds to SC Families on 60% Subsidy").build())
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
                .reconciliationStatus("PARTIAL")
                .documents(List.of(
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("DOC_SO2YT5YLM_001")
                                .officialDocumentName("Agreement 1 - Agreement deed between fish farmer and Fisheries Department")
                                .mandatory(true)
                                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                                .build(),
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("DOC_SO2YT5YLM_003")
                                .officialDocumentName("Date of Birth Certificate - Birth Certificate/Matriculation Certificate/PAN Card/Voter Card/Driving License")
                                .mandatory(true)
                                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                                .build(),
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("DOC_SO2YT5YLM_004")
                                .officialDocumentName("Identity Proof – Ration Card/Aadhar Card/PAN Card/Voter Card")
                                .mandatory(true)
                                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                                .build()
                ))
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
    @DisplayName("1. Checklist retrieval by valid scheme code returns canonical checklist")
    void testGetSchemeDocumentChecklist_ValidScheme() {
        when(schemeRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testScheme));
        when(schemeVerifiedDataRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testCanonicalData));

        SchemeDocumentRequirementResolver.ResolvedRequirement r1 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("DOC_SO2YT5YLM_001")
                .documentName("Agreement 1 - Agreement deed between fish farmer and Fisheries Department")
                .mandatory(true)
                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .build();

        SchemeDocumentRequirementResolver.ResolvedRequirement r2 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("DOC_SO2YT5YLM_003")
                .documentName("Date of Birth Certificate")
                .mandatory(true)
                .alternativeGroupId("ALT_DOC_SO2YT5YLM_003")
                .alternativeGroupType("ONE_OF")
                .alternatives(List.of("Birth Certificate", "Matriculation Certificate", "PAN Card", "Voter Card", "Driving License"))
                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .build();

        SchemeDocumentRequirementResolver.ResolvedRequirement r3 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("DOC_SO2YT5YLM_004")
                .documentName("Identity Proof")
                .mandatory(true)
                .alternativeGroupId("ALT_DOC_SO2YT5YLM_004")
                .alternativeGroupType("ONE_OF")
                .alternatives(List.of("Ration Card", "Aadhar Card", "PAN Card", "Voter Card"))
                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .build();

        when(schemeDocumentRequirementResolver.resolveRequirements(testScheme)).thenReturn(List.of(r1, r2, r3));

        SchemeDocumentChecklistResponse response = applicationService.getSchemeDocumentChecklist("SO2YT5YLM");

        assertThat(response).isNotNull();
        assertThat(response.getSchemeCode()).isEqualTo("SO2YT5YLM");
        assertThat(response.getDocumentStatus()).isEqualTo("DOCUMENTS_FOUND");
        assertThat(response.getTotalDocuments()).isEqualTo(3);
        assertThat(response.getMandatoryDocumentCount()).isEqualTo(3);
        assertThat(response.getAlternativeGroupCount()).isEqualTo(2);
        assertThat(response.getItems()).hasSize(3);

        DocumentChecklistItemResponse dobItem = response.getItems().get(1);
        assertThat(dobItem.getDocumentName()).isEqualTo("Date of Birth Certificate");
        assertThat(dobItem.getAlternativeGroupType()).isEqualTo("ONE_OF");
        assertThat(dobItem.getAlternatives()).hasSize(5);

        DocumentChecklistItemResponse idItem = response.getItems().get(2);
        assertThat(idItem.getDocumentName()).isEqualTo("Identity Proof");
        assertThat(idItem.getAlternativeGroupType()).isEqualTo("ONE_OF");
        assertThat(idItem.getAlternatives()).contains("Aadhar Card", "PAN Card");
    }

    @Test
    @DisplayName("2. Checklist retrieval for unknown scheme throws ResourceNotFoundException")
    void testGetSchemeDocumentChecklist_UnknownScheme() {
        when(schemeRepository.findBySchemeCode("UNKNOWN_SCHEME")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getSchemeDocumentChecklist("UNKNOWN_SCHEME"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("3. Unmapped scheme sets documentStatus to DOCUMENT_REQUIREMENTS_NOT_MAPPED")
    void testGetSchemeDocumentChecklist_UnmappedRequirements() {
        Scheme unmappedScheme = Scheme.builder()
                .id("sch-unmapped")
                .schemeCode("SCH-UNMAPPED")
                .title(MultilingualText.builder().english("Unmapped Scheme").build())
                .build();

        SchemeVerifiedData unmappedData = SchemeVerifiedData.builder()
                .schemeCode("SCH-UNMAPPED")
                .documentStatus("DOCUMENT_REQUIREMENTS_NOT_MAPPED")
                .documents(List.of())
                .build();

        when(schemeRepository.findBySchemeCode("SCH-UNMAPPED")).thenReturn(Optional.of(unmappedScheme));
        when(schemeVerifiedDataRepository.findBySchemeCode("SCH-UNMAPPED")).thenReturn(Optional.of(unmappedData));
        when(schemeDocumentRequirementResolver.resolveRequirements(unmappedScheme)).thenReturn(List.of());

        SchemeDocumentChecklistResponse response = applicationService.getSchemeDocumentChecklist("SCH-UNMAPPED");

        assertThat(response).isNotNull();
        assertThat(response.getDocumentStatus()).isEqualTo("DOCUMENT_REQUIREMENTS_NOT_MAPPED");
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    @DisplayName("4. ONE_OF alternative requirement is satisfied when exactly ONE alternative option is uploaded")
    void testOneOfAlternativeSatisfiedWithSingleOption() {
        when(applicationRepository.findById("app-123")).thenReturn(Optional.of(testApp));
        when(schemeRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testScheme));
        when(schemeVerifiedDataRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testCanonicalData));

        SchemeDocumentRequirementResolver.ResolvedRequirement r1 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("DOC_SO2YT5YLM_001")
                .documentName("Agreement 1")
                .mandatory(true)
                .build();

        SchemeDocumentRequirementResolver.ResolvedRequirement r2 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("DOC_SO2YT5YLM_004")
                .documentName("Identity Proof")
                .mandatory(true)
                .alternativeGroupId("ALT_DOC_SO2YT5YLM_004")
                .alternativeGroupType("ONE_OF")
                .alternatives(List.of("Ration Card", "Aadhar Card", "PAN Card", "Voter Card"))
                .build();

        when(schemeDocumentRequirementResolver.resolveRequirements(testScheme)).thenReturn(List.of(r1, r2));

        // Upload doc1 and only ONE of the alternatives (Aadhar Card) for doc2
        ApplicationDocument doc1 = ApplicationDocument.builder()
                .applicationId("app-123")
                .documentCode("DOC_SO2YT5YLM_001")
                .uploaded(true)
                .detailedStatus(DetailedDocumentStatus.VERIFIED)
                .verificationStatus(DocumentVerificationStatus.VERIFIED)
                .build();

        ApplicationDocument doc2 = ApplicationDocument.builder()
                .applicationId("app-123")
                .documentCode("DOC_SO2YT5YLM_004")
                .fileName("aadhar_card.pdf")
                .uploaded(true)
                .detailedStatus(DetailedDocumentStatus.OCR_COMPLETED)
                .verificationStatus(DocumentVerificationStatus.VERIFIED)
                .build();

        when(applicationDocumentRepository.findAllByApplicationId("app-123")).thenReturn(List.of(doc1, doc2));

        SchemeDocumentChecklistResponse response = applicationService.getDocumentChecklist("app-123", "citizen-1", false);

        assertThat(response.isCanSubmit()).isTrue();
        assertThat(response.getMissingRequirements()).isEmpty();
        assertThat(response.getItems().get(1).isAlternativeSatisfied()).isTrue();
    }

    @Test
    @DisplayName("5. ONE_OF alternative requirement fails submission gate if no alternative is uploaded")
    void testOneOfAlternativeUnsatisfiedFailsSubmission() {
        when(applicationRepository.findById("app-123")).thenReturn(Optional.of(testApp));
        when(schemeRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testScheme));
        when(schemeVerifiedDataRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testCanonicalData));

        SchemeDocumentRequirementResolver.ResolvedRequirement r1 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("DOC_SO2YT5YLM_001")
                .documentName("Agreement 1")
                .mandatory(true)
                .build();

        SchemeDocumentRequirementResolver.ResolvedRequirement r2 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("DOC_SO2YT5YLM_004")
                .documentName("Identity Proof")
                .mandatory(true)
                .alternativeGroupId("ALT_DOC_SO2YT5YLM_004")
                .alternativeGroupType("ONE_OF")
                .alternatives(List.of("Ration Card", "Aadhar Card", "PAN Card", "Voter Card"))
                .build();

        when(schemeDocumentRequirementResolver.resolveRequirements(testScheme)).thenReturn(List.of(r1, r2));

        // Only upload doc1, leaving doc2 completely un-uploaded
        ApplicationDocument doc1 = ApplicationDocument.builder()
                .applicationId("app-123")
                .documentCode("DOC_SO2YT5YLM_001")
                .uploaded(true)
                .detailedStatus(DetailedDocumentStatus.VERIFIED)
                .build();

        when(applicationDocumentRepository.findAllByApplicationId("app-123")).thenReturn(List.of(doc1));

        SchemeDocumentChecklistResponse response = applicationService.getDocumentChecklist("app-123", "citizen-1", false);

        assertThat(response.isCanSubmit()).isFalse();
        assertThat(response.getMissingRequirements()).hasSize(1);
        assertThat(response.getMissingRequirements().get(0)).contains("Identity Proof");
    }

    @Test
    @DisplayName("6. IDOR protection prevents unauthorized citizen from fetching another citizen's checklist")
    void testDocumentChecklist_IdorProtection() {
        when(applicationRepository.findById("app-123")).thenReturn(Optional.of(testApp));

        assertThatThrownBy(() -> applicationService.getDocumentChecklist("app-123", "malicious-user-999", false))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("7. Admin has authority to inspect any citizen application document checklist")
    void testDocumentChecklist_AdminAccess() {
        when(applicationRepository.findById("app-123")).thenReturn(Optional.of(testApp));
        when(schemeRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testScheme));
        when(schemeVerifiedDataRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testCanonicalData));
        when(schemeDocumentRequirementResolver.resolveRequirements(testScheme)).thenReturn(List.of());
        when(applicationDocumentRepository.findAllByApplicationId("app-123")).thenReturn(List.of());

        SchemeDocumentChecklistResponse response = applicationService.getDocumentChecklist("app-123", "admin_officer", true);

        assertThat(response).isNotNull();
        assertThat(response.getSchemeCode()).isEqualTo("SO2YT5YLM");
    }

    @Test
    @DisplayName("8. Resolver parseDocumentAlternatives correctly parses headers and alternative options")
    void testResolverParseDocumentAlternatives() {
        SchemeDocumentRequirementResolver resolver = new SchemeDocumentRequirementResolver();

        // Case A: Dash separated with slashes
        SchemeDocumentRequirementResolver.AlternativeParseResult r1 = resolver.parseDocumentAlternatives(
                "Identity Proof – Ration Card/Aadhar Card/PAN Card/Voter Card"
        );
        assertThat(r1.getAltRule()).isEqualTo("ONE_OF");
        assertThat(r1.getDisplayName()).isEqualTo("Identity Proof");
        assertThat(r1.getOptions()).containsExactly("Ration Card", "Aadhar Card", "PAN Card", "Voter Card");

        // Case B: DOB Certificate
        SchemeDocumentRequirementResolver.AlternativeParseResult r2 = resolver.parseDocumentAlternatives(
                "Date of Birth Certificate - Birth Certificate/Matriculation Certificate/PAN Card/Voter Card/Driving License"
        );
        assertThat(r2.getAltRule()).isEqualTo("ONE_OF");
        assertThat(r2.getDisplayName()).isEqualTo("Date of Birth Certificate");
        assertThat(r2.getOptions()).containsExactly("Birth Certificate", "Matriculation Certificate", "PAN Card", "Voter Card", "Driving License");

        // Case C: Standalone document without alternatives
        SchemeDocumentRequirementResolver.AlternativeParseResult r3 = resolver.parseDocumentAlternatives(
                "Agreement 1 - Agreement deed between fish farmer and Fisheries Department"
        );
        assertThat(r3.getAltRule()).isNull();
        assertThat(r3.getOptions()).isEmpty();
        assertThat(r3.getDisplayName()).isEqualTo("Agreement 1 - Agreement deed between fish farmer and Fisheries Department");
    }
}
