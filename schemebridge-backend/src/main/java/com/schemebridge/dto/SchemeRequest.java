package com.schemebridge.dto;

import com.schemebridge.enums.SchemeStatus;
import com.schemebridge.enums.SchemeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeRequest {

    @NotBlank(message = "Scheme name is required")
    private String schemeName;

    @NotBlank(message = "Scheme code is required")
    private String schemeCode;

    @NotBlank(message = "Description is required")
    private String description;

    private String shortDescription;

    @Builder.Default
    private List<String> benefits = new ArrayList<>();

    @NotBlank(message = "Category is required")
    private String category;

    private String subcategory;

    @NotNull(message = "Scheme type (CENTRAL/STATE) is required")
    private SchemeType schemeType;

    @Builder.Default
    private List<String> applicableStates = new ArrayList<>();

    @Min(value = 0, message = "Minimum age cannot be negative")
    private Integer minimumAge;

    @Min(value = 0, message = "Maximum age cannot be negative")
    private Integer maximumAge;

    @DecimalMin(value = "0.0", message = "Minimum income cannot be negative")
    private BigDecimal minimumIncome;

    @DecimalMin(value = "0.0", message = "Maximum income cannot be negative")
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

    private SchemeStatus status;

    @Builder.Default
    private Boolean featured = false;

    @Builder.Default
    private Integer priority = 1;

    private String thumbnailUrl;

    @Builder.Default
    private List<String> galleryImages = new ArrayList<>();

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    private Long maxBeneficiaries;
}
