package com.motherhood.journey.maternal.repository;

import com.motherhood.journey.maternal.entity.HealthVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface HealthVisitRepository extends JpaRepository<HealthVisit, UUID> {

    List<HealthVisit> findByPatientRefId(UUID patientRefId);

    List<HealthVisit> findByFacilityId(Long facilityId);

    List<HealthVisit> findByHealthWorkerId(UUID healthWorkerId);

    List<HealthVisit> findByFacilityIdAndVisitDatetimeBetween(Long facilityId, LocalDateTime start, LocalDateTime end);

    List<HealthVisit> findByPatientRefIdAndPatientType(UUID patientRefId, String patientType);
}
