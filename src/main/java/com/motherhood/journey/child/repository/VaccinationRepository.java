package com.motherhood.journey.child.repository;
import com.motherhood.journey.child.entity.VaccinationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VaccinationRepository extends JpaRepository<VaccinationRecord, UUID> {

    @Query("""
            SELECT v FROM VaccinationRecord v
            JOIN FETCH v.schedule
            JOIN FETCH v.child c
            JOIN FETCH c.mother m
            JOIN FETCH m.user
            WHERE v.status = 'PENDING'
            """)
    List<VaccinationRecord> findAllPendingWithDetails();

    List<VaccinationRecord> findByChildIdOrderByDueDateAsc(UUID childId);
}