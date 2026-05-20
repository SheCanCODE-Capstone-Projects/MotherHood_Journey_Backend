package com.motherhood.geo.domain.repository;

import com.motherhood.geo.domain.model.GeoLocation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeoLocationRepository {

    // Get all active locations — used to extract distinct provinces
    List<GeoLocation> findAllActive();

    // Cascading dropdown queries — only active locations
    List<GeoLocation> findByProvince(String province);

    List<GeoLocation> findByProvinceAndDistrict(
            String province, String district);

    List<GeoLocation> findByProvinceAndDistrictAndSector(
            String province, String district, String sector);

    List<GeoLocation> findByProvinceAndDistrictAndSectorAndCell(
            String province, String district, String sector, String cell);

    // Full path resolution — used for NIDA verification
    Optional<GeoLocation> findByFullPath(
            String province, String district,
            String sector, String cell, String village);

    // Find by ID — used for summary
    Optional<GeoLocation> findById(UUID id);
}