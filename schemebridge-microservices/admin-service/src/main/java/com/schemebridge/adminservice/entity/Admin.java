package com.schemebridge.adminservice.entity;

import com.schemebridge.adminservice.enums.OfficerRole;
import com.schemebridge.adminservice.enums.OfficerStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ADMINS", indexes = {
    @Index(name = "IDX_ADMIN_EMAIL", columnList = "EMAIL", unique = true),
    @Index(name = "IDX_ADMIN_STATUS", columnList = "STATUS")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ADMIN_ID", unique = true, nullable = false, length = 50)
    private String adminId;

    @Column(name = "FULL_NAME", nullable = false, length = 100)
    private String fullName;

    @Column(name = "EMAIL", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "PHONE_NUMBER", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 30)
    private OfficerRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private OfficerStatus status = OfficerStatus.ACTIVE;

    @Column(name = "DEPARTMENT", length = 100)
    private String department;
}
