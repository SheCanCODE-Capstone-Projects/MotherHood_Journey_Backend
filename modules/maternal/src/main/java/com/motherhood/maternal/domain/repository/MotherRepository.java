package com.motherhood.maternal.domain.repository;

import com.motherhood.maternal.domain.entity.Mother;
import com.motherhood.maternal.domain.enums.NidaVerifiedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MotherRepository extends JpaRepository<Mother, UUID> {

    Optional<Mother> findByHealthId(String healthId);

    Optional<Mother> findByUserIdAndFacilityId(UUID userId, UUID facilityId);

    List<Mother> findByNidaVerifiedStatus(NidaVerifiedStatus status);

    boolean existsByUserId(UUID userId);
}
