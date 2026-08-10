package com.schemebridge.adminservice.strategy;

import com.schemebridge.adminservice.enums.ReportFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory class resolving the correct ReportExportStrategy implementation
 * based on requested ReportFormat (PDF, EXCEL, CSV).
 */
@Component
@RequiredArgsConstructor
public class ReportExportStrategyFactory {

    private final List<ReportExportStrategy> strategies;

    public ReportExportStrategy getStrategy(ReportFormat format) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(format))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No report export strategy registered for format: " + format));
    }
}
