package com.motherhood.journey.child.repository;

import com.motherhood.journey.child.entity.VaccinationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, UUID> {

    List<VaccinationRecord> findByChildId(UUID childId);

    List<VaccinationRecord> findByChildIdAndStatus(UUID childId, String status);
}
