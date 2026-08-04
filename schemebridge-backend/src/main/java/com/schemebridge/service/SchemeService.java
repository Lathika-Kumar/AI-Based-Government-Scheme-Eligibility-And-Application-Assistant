package com.schemebridge.service;

import com.schemebridge.dto.CategoryResponse;
import com.schemebridge.dto.EligibilityCheckResponse;
import com.schemebridge.dto.SchemeCardResponse;
import com.schemebridge.dto.SchemeRecommendationResponse;
import com.schemebridge.dto.SchemeRequest;
import com.schemebridge.dto.SchemeResponse;
import com.schemebridge.enums.SchemeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SchemeService {

    SchemeResponse createScheme(SchemeRequest request, String adminEmail);

    SchemeResponse updateScheme(String id, SchemeRequest request, String adminEmail);

    void softDeleteScheme(String id, String adminEmail);

    SchemeResponse publishScheme(String id, String adminEmail);

    SchemeResponse getScheme(String id);

    SchemeResponse getSchemePreview(String id);

    Page<SchemeCardResponse> listSchemes(String category, SchemeType type, String state, Boolean featured, Pageable pageable);

    List<CategoryResponse> getCategories();

    List<SchemeCardResponse> getFeaturedSchemes();

    List<SchemeCardResponse> getTrendingSchemes();

    List<SchemeCardResponse> getLatestSchemes();

    Page<SchemeCardResponse> searchSchemes(String query, Pageable pageable);

    List<SchemeRecommendationResponse> getRecommendations(String userEmail);

    EligibilityCheckResponse checkEligibility(String schemeId, String userEmail);
}
