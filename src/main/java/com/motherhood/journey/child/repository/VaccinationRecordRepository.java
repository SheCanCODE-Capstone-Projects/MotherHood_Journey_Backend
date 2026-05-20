package com.motherhood.journey.child.repository;

import com.motherhood.journey.child.entity.VaccinationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, UUID> {
    List<VaccinationRecord> findByChild_Id(UUID childId);
    List<VaccinationRecord> findByChild_IdAndFacility_Id(UUID childId, UUID facilityId);
    Optional<VaccinationRecord> findByIdAndFacility_Id(UUID id, UUID facilityId);
    long countByStatus(String status);
}
