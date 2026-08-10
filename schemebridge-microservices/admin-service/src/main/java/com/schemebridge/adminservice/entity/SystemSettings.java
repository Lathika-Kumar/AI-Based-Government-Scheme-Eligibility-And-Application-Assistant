package com.schemebridge.adminservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SYSTEM_SETTINGS", indexes = {
    @Index(name = "IDX_SETTING_KEY", columnList = "SETTING_KEY", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SETTING_KEY", unique = true, nullable = false, length = 100)
    private String settingKey;

    @Column(name = "SETTING_VALUE", length = 1000)
    private String settingValue;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @Column(name = "CATEGORY", length = 50)
    private String category;
}
