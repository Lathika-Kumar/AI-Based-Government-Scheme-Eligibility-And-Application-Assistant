package com.schemebridge.coreservice.scheme.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SCHEME_CATEGORIES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeCategory {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "NAME", length = 150, nullable = false)
    private String name;

    @Column(name = "NAME_ENGLISH", length = 150)
    private String nameEnglish;

    @Column(name = "NAME_TAMIL", length = 150)
    private String nameTamil;

    @Column(name = "CODE", length = 50, unique = true, nullable = false)
    private String code;

    @Column(name = "DESCRIPTION", columnDefinition = "CLOB")
    private String description;

    @Column(name = "ICON_URL", length = 500)
    private String iconUrl;

    @Column(name = "DISPLAY_ORDER")
    private Integer displayOrder;

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

    @JsonIgnore
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Scheme> schemes = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
