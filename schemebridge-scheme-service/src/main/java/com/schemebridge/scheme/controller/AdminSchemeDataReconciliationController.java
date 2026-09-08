package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.document.SchemeVerifiedData;
import com.schemebridge.scheme.dto.response.SchemeReconciliationSummaryResponse;
import com.schemebridge.scheme.service.SchemeOfficialDataReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/scheme-data")
@RequiredArgsConstructor
@Tag(name = "Admin Scheme Data Reconciliation", description = "Endpoints for official scheme data reconciliation, quality metrics, and canonical knowledge base audit")
@SecurityRequirement(name = "BearerAuth")
public class AdminSchemeDataReconciliationController {

    private final SchemeOfficialDataReconciliationService reconciliationService;

    @GetMapping("/reconciliation/summary")
    @Operation(summary = "Get official scheme data reconciliation summary metrics (Admin only)")
    public ResponseEntity<SchemeReconciliationSummaryResponse> getSummary() {
        return ResponseEntity.ok(reconciliationService.getReconciliationSummary());
    }

    @GetMapping("/reconciliation/{schemeCode}")
    @Operation(summary = "Get field-level reconciliation for a scheme (Admin only)")
    public ResponseEntity<SchemeVerifiedData.ReconciliationResult> getReconciliationForScheme(
            @PathVariable String schemeCode) {
        return ResponseEntity.ok(reconciliationService.getReconciliationForScheme(schemeCode));
    }

    @GetMapping("/quality/{schemeCode}")
    @Operation(summary = "Get data quality score and completeness breakdown for a scheme (Admin only)")
    public ResponseEntity<SchemeVerifiedData.DataQualityMetrics> getQualityForScheme(
            @PathVariable String schemeCode) {
        return ResponseEntity.ok(reconciliationService.getQualityMetricsForScheme(schemeCode));
    }

    @GetMapping("/canonical/{schemeCode}")
    @Operation(summary = "Get canonical verified scheme record (Admin only)")
    public ResponseEntity<SchemeVerifiedData> getCanonicalScheme(
            @PathVariable String schemeCode) {
        return reconciliationService.getVerifiedScheme(schemeCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
