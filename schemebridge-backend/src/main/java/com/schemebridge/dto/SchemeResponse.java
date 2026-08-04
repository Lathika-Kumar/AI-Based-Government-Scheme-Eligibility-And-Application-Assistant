package com.schemebridge.dto;

import com.schemebridge.enums.SchemeStatus;
import com.schemebridge.enums.SchemeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeResponse {

    private String id;
    private String schemeName;
    private String schemeCode;
    private String description;
    private String shortDescription;
    private List<String> benefits;
    private String category;
    private String subcategory;
    private SchemeType schemeType;
    private List<String> applicableStates;
    private Integer minimumAge;
    private Integer maximumAge;
    private BigDecimal minimumIncome;
    private BigDecimal maximumIncome;
    private List<String> allowedGenders;
    private List<String> allowedCategories;
    private List<String> allowedOccupations;
    private List<String> requiredDocuments;
    private String websiteUrl;
    private String applicationMode;
    private LocalDate applicationStartDate;
    private LocalDate applicationEndDate;
    private Boolean alwaysOpen;
    private LocalDate launchDate;
    private LocalDateTime lastUpdated;
    private SchemeStatus status;
    private Boolean featured;
    private Integer priority;
    private String thumbnailUrl;
    private List<String> galleryImages;
    private List<String> tags;
    private Long viewCount;
    private Long recommendationCount;
    private Long maxBeneficiaries;
    private Long currentBeneficiaries;
    private Integer schemeVersion;
    private LocalDateTime lastUpdatedAt;
    private String lastUpdatedBy;
    private Boolean deleted;
    private Instant createdAt;
    private Instant updatedAt;
}
