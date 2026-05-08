package com.motherhood.journey.geo.service;

import com.motherhood.journey.geo.dto.response.GeoResponse;
import com.motherhood.journey.geo.dto.response.GeoSummaryResponseDTO;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeoServiceImpl implements GeoService {

    private final GeoRepository repository;

    // Returns all distinct province names
    // Used for the first dropdown on the registration form
    @Override
    public List<String> getProvinces() {
        return repository.findByActiveTrue()
                .stream()
                .map(GeoLocation::getProvince)
                .distinct()
                .toList();
    }

    // Returns all distinct districts in a province
    @Override
    public List<String> getDistricts(String province) {
        return repository
                .findByProvinceAndActiveTrue(province)
                .stream()
                .map(GeoLocation::getDistrict)
                .distinct()
                .toList();
    }

    // Returns all distinct sectors in a district
    @Override
    public List<String> getSectors(String province, String district) {
        return repository
                .findByProvinceAndDistrictAndActiveTrue(province, district)
                .stream()
                .map(GeoLocation::getSector)
                .distinct()
                .toList();
    }

    // Returns all distinct cells in a sector
    @Override
    public List<String> getCells(
            String province, String district, String sector) {
        return repository
                .findByProvinceAndDistrictAndSectorAndActiveTrue(
                        province, district, sector)
                .stream()
                .map(GeoLocation::getCell)
                .distinct()
                .toList();
    }

    // Returns all distinct villages in a cell
    @Override
    public List<String> getVillages(
            String province, String district,
            String sector, String cell) {
        return repository
                .findByProvinceAndDistrictAndSectorAndCellAndActiveTrue(
                        province, district, sector, cell)
                .stream()
                .map(GeoLocation::getVillage)
                .distinct()
                .toList();
    }

    // Returns full location details including UUID
    // Used when saving geo_location_id to mother or user record
    @Override
    public GeoResponse getFullLocation(
            String province, String district,
            String sector, String cell, String village) {

        GeoLocation geo = repository
                .findByProvinceAndDistrictAndSectorAndCellAndVillage(
                        province, district, sector, cell, village)
                .orElseThrow(() -> new RuntimeException(
                        "Location not found for the given path"));

        return convertToResponse(geo);
    }

    // Returns lightweight summary by UUID
    // Used when embedding location inside another response
    @Override
    public GeoSummaryResponseDTO getSummary(UUID id) {
        GeoLocation geo = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Location not found with id: " + id));

        return convertToSummary(geo);
    }

    // Converts GeoLocation entity to full GeoResponse DTO
    // No mapper needed — done manually here
    private GeoResponse convertToResponse(GeoLocation geo) {
        return new GeoResponse(
                geo.getId(),
                geo.getProvince(),
                geo.getDistrict(),
                geo.getSector(),
                geo.getCell(),
                geo.getVillage(),
                geo.getLatitude(),
                geo.getLongitude()
        );
    }

    // Converts GeoLocation entity to lightweight GeoSummaryResponse DTO
    private GeoSummaryResponseDTO convertToSummary(GeoLocation geo) {
        return new GeoSummaryResponseDTO(
                geo.getId(),
                geo.getSector(),
                geo.getCell(),
                geo.getVillage()
        );
    }
}