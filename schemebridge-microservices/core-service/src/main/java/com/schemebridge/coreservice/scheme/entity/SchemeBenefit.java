package com.schemebridge.coreservice.scheme.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "SCHEME_BENEFITS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeBenefit {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SCHEME_ID", nullable = false)
    private Scheme scheme;

    @Column(name = "BENEFIT_TYPE", length = 100)
    private String benefitType; // FINANCIAL, IN_KIND, SERVICE, SUBSIDY, SCHOLARSHIP

    @Column(name = "TITLE", length = 300)
    private String title;

    @Column(name = "DESCRIPTION_ENGLISH", columnDefinition = "CLOB")
    private String descriptionEnglish;

    @Column(name = "DESCRIPTION_TAMIL", columnDefinition = "CLOB")
    private String descriptionTamil;

    @Column(name = "AMOUNT_MIN", precision = 15, scale = 2)
    private BigDecimal amountMin;

    @Column(name = "AMOUNT_MAX", precision = 15, scale = 2)
    private BigDecimal amountMax;

    @Column(name = "FREQUENCY", length = 50)
    private String frequency; // ONE_TIME, MONTHLY, ANNUALLY

    @Column(name = "CURRENCY", length = 10)
    private String currency;

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
        if (this.currency == null) this.currency = "INR";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
