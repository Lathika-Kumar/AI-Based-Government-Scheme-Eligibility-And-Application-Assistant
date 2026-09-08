package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.DocumentChecklistItemResponse;
import com.schemebridge.scheme.dto.response.SchemeDocumentChecklistResponse;
import com.schemebridge.scheme.dto.response.SchemeResponse;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.enums.RequirementProvenance;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.repository.SchemeRepository;
import com.schemebridge.scheme.repository.SchemeVerifiedDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Phase16SchemeConsistencyValidationTest {

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private SchemeVerifiedDataRepository schemeVerifiedDataRepository;

    @Mock
    private SchemeDocumentRequirementResolver schemeDocumentRequirementResolver;

    @InjectMocks
    private SchemeService schemeService;

    @InjectMocks
    private ApplicationService applicationService;

    private Scheme centralScheme;
    private Scheme stateScheme;
    private SchemeVerifiedData canonicalData;

    @BeforeEach
    void setUp() {
        centralScheme = Scheme.builder()
                .id("mongo-id-001")
                .schemeCode("SO2YT5YLM")
                .slug("so2yt5ylm")
                .title(MultilingualText.builder().english("Establishment of Fish Culture in Ponds to SC Families on 60% Subsidy").build())
                .description(MultilingualText.builder().english("Comprehensive financial subsidy to SC families").build())
                .schemeLevel(SchemeLevel.CENTRAL)
                .status(SchemeStatus.ACTIVE)
                .applicationInfo(ApplicationInfo.builder()
                        .applicationMode("ONLINE")
                        .applicationUrl("https://fisheries.gov.in")
                        .deadline(Instant.parse("2026-12-31T23:59:59Z"))
                        .build())
                .build();

        stateScheme = Scheme.builder()
                .id("mongo-id-002")
                .schemeCode("UK-AMB-108")
                .slug("108-emergency-ambulance-service-uttarakhand")
                .title(MultilingualText.builder().english("108, Emergency Ambulance Service - Uttarakhand").build())
                .description(MultilingualText.builder().english("24/7 free emergency medical response service").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Uttarakhand")
                .status(SchemeStatus.ACTIVE)
                .applicationInfo(ApplicationInfo.builder()
                        .applicationMode("OFFLINE")
                        .build())
                .build();

        canonicalData = SchemeVerifiedData.builder()
                .schemeCode("SO2YT5YLM")
                .slug("so2yt5ylm")
                .reconciliationStatus("MATCHED")
                .documentStatus("DOCUMENTS_FOUND")
                .overallProvenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .build();

        applicationService.setSchemeDocumentRequirementResolver(schemeDocumentRequirementResolver);
        applicationService.setSchemeVerifiedDataRepository(schemeVerifiedDataRepository);
    }

    // =========================================================================
    // ISSUE 3: RESILIENT SCHEME LOOKUP BY ID, SCHEMECODE, SLUG
    // =========================================================================

    @Test
    @DisplayName("Issue 3: getSchemeById successfully finds scheme by MongoDB ObjectId")
    void testGetSchemeById_ByMongoId() {
        when(schemeRepository.findById("mongo-id-001")).thenReturn(Optional.of(centralScheme));

        SchemeResponse res = schemeService.getSchemeById("mongo-id-001");
        assertThat(res).isNotNull();
        assertThat(res.getSchemeCode()).isEqualTo("SO2YT5YLM");
        assertThat(res.getSlug()).isEqualTo("so2yt5ylm");
    }

    @Test
    @DisplayName("Issue 3: getSchemeById fallback finds scheme by SchemeCode")
    void testGetSchemeById_FallbackSchemeCode() {
        when(schemeRepository.findById("SO2YT5YLM")).thenReturn(Optional.empty());
        when(schemeRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(centralScheme));

        SchemeResponse res = schemeService.getSchemeById("SO2YT5YLM");
        assertThat(res).isNotNull();
        assertThat(res.getSchemeCode()).isEqualTo("SO2YT5YLM");
    }

    @Test
    @DisplayName("Issue 3: getSchemeById fallback finds scheme by Slug (e.g. from route /scheme/:slug)")
    void testGetSchemeById_FallbackSlug() {
        when(schemeRepository.findById("so2yt5ylm")).thenReturn(Optional.empty());
        when(schemeRepository.findBySchemeCode("so2yt5ylm")).thenReturn(Optional.empty());
        when(schemeRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(centralScheme));

        SchemeResponse res = schemeService.getSchemeById("so2yt5ylm");
        assertThat(res).isNotNull();
        assertThat(res.getSchemeCode()).isEqualTo("SO2YT5YLM");
        assertThat(res.getSlug()).isEqualTo("so2yt5ylm");
    }

    @Test
    @DisplayName("Issue 3: getSchemeById fallback finds State Scheme by Slug")
    void testGetSchemeById_StateSchemeBySlug() {
        when(schemeRepository.findById("108-emergency-ambulance-service-uttarakhand")).thenReturn(Optional.empty());
        when(schemeRepository.findBySchemeCode("108-emergency-ambulance-service-uttarakhand")).thenReturn(Optional.empty());
        when(schemeRepository.findBySchemeCode("108-EMERGENCY-AMBULANCE-SERVICE-UTTARAKHAND")).thenReturn(Optional.empty());
        when(schemeRepository.findBySlug("108-emergency-ambulance-service-uttarakhand")).thenReturn(Optional.of(stateScheme));

        SchemeResponse res = schemeService.getSchemeById("108-emergency-ambulance-service-uttarakhand");
        assertThat(res).isNotNull();
        assertThat(res.getSchemeCode()).isEqualTo("UK-AMB-108");
        assertThat(res.getSchemeLevel()).isEqualTo(SchemeLevel.STATE);
        assertThat(res.getStateOrUt()).isEqualTo("Uttarakhand");
    }

    @Test
    @DisplayName("Issue 3: getSchemeById throws ResourceNotFoundException when non-existent identifier queried")
    void testGetSchemeById_NotFound() {
        when(schemeRepository.findById("non-existent")).thenReturn(Optional.empty());
        when(schemeRepository.findBySchemeCode("non-existent")).thenReturn(Optional.empty());
        when(schemeRepository.findBySchemeCode("NON-EXISTENT")).thenReturn(Optional.empty());
        when(schemeRepository.findBySlug("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schemeService.getSchemeById("non-existent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Scheme not found with ID/Code/Slug: non-existent");
    }

    // =========================================================================
    // ISSUE 1: CANONICAL DOCUMENT REQUIREMENTS RESOLUTION FOR ADMIN/CITIZEN
    // =========================================================================

    @Test
    @DisplayName("Issue 1: getSchemeDocumentChecklist resolves canonical requirements with ONE_OF alternatives")
    void testGetSchemeDocumentChecklist_CanonicalWithOneOf() {
        when(schemeRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(centralScheme));
        when(schemeVerifiedDataRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(canonicalData));

        SchemeDocumentRequirementResolver.ResolvedRequirement req1 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("DOC-ID-PROOF")
                .documentName("Identity Proof")
                .mandatory(true)
                .alternativeGroupId("ALT-ID-01")
                .alternativeGroupType("ONE_OF")
                .alternatives(List.of("Aadhaar Card", "PAN Card", "Voter ID Card", "Ration Card"))
                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .build();

        SchemeDocumentRequirementResolver.ResolvedRequirement req2 = SchemeDocumentRequirementResolver.ResolvedRequirement.builder()
                .documentCode("DOC-CASTE-CERT")
                .documentName("Caste Certificate (SC/ST)")
                .mandatory(true)
                .provenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .build();

        when(schemeDocumentRequirementResolver.resolveRequirements(centralScheme)).thenReturn(List.of(req1, req2));

        SchemeDocumentChecklistResponse checklist = applicationService.getSchemeDocumentChecklist("SO2YT5YLM");

        assertThat(checklist).isNotNull();
        assertThat(checklist.getSchemeCode()).isEqualTo("SO2YT5YLM");
        assertThat(checklist.getTotalDocuments()).isEqualTo(2);
        assertThat(checklist.getMandatoryDocumentCount()).isEqualTo(2);
        assertThat(checklist.getItems()).hasSize(2);

        DocumentChecklistItemResponse item1 = checklist.getItems().get(0);
        assertThat(item1.getDocumentName()).isEqualTo("Identity Proof");
        assertThat(item1.isMandatory()).isTrue();
        assertThat(item1.getAlternativeGroupType()).isEqualTo("ONE_OF");
        assertThat(item1.getAlternatives()).containsExactly("Aadhaar Card", "PAN Card", "Voter ID Card", "Ration Card");

        DocumentChecklistItemResponse item2 = checklist.getItems().get(1);
        assertThat(item2.getDocumentName()).isEqualTo("Caste Certificate (SC/ST)");
        assertThat(item2.isMandatory()).isTrue();
        assertThat(item2.getAlternativeGroupType()).isNull();
    }

    @Test
    @DisplayName("Issue 1: getSchemeDocumentChecklist fallback resolves by slug")
    void testGetSchemeDocumentChecklist_ResolvedBySlug() {
        when(schemeRepository.findBySchemeCode("so2yt5ylm")).thenReturn(Optional.empty());
        when(schemeRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(centralScheme));
        when(schemeVerifiedDataRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(canonicalData));
        when(schemeDocumentRequirementResolver.resolveRequirements(centralScheme)).thenReturn(List.of());

        SchemeDocumentChecklistResponse checklist = applicationService.getSchemeDocumentChecklist("so2yt5ylm");
        assertThat(checklist).isNotNull();
        assertThat(checklist.getSchemeCode()).isEqualTo("SO2YT5YLM");
    }

    // =========================================================================
    // ISSUE 2: DEADLINE & APPROVAL RATE AUTHORITATIVE DATA MAPPING
    // =========================================================================

    @Test
    @DisplayName("Issue 2: SchemeResponse preserves authoritative deadline when present")
    void testSchemeResponse_MapsAuthoritativeDeadline() {
        when(schemeRepository.findById("mongo-id-001")).thenReturn(Optional.of(centralScheme));

        SchemeResponse res = schemeService.getSchemeById("mongo-id-001");
        assertThat(res.getApplicationInfo()).isNotNull();
        assertThat(res.getApplicationInfo().getDeadline()).isEqualTo(Instant.parse("2026-12-31T23:59:59Z"));
    }

    @Test
    @DisplayName("Issue 2: SchemeResponse returns null deadline when not present in authoritative source (zero fabrication)")
    void testSchemeResponse_PreservesNullDeadlinesWithoutFabrication() {
        when(schemeRepository.findById("mongo-id-002")).thenReturn(Optional.of(stateScheme));

        SchemeResponse res = schemeService.getSchemeById("mongo-id-002");
        assertThat(res.getApplicationInfo()).isNotNull();
        assertThat(res.getApplicationInfo().getDeadline()).isNull();
    }
}
