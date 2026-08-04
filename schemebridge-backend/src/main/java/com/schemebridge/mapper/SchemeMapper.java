package com.schemebridge.mapper;

import com.schemebridge.dto.SchemeCardResponse;
import com.schemebridge.dto.SchemeRequest;
import com.schemebridge.dto.SchemeResponse;
import com.schemebridge.entity.Scheme;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SchemeMapper {

    public SchemeResponse toSchemeResponse(Scheme scheme) {
        if (scheme == null) return null;

        return SchemeResponse.builder()
                .id(scheme.getId())
                .schemeName(scheme.getSchemeName())
                .schemeCode(scheme.getSchemeCode())
                .description(scheme.getDescription())
                .shortDescription(scheme.getShortDescription())
                .benefits(scheme.getBenefits())
                .category(scheme.getCategory())
                .subcategory(scheme.getSubcategory())
                .schemeType(scheme.getSchemeType())
                .applicableStates(scheme.getApplicableStates())
                .minimumAge(scheme.getMinimumAge())
                .maximumAge(scheme.getMaximumAge())
                .minimumIncome(scheme.getMinimumIncome())
                .maximumIncome(scheme.getMaximumIncome())
                .allowedGenders(scheme.getAllowedGenders())
                .allowedCategories(scheme.getAllowedCategories())
                .allowedOccupations(scheme.getAllowedOccupations())
                .requiredDocuments(scheme.getRequiredDocuments())
                .websiteUrl(scheme.getWebsiteUrl())
                .applicationMode(scheme.getApplicationMode())
                .applicationStartDate(scheme.getApplicationStartDate())
                .applicationEndDate(scheme.getApplicationEndDate())
                .alwaysOpen(scheme.getAlwaysOpen())
                .launchDate(scheme.getLaunchDate())
                .lastUpdated(scheme.getLastUpdated())
                .status(scheme.getStatus())
                .featured(scheme.getFeatured())
                .priority(scheme.getPriority())
                .thumbnailUrl(scheme.getThumbnailUrl())
                .galleryImages(scheme.getGalleryImages())
                .tags(scheme.getTags())
                .viewCount(scheme.getViewCount())
                .recommendationCount(scheme.getRecommendationCount())
                .maxBeneficiaries(scheme.getMaxBeneficiaries())
                .currentBeneficiaries(scheme.getCurrentBeneficiaries())
                .schemeVersion(scheme.getSchemeVersion())
                .lastUpdatedAt(scheme.getLastUpdatedAt())
                .lastUpdatedBy(scheme.getLastUpdatedBy())
                .deleted(scheme.getDeleted())
                .createdAt(scheme.getCreatedAt())
                .updatedAt(scheme.getUpdatedAt())
                .build();
    }

    public SchemeCardResponse toSchemeCardResponse(Scheme scheme) {
        if (scheme == null) return null;

        return SchemeCardResponse.builder()
                .id(scheme.getId())
                .schemeName(scheme.getSchemeName())
                .schemeCode(scheme.getSchemeCode())
                .shortDescription(scheme.getShortDescription())
                .category(scheme.getCategory())
                .schemeType(scheme.getSchemeType())
                .featured(scheme.getFeatured())
                .matchPercentage(0)
                .thumbnailUrl(scheme.getThumbnailUrl())
                .benefits(scheme.getBenefits())
                .tags(scheme.getTags())
                .build();
    }

    public Scheme toEntity(SchemeRequest request, String createdBy) {
        if (request == null) return null;

        return Scheme.builder()
                .schemeName(request.getSchemeName() != null ? request.getSchemeName().trim() : null)
                .schemeCode(request.getSchemeCode() != null ? request.getSchemeCode().trim().toUpperCase() : null)
                .description(request.getDescription())
                .shortDescription(request.getShortDescription())
                .benefits(request.getBenefits())
                .category(request.getCategory())
                .subcategory(request.getSubcategory())
                .schemeType(request.getSchemeType())
                .applicableStates(request.getApplicableStates())
                .minimumAge(request.getMinimumAge())
                .maximumAge(request.getMaximumAge())
                .minimumIncome(request.getMinimumIncome())
                .maximumIncome(request.getMaximumIncome())
                .allowedGenders(request.getAllowedGenders())
                .allowedCategories(request.getAllowedCategories())
                .allowedOccupations(request.getAllowedOccupations())
                .requiredDocuments(request.getRequiredDocuments())
                .websiteUrl(request.getWebsiteUrl())
                .applicationMode(request.getApplicationMode())
                .applicationStartDate(request.getApplicationStartDate())
                .applicationEndDate(request.getApplicationEndDate())
                .alwaysOpen(request.getAlwaysOpen() != null ? request.getAlwaysOpen() : true)
                .launchDate(request.getLaunchDate())
                .lastUpdated(LocalDateTime.now())
                .status(request.getStatus() != null ? request.getStatus() : com.schemebridge.enums.SchemeStatus.DRAFT)
                .featured(request.getFeatured() != null ? request.getFeatured() : false)
                .priority(request.getPriority() != null ? request.getPriority() : 1)
                .thumbnailUrl(request.getThumbnailUrl())
                .galleryImages(request.getGalleryImages())
                .tags(request.getTags())
                .maxBeneficiaries(request.getMaxBeneficiaries())
                .currentBeneficiaries(0L)
                .viewCount(0L)
                .recommendationCount(0L)
                .schemeVersion(1)
                .lastUpdatedAt(LocalDateTime.now())
                .lastUpdatedBy(createdBy)
                .deleted(false)
                .build();
    }
}
