package com.schemebridge.schemeservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "SCHEME_ELIGIBILITY_RULES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeEligibilityRule {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SCHEME_ID", nullable = false)
    private Scheme scheme;

    @Column(name = "RULE_TYPE", length = 50, nullable = false)
    private String ruleType; // AGE, GENDER, INCOME, CATEGORY, RELIGION, OCCUPATION, STATE, etc.

    @Column(name = "OPERATOR", length = 20)
    private String operator; // EQ, LTE, GTE, IN, BETWEEN, NOT_EQ

    @Column(name = "VALUE_STRING", length = 1000)
    private String valueString;

    @Column(name = "VALUE_NUMBER_MIN", precision = 15, scale = 2)
    private BigDecimal valueNumberMin;

    @Column(name = "VALUE_NUMBER_MAX", precision = 15, scale = 2)
    private BigDecimal valueNumberMax;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "MANDATORY")
    private Boolean mandatory;

    @Column(name = "DISPLAY_ORDER")
    private Integer displayOrder;

    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
        if (this.mandatory == null) this.mandatory = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
