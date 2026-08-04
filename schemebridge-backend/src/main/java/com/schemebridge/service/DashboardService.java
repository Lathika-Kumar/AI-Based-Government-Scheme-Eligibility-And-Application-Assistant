package com.schemebridge.service;

import com.schemebridge.dto.DashboardSummaryResponse;

public interface DashboardService {
    DashboardSummaryResponse getDashboardSummary(String userEmail);
}
