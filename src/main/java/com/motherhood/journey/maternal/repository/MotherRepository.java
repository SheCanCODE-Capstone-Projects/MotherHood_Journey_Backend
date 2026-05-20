package com.motherhood.journey.maternal.repository;

import com.motherhood.journey.maternal.entity.Mother;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MotherRepository extends JpaRepository<Mother, UUID> {
    Optional<Mother> findByUserId(UUID userId);
    boolean existsByHealthId(String healthId);
    List<Mother> findByFacility_Id(UUID facilityId);
    Optional<Mother> findByIdAndFacility_Id(UUID id, UUID facilityId);

    @Query("SELECT m FROM Mother m WHERE m.geoLocation.sector = :sector AND m.facility.id = :facilityId")
    List<Mother> findBySectorAndFacility(@Param("sector") String sector, @Param("facilityId") UUID facilityId);
}
