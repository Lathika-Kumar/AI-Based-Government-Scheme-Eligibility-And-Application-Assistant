package com.schemebridge.schemeservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TRENDING_SCHEMES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendingScheme {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "ID", length = 36)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SCHEME_ID", nullable = false, unique = true)
    private Scheme scheme;

    @Column(name = "RANK_POSITION", nullable = false)
    private Integer rankPosition;

    @Column(name = "VIEW_COUNT")
    private Long viewCount;

    @Column(name = "APPLICATION_COUNT")
    private Long applicationCount;

    @Column(name = "TRENDING_SCORE", precision = 10, scale = 2)
    private BigDecimal trendingScore;

    @Column(name = "TRENDING_SINCE")
    private LocalDateTime trendingSince;

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
        this.trendingSince = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
        if (this.viewCount == null) this.viewCount = 0L;
        if (this.applicationCount == null) this.applicationCount = 0L;
        if (this.trendingScore == null) this.trendingScore = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
