package com.schemebridge.schemeservice.service;

import com.schemebridge.schemeservice.dto.SchemeResponse;
import com.schemebridge.schemeservice.repository.TrendingSchemeRepository;
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
