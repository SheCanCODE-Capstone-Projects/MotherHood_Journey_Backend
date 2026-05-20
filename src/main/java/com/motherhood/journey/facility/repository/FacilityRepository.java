package com.motherhood.journey.facility.repository;

import com.motherhood.journey.facility.entity.FacilityType;
import com.motherhood.journey.geo.entity.Facility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, UUID> {

    @EntityGraph(attributePaths = "geoLocation")
    Page<Facility> findByActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = "geoLocation")
    Page<Facility> findByDistrictAndActiveTrue(String district, Pageable pageable);

    @EntityGraph(attributePaths = "geoLocation")
    Page<Facility> findByFacilityTypeAndActiveTrue(FacilityType facilityType, Pageable pageable);

    @EntityGraph(attributePaths = "geoLocation")
    Page<Facility> findByDistrictAndFacilityTypeAndActiveTrue(String district, FacilityType facilityType, Pageable pageable);

    Optional<Facility> findByFacilityCode(String facilityCode);
}
