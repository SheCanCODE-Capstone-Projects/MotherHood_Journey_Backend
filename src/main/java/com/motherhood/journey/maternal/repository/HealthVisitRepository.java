package com.motherhood.journey.maternal.repository;

import com.motherhood.journey.maternal.entity.HealthVisit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthVisitRepository extends JpaRepository<HealthVisit, UUID> {

    @EntityGraph(attributePaths = {"facility", "healthWorker"})
    Page<HealthVisit> findByFacility_Id(UUID facilityId, Pageable pageable);

    @EntityGraph(attributePaths = {"facility", "healthWorker"})
    Page<HealthVisit> findByPatientRefIdAndPatientTypeAndFacility_Id(
        UUID patientRefId, String patientType, UUID facilityId, Pageable pageable);

    @EntityGraph(attributePaths = {"facility", "healthWorker"})
    Optional<HealthVisit> findByIdAndFacility_Id(UUID id, UUID facilityId);
}
