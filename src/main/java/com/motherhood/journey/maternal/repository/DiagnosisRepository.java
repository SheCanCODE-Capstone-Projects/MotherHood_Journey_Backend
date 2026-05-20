package com.motherhood.journey.maternal.repository;

import com.motherhood.journey.maternal.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {
    List<Diagnosis> findByVisit_Id(UUID visitId);
}
