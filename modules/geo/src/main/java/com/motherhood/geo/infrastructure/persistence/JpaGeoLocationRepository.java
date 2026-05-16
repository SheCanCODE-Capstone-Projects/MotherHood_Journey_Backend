package com.motherhood.geo.infrastructure.persistence;

import com.motherhood.geo.domain.model.GeoLocation;
import com.motherhood.geo.domain.repository.GeoLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaGeoLocationRepository implements GeoLocationRepository {

    private final SpringDataGeoRepository springDataRepo;

    @Override
    public List<GeoLocation> findAllActive() {
        return springDataRepo.findByActiveTrue();
    }

    @Override
    public List<GeoLocation> findByProvince(String province) {
        return springDataRepo.findByProvinceAndActiveTrue(province);
    }

    @Override
    public List<GeoLocation> findByProvinceAndDistrict(
            String province, String district) {
        return springDataRepo
                .findByProvinceAndDistrictAndActiveTrue(province, district);
    }

    @Override
    public List<GeoLocation> findByProvinceAndDistrictAndSector(
            String province, String district, String sector) {
        return springDataRepo
                .findByProvinceAndDistrictAndSectorAndActiveTrue(
                        province, district, sector);
    }

    @Override
    public List<GeoLocation> findByProvinceAndDistrictAndSectorAndCell(
            String province, String district,
            String sector, String cell) {
        return springDataRepo
                .findByProvinceAndDistrictAndSectorAndCellAndActiveTrue(
                        province, district, sector, cell);
    }

    @Override
    public Optional<GeoLocation> findByFullPath(
            String province, String district,
            String sector, String cell, String village) {
        return springDataRepo
                .findByProvinceAndDistrictAndSectorAndCellAndVillage(
                        province, district, sector, cell, village);
    }

    @Override
    public Optional<GeoLocation> findById(UUID id) {
        return springDataRepo.findById(id);
    }
}