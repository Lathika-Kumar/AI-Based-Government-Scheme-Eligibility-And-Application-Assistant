package com.schemebridge.authservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PERMISSIONS")
public class PermissionEntity {

    @Id
    @Column(name = "PERMISSION_ID", nullable = false)
    private Long permissionId;

    @Column(name = "PERMISSION_NAME", length = 100, nullable = false, unique = true)
    private String permissionName;

    @Column(name = "CATEGORY", length = 50, nullable = false)
    private String category;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @Column(name = "STATUS", length = 30, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "CREATED_BY", length = 100, nullable = false)
    private String createdBy = "SYSTEM";

    @Column(name = "UPDATED_BY", length = 100, nullable = false)
    private String updatedBy = "SYSTEM";

    public PermissionEntity() {}

    public PermissionEntity(Long permissionId, String permissionName, String category, String description) {
        this.permissionId = permissionId;
        this.permissionName = permissionName;
        this.category = category;
        this.description = description;
    }

    public Long getPermissionId() { return permissionId; }
    public void setPermissionId(Long permissionId) { this.permissionId = permissionId; }

    public String getPermissionName() { return permissionName; }
    public void setPermissionName(String permissionName) { this.permissionName = permissionName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
