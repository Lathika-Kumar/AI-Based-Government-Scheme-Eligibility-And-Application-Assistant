package com.schemebridge.coreservice.migration;

import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/migration")
@RequiredArgsConstructor
@Tag(name = "Data Migration", description = "Admin endpoint to trigger Phase 2B myScheme data migration into Oracle")
public class MySchemeImportController {

    private final MySchemeImportService importService;

    @PostMapping("/import")
    @Operation(summary = "Run myScheme Data Migration", description = "Triggers Phase 2B myScheme migration into Oracle DB. Mode options: TEST_10 or FULL.")
    public ResponseEntity<ApiResponse<MySchemeImportReport>> runMigration(
            @RequestParam(defaultValue = "TEST_10") String mode) {
        MySchemeImportReport report = importService.runImport(mode);
        return ResponseEntity.ok(ApiResponse.success("Migration execution completed [Mode: " + mode + "]", report));
    }
}
