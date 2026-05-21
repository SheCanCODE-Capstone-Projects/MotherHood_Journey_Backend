package com.motherhood.journey.geo.repository;

import com.motherhood.journey.geo.entity.GeoLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeoRepository extends JpaRepository<GeoLocation, UUID> {

    // Get all active locations
    // Used to extract distinct province names
    List<GeoLocation> findByActiveTrue();

    // Get all active locations in a province
    // Used for district dropdown
    List<GeoLocation> findByProvinceAndActiveTrue(String province);

    // Get all active locations in a province and district
    // Used for sector dropdown
    List<GeoLocation> findByProvinceAndDistrictAndActiveTrue(
            String province, String district);

    // Get all active locations in a province, district and sector
    // Used for cell dropdown
    List<GeoLocation> findByProvinceAndDistrictAndSectorAndActiveTrue(
            String province, String district, String sector);

    // Get all active locations in a province, district, sector and cell
    // Used for village dropdown
    List<GeoLocation> findByProvinceAndDistrictAndSectorAndCellAndActiveTrue(
            String province, String district,
            String sector, String cell);

    // Find exact location by full path
    // Used for NIDA resolution and saving geo_location_id
    Optional<GeoLocation> findByProvinceAndDistrictAndSectorAndCellAndVillage(
            String province, String district,
            String sector, String cell, String village);
}