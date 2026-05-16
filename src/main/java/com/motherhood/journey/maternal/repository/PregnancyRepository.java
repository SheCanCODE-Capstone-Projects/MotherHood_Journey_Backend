package com.motherhood.journey.maternal.repository;

import com.motherhood.journey.maternal.entity.Pregnancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PregnancyRepository extends JpaRepository<Pregnancy, UUID> {

    List<Pregnancy> findByMother_Id(UUID motherId);

    List<Pregnancy> findByMother_IdAndMother_Facility_Id(UUID motherId, UUID facilityId);

    Optional<Pregnancy> findByIdAndMother_Facility_Id(UUID id, UUID facilityId);

    Optional<Pregnancy> findFirstByMother_IdAndStatusOrderByCreatedAtDesc(UUID motherId, String status);

    long countByStatus(String status);
}
