package com.schemebridge.adminservice.repository;

import com.schemebridge.adminservice.entity.ReportHistory;
import com.schemebridge.adminservice.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {

    Optional<ReportHistory> findByReportId(String reportId);

    Page<ReportHistory> findByActiveTrue(Pageable pageable);

    Page<ReportHistory> findByReportTypeAndActiveTrue(ReportType reportType, Pageable pageable);
}
