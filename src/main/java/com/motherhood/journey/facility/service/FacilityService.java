package com.motherhood.journey.facility.service;

import com.motherhood.journey.facility.dto.request.CreateFacilityRequest;
import com.motherhood.journey.facility.dto.request.UpdateFacilityRequest;
import com.motherhood.journey.facility.dto.response.FacilityResponse;
import com.motherhood.journey.facility.entity.FacilityType;

import java.util.List;

public interface FacilityService {

    /**
     * Create a new facility
     */
    FacilityResponse createFacility(CreateFacilityRequest request);

    /**
     * Get facility by ID
     */
    FacilityResponse getFacilityById(Long id);

    /**
     * Get all facilities
     */
    List<FacilityResponse> getAllFacilities();

    /**
     * Get facilities by district
     */
    List<FacilityResponse> getFacilitiesByDistrict(String district);

    /**
     * Get facilities by type
     */
    List<FacilityResponse> getFacilitiesByType(FacilityType type);

    /**
     * Get facilities by district and type
     */
    List<FacilityResponse> getFacilitiesByDistrictAndType(String district, FacilityType type);

    /**
     * Update facility
     */
    FacilityResponse updateFacility(Long id, UpdateFacilityRequest request);

    /**
     * Delete facility
     */
    void deleteFacility(Long id);
}

