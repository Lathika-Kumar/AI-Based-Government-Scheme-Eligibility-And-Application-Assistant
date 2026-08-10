package com.schemebridge.adminservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "FEATURE_FLAGS", indexes = {
    @Index(name = "IDX_FLAG_NAME", columnList = "FLAG_NAME", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureFlag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "FLAG_NAME", unique = true, nullable = false, length = 100)
    private String flagName;

    @Column(name = "ENABLED", nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;
}
