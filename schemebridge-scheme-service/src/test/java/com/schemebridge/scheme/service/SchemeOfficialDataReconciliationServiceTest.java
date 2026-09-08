package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.document.SchemeVerifiedData;
import com.schemebridge.scheme.dto.response.SchemeReconciliationSummaryResponse;
import com.schemebridge.scheme.repository.SchemeRepository;
import com.schemebridge.scheme.repository.SchemeVerifiedDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemeOfficialDataReconciliationServiceTest {

    @Mock
    private SchemeVerifiedDataRepository verifiedDataRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private SchemeOfficialDataReconciliationService reconciliationService;

    private SchemeVerifiedData testVerifiedScheme;

    @BeforeEach
    void setUp() {
        testVerifiedScheme = SchemeVerifiedData.builder()
                .schemeCode("SO2YT5YLM")
                .slug("so2yt5ylm")
                .documentStatus("DOCUMENTS_FOUND")
                .reconciliationStatus("MATCHED")
                .reconciliation(SchemeVerifiedData.ReconciliationResult.builder()
                        .status("MATCHED")
                        .matchedFields(List.of("schemeName", "slug", "department", "eligibilityText", "requiredDocuments"))
                        .missingFields(List.of())
                        .conflictingFields(List.of())
                        .lastReconciled(Instant.now())
                        .build())
                .qualityMetrics(SchemeVerifiedData.DataQualityMetrics.builder()
                        .completenessScore(1.0)
                        .identityCompleteness(1.0)
                        .eligibilityCompleteness(1.0)
                        .documentCompleteness(1.0)
                        .benefitCompleteness(1.0)
                        .applicationCompleteness(1.0)
                        .missingFields(List.of())
                        .build())
                .build();
    }

    @Test
    @DisplayName("Test 1: Reconciliation summary aggregates counts properly")
    void testReconciliationSummary() {
        when(verifiedDataRepository.count()).thenReturn(4682L);
        when(verifiedDataRepository.countByReconciliationStatus("MATCHED")).thenReturn(864L);
        when(verifiedDataRepository.countByReconciliationStatus("PARTIAL")).thenReturn(3818L);
        when(verifiedDataRepository.countByDocumentStatus("DOCUMENTS_FOUND")).thenReturn(736L);
        when(verifiedDataRepository.countByDocumentStatus("DOCUMENT_REQUIREMENTS_NOT_MAPPED")).thenReturn(3946L);

        SchemeReconciliationSummaryResponse summary = reconciliationService.getReconciliationSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalSchemes()).isEqualTo(4682L);
        assertThat(summary.getMatched()).isEqualTo(4682L); // 864 + 3818
        assertThat(summary.getDocumentMapped()).isEqualTo(736L);
        assertThat(summary.getDocumentUnmapped()).isEqualTo(3946L);
    }

    @Test
    @DisplayName("Test 2: Retrieve field-level reconciliation for verified scheme")
    void testGetReconciliationForScheme() {
        when(verifiedDataRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testVerifiedScheme));

        SchemeVerifiedData.ReconciliationResult result = reconciliationService.getReconciliationForScheme("SO2YT5YLM");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("MATCHED");
        assertThat(result.getMatchedFields()).contains("schemeName", "requiredDocuments");
    }

    @Test
    @DisplayName("Test 3: Retrieve quality metrics for verified scheme")
    void testGetQualityMetricsForScheme() {
        when(verifiedDataRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testVerifiedScheme));

        SchemeVerifiedData.DataQualityMetrics metrics = reconciliationService.getQualityMetricsForScheme("SO2YT5YLM");

        assertThat(metrics).isNotNull();
        assertThat(metrics.getCompletenessScore()).isEqualTo(1.0);
        assertThat(metrics.getDocumentCompleteness()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Test 4: Retrieve canonical verified scheme record")
    void testGetVerifiedScheme() {
        when(verifiedDataRepository.findBySchemeCode("SO2YT5YLM")).thenReturn(Optional.of(testVerifiedScheme));

        Optional<SchemeVerifiedData> result = reconciliationService.getVerifiedScheme("SO2YT5YLM");

        assertThat(result).isPresent();
        assertThat(result.get().getSchemeCode()).isEqualTo("SO2YT5YLM");
        assertThat(result.get().getDocumentStatus()).isEqualTo("DOCUMENTS_FOUND");
    }
}
