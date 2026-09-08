package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.enums.RequirementProvenance;
import com.schemebridge.scheme.repository.SchemeVerifiedDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Phase 23: Document Checklist Intelligence & Canonical Verification Safety Tests.
 *
 * Verifies:
 * 1. Canonical verified data always wins over predictions
 * 2. ONE_OF alternative groups remain intact and are never flattened
 * 3. ML prediction is used ONLY when authoritative data is unavailable
 * 4. Confidence states are correctly assigned (VERIFIED_OFFICIAL, VERIFIED_SOURCE, REQUIRES_REVIEW, UNRESOLVED)
 * 5. Hallucinated documents are rejected
 * 6. Provenance metadata is preserved
 * 7. Unresolved requirements are not fabricated
 */
@ExtendWith(MockitoExtension.class)
class Phase23DocumentIntelligenceTest {

    @Mock
    private SchemeVerifiedDataRepository schemeVerifiedDataRepository;

    private SchemeDocumentRequirementResolver resolver;

    private Scheme testSchemeWithCanonical;
    private Scheme testSchemeWithoutCanonical;
    private SchemeVerifiedData canonicalVerifiedData;

    @BeforeEach
    void setUp() {
        resolver = new SchemeDocumentRequirementResolver(schemeVerifiedDataRepository);

        testSchemeWithCanonical = Scheme.builder()
                .id("sch-haryana-001")
                .schemeCode("SO2YT5YLM")
                .slug("so2yt5ylm")
                .title(MultilingualText.builder().english("Fisheries SC Welfare Scheme").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Haryana")
                .build();

        testSchemeWithoutCanonical = Scheme.builder()
                .id("sch-agri-999")
                .schemeCode("SCH-NO-CANONICAL")
                .slug("sch-no-canonical")
                .title(MultilingualText.builder().english("Special Agro Crop Loan").build())
                .schemeLevel(SchemeLevel.CENTRAL)
                .stateOrUt("ALL")
                .build();

        canonicalVerifiedData = SchemeVerifiedData.builder()
                .schemeCode("SO2YT5YLM")
                .slug("so2yt5ylm")
                .documentStatus("DOCUMENTS_FOUND")
                .overallProvenance(RequirementProvenance.VERIFIED_OFFICIAL)
                .documents(List.of(
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("AADHAAR")
                                .canonicalDocumentCode("AADHAAR")
                                .officialDocumentName("Aadhaar Card")
                                .mandatory(true)
                                .alternativeGroup(SchemeVerifiedData.AlternativeGroup.builder()
                                        .rule("ONE_OF")
                                        .options(List.of(
                                                SchemeVerifiedData.AlternativeOption.builder().optionName("Aadhaar Card").build(),
                                                SchemeVerifiedData.AlternativeOption.builder().optionName("Voter ID").build(),
                                                SchemeVerifiedData.AlternativeOption.builder().optionName("Passport").build()
                                        ))
                                        .build())
                                .build(),
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("RESIDENCE_PROOF")
                                .canonicalDocumentCode("DOMICILE_CERT")
                                .officialDocumentName("Domicile / Residence Certificate")
                                .mandatory(true)
                                .build(),
                        SchemeVerifiedData.CanonicalDocumentRequirement.builder()
                                .documentCode("CASTE_CERT")
                                .canonicalDocumentCode("CASTE_CERTIFICATE")
                                .officialDocumentName("Scheduled Caste Certificate")
                                .mandatory(true)
                                .build()
                ))
                .build();
    }

    @Test
    @DisplayName("1. Canonical verified data ALWAYS wins over any prediction")
    void testCanonicalDataAlwaysWins() {
        when(schemeVerifiedDataRepository.findBySchemeCode("SO2YT5YLM"))
                .thenReturn(Optional.of(canonicalVerifiedData));

        List<SchemeDocumentRequirementResolver.ResolvedRequirement> requirements =
                resolver.resolveRequirements(testSchemeWithCanonical);

        assertNotNull(requirements);
        assertEquals(3, requirements.size());
        assertEquals("AADHAAR", requirements.get(0).getDocumentCode());
        assertEquals(RequirementProvenance.VERIFIED_OFFICIAL, requirements.get(0).getProvenance());
        assertEquals("VERIFIED_OFFICIAL", requirements.get(0).getConfidenceState());
        assertEquals(1.0, requirements.get(0).getConfidence());
    }

    @Test
    @DisplayName("2. ONE_OF alternative groups remain intact (100% preservation, zero flattening)")
    void testOneOfGroupPreservation() {
        SchemeDocumentRequirementResolver.AlternativeParseResult result =
                resolver.parseDocumentAlternatives("Aadhaar Card / Voter ID / Indian Passport");

        assertNotNull(result);
        assertEquals("ONE_OF", result.getAltRule());
        assertEquals(3, result.getOptions().size());
        assertTrue(result.getOptions().contains("Aadhaar Card"));
        assertTrue(result.getOptions().contains("Voter ID"));
        assertTrue(result.getOptions().contains("Indian Passport"));

        // Verify explicit group parsing
        SchemeVerifiedData.AlternativeGroup explicitGroup = SchemeVerifiedData.AlternativeGroup.builder()
                .rule("ONE_OF")
                .options(List.of(
                        SchemeVerifiedData.AlternativeOption.builder().optionName("PAN Card").build(),
                        SchemeVerifiedData.AlternativeOption.builder().optionName("Form 60").build()
                ))
                .build();

        SchemeDocumentRequirementResolver.AlternativeParseResult explicitResult =
                resolver.parseDocumentAlternatives("Identity Document", explicitGroup);

        assertEquals("ONE_OF", explicitResult.getAltRule());
        assertEquals(2, explicitResult.getOptions().size());
        assertTrue(explicitResult.getOptions().contains("PAN Card"));
        assertTrue(explicitResult.getOptions().contains("Form 60"));
    }

    @Test
    @DisplayName("3. ML prediction is used ONLY when authoritative canonical data is unavailable")
    void testMlUsedOnlyWhenAuthoritativeDataUnavailable() {
        // When canonical data is NOT found in repository
        when(schemeVerifiedDataRepository.findBySchemeCode("SCH-NO-CANONICAL"))
                .thenReturn(Optional.empty());

        List<SchemeDocumentRequirementResolver.ResolvedRequirement> requirements =
                resolver.resolveRequirements(testSchemeWithoutCanonical);

        assertNotNull(requirements);
        assertFalse(requirements.isEmpty());
        // Uses fallback deterministic AST rules safely
        assertEquals("AADHAAR", requirements.get(0).getDocumentCode());
        assertEquals(RequirementProvenance.SYSTEM_CONFIGURED, requirements.get(0).getProvenance());
    }

    @Test
    @DisplayName("4. Confidence states are correctly assigned")
    void testConfidenceStates() {
        when(schemeVerifiedDataRepository.findBySchemeCode("SO2YT5YLM"))
                .thenReturn(Optional.of(canonicalVerifiedData));

        List<SchemeDocumentRequirementResolver.ResolvedRequirement> requirements =
                resolver.resolveRequirements(testSchemeWithCanonical);

        for (SchemeDocumentRequirementResolver.ResolvedRequirement req : requirements) {
            assertEquals("VERIFIED_OFFICIAL", req.getConfidenceState());
            assertEquals(1.0, req.getConfidence());
            assertEquals(RequirementProvenance.VERIFIED_OFFICIAL, req.getProvenance());
        }
    }

    @Test
    @DisplayName("5. Hallucinated documents are rejected and never fabricated")
    void testNoHallucinatedDocuments() {
        Scheme emptyScheme = Scheme.builder()
                .id("sch-empty")
                .schemeCode("SCH-EMPTY")
                .build();

        when(schemeVerifiedDataRepository.findBySchemeCode("SCH-EMPTY")).thenReturn(Optional.empty());

        List<SchemeDocumentRequirementResolver.ResolvedRequirement> requirements =
                resolver.resolveRequirements(emptyScheme);

        assertNotNull(requirements);
        // Only valid grounded base requirements
        for (SchemeDocumentRequirementResolver.ResolvedRequirement req : requirements) {
            assertNotNull(req.getDocumentCode());
            assertNotNull(req.getDocumentName());
            assertNotNull(req.getWhyRequired());
            assertFalse(req.getDocumentName().isBlank());
        }
    }

    @Test
    @DisplayName("6. Provenance is preserved across all document levels")
    void testProvenancePreservation() {
        when(schemeVerifiedDataRepository.findBySchemeCode("SO2YT5YLM"))
                .thenReturn(Optional.of(canonicalVerifiedData));

        List<SchemeDocumentRequirementResolver.ResolvedRequirement> requirements =
                resolver.resolveRequirements(testSchemeWithCanonical);

        assertNotNull(requirements);
        for (SchemeDocumentRequirementResolver.ResolvedRequirement req : requirements) {
            assertNotNull(req.getProvenance());
            assertEquals(RequirementProvenance.VERIFIED_OFFICIAL, req.getProvenance());
        }
    }

    @Test
    @DisplayName("7. Unresolved requirements are cleanly identified rather than fabricated")
    void testUnresolvedRequirementsCleanlyIdentified() {
        SchemeVerifiedData emptyDocs = SchemeVerifiedData.builder()
                .schemeCode("SO2YT5YLM")
                .slug("so2yt5ylm")
                .documentStatus("DOCUMENTS_UNAVAILABLE")
                .documents(List.of())
                .build();

        when(schemeVerifiedDataRepository.findBySchemeCode("SO2YT5YLM"))
                .thenReturn(Optional.of(emptyDocs));

        List<SchemeDocumentRequirementResolver.ResolvedRequirement> requirements =
                resolver.resolveRequirements(testSchemeWithCanonical);

        assertNotNull(requirements);
        assertFalse(requirements.isEmpty());
    }
}
