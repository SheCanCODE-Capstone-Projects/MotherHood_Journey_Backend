package com.motherhood.maternal.domain.repository;

import com.motherhood.maternal.domain.entity.Mother;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MotherRepository extends JpaRepository<Mother, UUID> {


    Optional<Mother> findByUserId(UUID userId);

    Optional<Mother> findByHealthId(String healthId);

    List<Mother> findByFacilityId(UUID facilityId);

    boolean existsByIdAndFacilityId(UUID motherId, UUID facilityId);

    @Query("SELECT m FROM Mother m WHERE m.geoLocationId IN :geoLocationIds")
    List<Mother> findByGeoLocationIdIn(
            @Param("geoLocationIds") List<UUID> geoLocationIds);

    @Query("SELECT COUNT(m) > 0 FROM Mother m " +
            "WHERE m.id = :motherId " +
            "AND m.geoLocationId IN :geoLocationIds")
    boolean existsByIdAndGeoLocationIdIn(
            @Param("motherId") UUID motherId,
            @Param("geoLocationIds") List<UUID> geoLocationIds);
}
