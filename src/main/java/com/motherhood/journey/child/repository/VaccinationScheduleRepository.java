package com.motherhood.journey.child.repository;

import com.motherhood.journey.child.entity.VaccinationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VaccinationScheduleRepository extends JpaRepository<VaccinationSchedule, UUID> {

    List<VaccinationSchedule> findByIsMandatoryTrue();
}