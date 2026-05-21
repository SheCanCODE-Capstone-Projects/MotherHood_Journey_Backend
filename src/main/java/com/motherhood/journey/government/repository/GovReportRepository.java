package com.motherhood.journey.government.repository;

import com.motherhood.journey.government.entity.GovReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GovReportRepository extends JpaRepository<GovReport, UUID> {
    Page<GovReport> findByGeneratedBy_Id(UUID userId, Pageable pageable);
    Page<GovReport> findByReportTypeAndScopeLevel(String reportType, String scopeLevel, Pageable pageable);
}
