package com.schemebridge.coreservice.migration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MySchemeImportReport {
    private String mode; // TEST_10, FULL
    private int totalFilesFound;
    private int processedCount;
    private int insertedCount;
    private int updatedCount;
    private int skippedCount;
    private int failedCount;
    private long executionTimeMs;
    private String status; // SUCCESS, COMPLETED_WITH_ERRORS, FAILED
    
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
