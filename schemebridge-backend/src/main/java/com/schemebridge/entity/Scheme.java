package com.schemebridge.entity;

import com.schemebridge.enums.SchemeStatus;
import com.schemebridge.enums.SchemeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "schemes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scheme extends BaseEntity {

    @Id
    private String id;

    @Indexed
    private String schemeName;

    @Indexed(unique = true)
    private String schemeCode;

    private String description;
    private String shortDescription;

    @Builder.Default
    private List<String> benefits = new ArrayList<>();

    @Indexed
    private String category;
    private String subcategory;

    @Indexed
    private SchemeType schemeType;

    @Indexed
    @Builder.Default
    private List<String> applicableStates = new ArrayList<>();

    private Integer minimumAge;
    private Integer maximumAge;

    private BigDecimal minimumIncome;
    private BigDecimal maximumIncome;

    @Builder.Default
    private List<String> allowedGenders = new ArrayList<>();

    @Builder.Default
    private List<String> allowedCategories = new ArrayList<>();

    @Builder.Default
    private List<String> allowedOccupations = new ArrayList<>();

    @Builder.Default
    private List<String> requiredDocuments = new ArrayList<>();

    private String websiteUrl;
    private String applicationMode;

    private LocalDate applicationStartDate;
    private LocalDate applicationEndDate;

    @Builder.Default
    private Boolean alwaysOpen = true;

    private LocalDate launchDate;
    private LocalDateTime lastUpdated;

    @Indexed
    @Builder.Default
    private SchemeStatus status = SchemeStatus.DRAFT;

    @Indexed
    @Builder.Default
    private Boolean featured = false;

    @Builder.Default
    private Integer priority = 1;

    private String thumbnailUrl;

    @Builder.Default
    private List<String> galleryImages = new ArrayList<>();

    @Indexed
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private Long viewCount = 0L;

    @Builder.Default
    private Long recommendationCount = 0L;

    private Long maxBeneficiaries;

    @Builder.Default
    private Long currentBeneficiaries = 0L;

    @Builder.Default
    private Integer schemeVersion = 1;

    private LocalDateTime lastUpdatedAt;
    private String lastUpdatedBy;

    @Builder.Default
    private Boolean deleted = false;

    private LocalDateTime deletedAt;
    private String deletedBy;
}
