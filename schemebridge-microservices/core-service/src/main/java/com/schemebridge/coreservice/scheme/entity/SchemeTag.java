package com.schemebridge.coreservice.scheme.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "SCHEME_TAGS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeTag {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SCHEME_ID", nullable = false)
    private Scheme scheme;

    @Column(name = "TAG", length = 100, nullable = false)
    private String tag;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
