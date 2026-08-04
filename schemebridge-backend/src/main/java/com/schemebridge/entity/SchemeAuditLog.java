package com.schemebridge.entity;

import com.schemebridge.enums.SchemeAuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;

@Document(collection = "scheme_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeAuditLog {

    @Id
    private String id;

    private String schemeId;

    private SchemeAuditAction action;

    private Set<String> updatedFields;

    private String updatedBy;

    private LocalDateTime updatedAt;
}
