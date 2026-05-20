package com.motherhood.geo.application.service;

import com.motherhood.geo.application.dto.GeoLocationResponse;
import com.motherhood.geo.application.dto.GeoLocationSummary;
import com.motherhood.geo.domain.repository.GeoLocationRepository;
import com.motherhood.geo.domain.model.GeoLocation;
import com.motherhood.geo.infrastructure.mapper.GeoLocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeoLocationService {

    private final GeoLocationRepository repository;
    private final GeoLocationMapper mapper;

    // Returns all distinct province names for the first dropdown
    public List<String> getProvinces() {
        return repository.findAllActive()
                .stream()
                .map(GeoLocation::getProvince)
                .distinct()
                .toList();
    }

    // Returns all distinct districts inside a given province
    public List<String> getDistricts(String province) {
        return repository.findByProvince(province)
                .stream()
                .map(GeoLocation::getDistrict)
                .distinct()
                .toList();
    }

    // Returns all distinct sectors inside a given province and district
    public List<String> getSectors(String province, String district) {
        return repository.findByProvinceAndDistrict(province, district)
                .stream()
                .map(GeoLocation::getSector)
                .distinct()
                .toList();
    }

    // Returns all distinct cells inside a given sector
    public List<String> getCells(
            String province, String district, String sector) {
        return repository
                .findByProvinceAndDistrictAndSector(province, district, sector)
                .stream()
                .map(GeoLocation::getCell)
                .distinct()
                .toList();
    }

    // Returns all distinct villages inside a given cell
    public List<String> getVillages(
            String province, String district,
            String sector, String cell) {
        return repository
                .findByProvinceAndDistrictAndSectorAndCell(
                        province, district, sector, cell)
                .stream()
                .map(GeoLocation::getVillage)
                .distinct()
                .toList();
    }

    // Returns full location details including UUID
    // Used when saving geo_location_id to mother or user record
    public GeoLocationResponse getFullLocation(
            String province, String district,
            String sector, String cell, String village) {

        GeoLocation geo = repository
                .findByFullPath(province, district, sector, cell, village)
                .orElseThrow(() -> new RuntimeException(
                        "Location not found for the given path"));

        return mapper.toResponse(geo);
    }

    // Returns lightweight summary by UUID
    // Used when embedding location inside another response
    public GeoLocationSummary getSummary(UUID id) {
        GeoLocation geo = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Location not found with id: " + id));

        return mapper.toSummary(geo);
    }
}