package com.schemebridge.coreservice.scheme.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "SCHEME_DOCUMENTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeDocument {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SCHEME_ID", nullable = false)
    private Scheme scheme;

    @Column(name = "DOCUMENT_NAME", length = 300, nullable = false)
    private String documentName;

    @Column(name = "DOCUMENT_TYPE", length = 100)
    private String documentType;

    @Column(name = "DESCRIPTION", length = 1000)
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
