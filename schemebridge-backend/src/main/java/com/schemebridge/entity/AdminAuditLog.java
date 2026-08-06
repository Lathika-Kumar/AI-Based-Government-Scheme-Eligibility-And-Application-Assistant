package com.schemebridge.entity;

import com.schemebridge.enums.AdminAuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "admin_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAuditLog extends BaseEntity {

    @Id
    private String id;

    private String actorEmail;
    private AdminAuditAction action;
    private String targetType;
    private String targetId;
    private String details;
}
