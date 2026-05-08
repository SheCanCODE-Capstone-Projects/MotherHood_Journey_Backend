package com.motherhood.journey.maternal.repository;


import com.motherhood.journey.maternal.entity.Pregnancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PregnancyRepository extends JpaRepository<Pregnancy, UUID> {

    List<Pregnancy> findByMotherIdOrderByCreatedAtDesc(UUID motherId);

    boolean existsByMotherIdAndStatus(UUID motherId, String status);

    Optional<Pregnancy> findByMotherIdAndStatus(UUID motherId, String status);

    List<Pregnancy> findByAssignedChwId(UUID chwId);
}
