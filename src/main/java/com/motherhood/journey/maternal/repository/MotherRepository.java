package com.motherhood.journey.maternal.repository;

import com.motherhood.journey.maternal.entity.Mother;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MotherRepository extends JpaRepository<Mother, UUID> {

    // required by task
    Optional<Mother> findByHealthId(String healthId);

    Optional<Mother> findByUserIdAndFacilityId(UUID userId, UUID facilityId);

    List<Mother> findByNidaVerifiedStatus(String nidaVerifiedStatus);

    // existing helpers
    Optional<Mother> findByUserId(UUID userId);

    boolean existsByHealthId(String healthId);

    // duplicate NID check via linked user
    boolean existsByUser_NationalId(String nationalId);

    @Query("SELECT m FROM Mother m WHERE m.geoLocation.sector = :sector")
    List<Mother> findBySector(@Param("sector") String sector);
}
