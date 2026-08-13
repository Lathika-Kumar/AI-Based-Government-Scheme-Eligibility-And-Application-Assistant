package com.schemebridge.coreservice.scheme.service;

import com.schemebridge.coreservice.scheme.dto.SchemeResponse;
import com.schemebridge.coreservice.scheme.repository.TrendingSchemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrendingSchemeService {

    private final TrendingSchemeRepository trendingSchemeRepository;
    private final SchemeService schemeService;

    @Transactional(readOnly = true)
    public List<SchemeResponse> getTrendingSchemes() {
        return trendingSchemeRepository.findByStatusOrderByRankPositionAsc("ACTIVE")
                .stream()
                .map(t -> schemeService.mapToResponse(t.getScheme()))
                .collect(Collectors.toList());
    }
}
