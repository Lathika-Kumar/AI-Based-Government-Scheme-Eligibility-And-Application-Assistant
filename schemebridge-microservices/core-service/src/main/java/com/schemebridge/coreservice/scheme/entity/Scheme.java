package com.schemebridge.coreservice.scheme.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SCHEMES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scheme {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "SCHEME_CODE", length = 100, unique = true, nullable = false)
    private String schemeCode;

    @Column(name = "TITLE_ENGLISH", length = 1000, nullable = false)
    private String titleEnglish;

    @Column(name = "TITLE_TAMIL", length = 1000)
    private String titleTamil;

    @Column(name = "SLUG", length = 200)
    private String slug;

    @Column(name = "DESCRIPTION_ENGLISH", columnDefinition = "CLOB")
    private String descriptionEnglish;

    @Column(name = "DESCRIPTION_TAMIL", columnDefinition = "CLOB")
    private String descriptionTamil;

    @Column(name = "DETAILED_DESCRIPTION", columnDefinition = "CLOB")
    private String detailedDescription;

    @Column(name = "ELIGIBILITY_TEXT", columnDefinition = "CLOB")
    private String eligibilityText;

    @Column(name = "APPLICATION_PROCESS", columnDefinition = "CLOB")
    private String applicationProcess;

    @Column(name = "DBT_SCHEME")
    private Boolean dbtScheme;

    @Column(name = "TARGET_BENEFICIARIES", length = 1000)
    private String targetBeneficiaries;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CATEGORY_ID")
    private SchemeCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEPARTMENT_ID")
    private SchemeDepartment department;

    @Column(name = "SCHEME_TYPE", length = 50)
    private String schemeType; // CENTRAL, STATE, CENTRALLY_SPONSORED

    @Column(name = "LAUNCH_YEAR")
    private Integer launchYear;

    @Column(name = "SCHEME_URL", length = 500)
    private String schemeUrl;

    @Column(name = "APPLICATION_URL", length = 500)
    private String applicationUrl;

    @Column(name = "HELPLINE_NUMBER", length = 50)
    private String helplineNumber;

    @Column(name = "STATE_SPECIFIC")
    private Boolean stateSpecific;

    @Column(name = "APPLICABLE_STATES", length = 2000)
    private String applicableStates;

    @Column(name = "APPLICABLE_DISTRICTS", length = 2000)
    private String applicableDistricts;

    @Column(name = "VERSION")
    private Integer version;

    @Column(name = "EFFECTIVE_FROM")
    private LocalDate effectiveFrom;

    @Column(name = "EFFECTIVE_TO")
    private LocalDate effectiveTo;

    // Recommendation metadata
    @Column(name = "PRIORITY")
    private Integer priority;

    @Column(name = "POPULARITY_SCORE", precision = 10, scale = 2)
    private BigDecimal popularityScore;

    @Column(name = "FEATURED")
    private Boolean featured;

    @Column(name = "NEWLY_ADDED")
    private Boolean newlyAdded;

    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    // Child relationships
    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SchemeEligibilityRule> eligibilityRules = new ArrayList<>();

    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SchemeBenefit> benefits = new ArrayList<>();

    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SchemeDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SchemeTag> tags = new ArrayList<>();

    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SchemeFaq> faqs = new ArrayList<>();

    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SchemeVersion> versions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
        if (this.version == null) this.version = 1;
        if (this.priority == null) this.priority = 0;
        if (this.popularityScore == null) this.popularityScore = BigDecimal.ZERO;
        if (this.featured == null) this.featured = false;
        if (this.newlyAdded == null) this.newlyAdded = false;
        if (this.stateSpecific == null) this.stateSpecific = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
