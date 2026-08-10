package com.schemebridge.adminservice.entity;

import com.schemebridge.adminservice.enums.ReportFormat;
import com.schemebridge.adminservice.enums.ReportType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "REPORT_HISTORY", indexes = {
    @Index(name = "IDX_REP_ID", columnList = "REPORT_ID", unique = true),
    @Index(name = "IDX_REP_TYPE", columnList = "REPORT_TYPE")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "REPORT_ID", unique = true, nullable = false, length = 50)
    private String reportId;

    @Column(name = "REPORT_NAME", nullable = false, length = 150)
    private String reportName;

    @Enumerated(EnumType.STRING)
    @Column(name = "REPORT_TYPE", nullable = false, length = 40)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "REPORT_FORMAT", nullable = false, length = 15)
    private ReportFormat reportFormat;

    @Column(name = "GENERATED_BY", nullable = false, length = 100)
    private String generatedBy;

    @Column(name = "FILE_PATH", length = 500)
    private String filePath;

    @Column(name = "FILE_SIZE_BYTES")
    private Long fileSizeBytes;

    @Column(name = "GENERATED_AT", nullable = false)
    private Instant generatedAt;
}
