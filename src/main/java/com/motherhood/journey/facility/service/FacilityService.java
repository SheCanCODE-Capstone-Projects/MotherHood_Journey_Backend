package com.motherhood.journey.facility.service;

import com.motherhood.journey.facility.dto.request.CreateFacilityRequest;
import com.motherhood.journey.facility.dto.request.UpdateFacilityRequest;
import com.motherhood.journey.facility.dto.response.FacilityResponse;
import com.motherhood.journey.facility.entity.FacilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FacilityService {
    FacilityResponse createFacility(CreateFacilityRequest request);
    FacilityResponse getFacilityById(UUID id);
    Page<FacilityResponse> getFacilities(String district, FacilityType facilityType, Pageable pageable);
    FacilityResponse updateFacility(UUID id, UpdateFacilityRequest request);
    void deleteFacility(UUID id);
}
