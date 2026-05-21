package com.motherhood.journey.geo.repository;

import com.motherhood.journey.geo.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, UUID> {

    // Find a facility by its official code
    // Used when registering a mother at a specific facility
    Optional<Facility> findByFacilityCode(String facilityCode);

    // Find all active facilities
    // Used for facility dropdown on registration form
    List<Facility> findByActiveTrue();

    // Find all facilities in a district
    // Used by district officers to see facilities in their area
    List<Facility> findByDistrictAndActiveTrue(String district);

    // Find all facilities linked to a specific geo location
    // Used to show all facilities in a village or cell
    List<Facility> findByGeoLocationIdAndActiveTrue(UUID geoLocationId);

    // Check if a facility code already exists
    // Used to prevent duplicate facility codes
    boolean existsByFacilityCode(String facilityCode);
}