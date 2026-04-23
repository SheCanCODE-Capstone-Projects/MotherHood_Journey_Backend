package com.motherhood.geo.infrastructure.persistence;

import com.motherhood.geo.domain.model.GeoLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataGeoRepository
        extends JpaRepository<GeoLocation, UUID> {

    List<GeoLocation> findByActiveTrue();

    List<GeoLocation> findByProvinceAndActiveTrue(String province);

    List<GeoLocation> findByProvinceAndDistrictAndActiveTrue(
            String province, String district);

    List<GeoLocation> findByProvinceAndDistrictAndSectorAndActiveTrue(
            String province, String district, String sector);

    List<GeoLocation> findByProvinceAndDistrictAndSectorAndCellAndActiveTrue(
            String province, String district, String sector, String cell);

    Optional<GeoLocation> findByProvinceAndDistrictAndSectorAndCellAndVillage(
            String province, String district,
            String sector, String cell, String village);
}