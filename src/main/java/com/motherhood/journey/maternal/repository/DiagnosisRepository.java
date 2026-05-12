package com.motherhood.journey.maternal.repository;

import com.motherhood.journey.maternal.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {
    List<Diagnosis> findByVisitId(UUID visitId);
}
