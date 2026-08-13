package com.schemebridge.coreservice.scheme.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "SCHEME_VERSIONS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeVersion {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SCHEME_ID", nullable = false)
    private Scheme scheme;

    @Column(name = "VERSION_NUMBER", nullable = false)
    private Integer versionNumber;

    @Column(name = "CHANGE_SUMMARY", length = 2000)
    private String changeSummary;

    @Column(name = "EFFECTIVE_FROM")
    private LocalDate effectiveFrom;

    @Column(name = "EFFECTIVE_TO")
    private LocalDate effectiveTo;

    @Column(name = "CHANGED_BY", length = 100)
    private String changedBy;

    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
    }
}
