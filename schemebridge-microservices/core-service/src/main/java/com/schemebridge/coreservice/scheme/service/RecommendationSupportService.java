package com.schemebridge.coreservice.scheme.service;

import com.schemebridge.coreservice.scheme.dto.SchemeResponse;
import com.schemebridge.coreservice.scheme.entity.Scheme;
import com.schemebridge.coreservice.scheme.repository.SchemeRepository;
import com.schemebridge.coreservice.scheme.repository.SchemeCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RecommendationSupportService provides recommendation-oriented queries for the Scheme Catalog.
 * This is called internally or via REST by future AI recommendation engines.
 */
@Service
@RequiredArgsConstructor
public class RecommendationSupportService {

    private final SchemeRepository schemeRepository;
    private final SchemeService schemeService;

    @Transactional(readOnly = true)
    public List<SchemeResponse> getRecommendedSchemes(String category, java.math.BigDecimal income) {
        List<Scheme> eligible = schemeRepository.findEligibleSchemes(category, income);
        return eligible.stream().map(schemeService::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SchemeResponse> getFeaturedSchemes() {
        return schemeRepository.findByStatusAndFeaturedTrue("ACTIVE")
                .stream().map(schemeService::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SchemeResponse> getHighPrioritySchemes() {
        return schemeRepository.findByStatusOrderByPriorityAscPopularityScoreDesc("ACTIVE")
                .stream().limit(10).map(schemeService::mapToResponse).collect(Collectors.toList());
    }
}
