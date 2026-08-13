package com.schemebridge.coreservice.scheme.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SCHEME_DEPARTMENTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeDepartment {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "NAME", length = 200, nullable = false)
    private String name;

    @Column(name = "MINISTRY", length = 200)
    private String ministry;

    @Column(name = "CODE", length = 50, unique = true, nullable = false)
    private String code;

    @Column(name = "DESCRIPTION", columnDefinition = "CLOB")
    private String description;

    @Column(name = "WEBSITE_URL", length = 500)
    private String websiteUrl;

    @Column(name = "HELPLINE_NUMBER", length = 50)
    private String helplineNumber;

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
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
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
