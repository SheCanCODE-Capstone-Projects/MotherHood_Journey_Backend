package com.motherhood.journey.facility.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.facility.dto.request.CreateFacilityRequest;
import com.motherhood.journey.facility.dto.request.UpdateFacilityRequest;
import com.motherhood.journey.facility.dto.response.FacilityResponse;
import com.motherhood.journey.facility.entity.FacilityType;
import com.motherhood.journey.facility.service.FacilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Facility management
 * Handles all facility-related endpoints
 */
@RestController
@RequestMapping("/api/v1/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    /**
     * Create a new facility
     * POST /api/v1/facilities
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FacilityResponse> createFacility(@Valid @RequestBody CreateFacilityRequest request) {
        return ApiResponse.success(facilityService.createFacility(request), "Facility created successfully");
    }

    /**
     * Get all facilities
     * GET /api/v1/facilities
     *
     * Optional query parameters:
     * - district: filter by district
     * - type: filter by facility type
     */
    @GetMapping
    public ApiResponse<List<FacilityResponse>> getAllFacilities(
        @RequestParam(required = false) String district,
        @RequestParam(required = false) FacilityType type
    ) {
        List<FacilityResponse> facilities;

        if (district != null && type != null) {
            facilities = facilityService.getFacilitiesByDistrictAndType(district, type);
        } else if (district != null) {
            facilities = facilityService.getFacilitiesByDistrict(district);
        } else if (type != null) {
            facilities = facilityService.getFacilitiesByType(type);
        } else {
            facilities = facilityService.getAllFacilities();
        }

        return ApiResponse.success(facilities, "Facilities retrieved successfully");
    }

    /**
     * Get facility by ID
     * GET /api/v1/facilities/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<FacilityResponse> getFacilityById(@PathVariable Long id) {
        return ApiResponse.success(facilityService.getFacilityById(id), "Facility retrieved successfully");
    }

    /**
     * Update facility
     * PUT /api/v1/facilities/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<FacilityResponse> updateFacility(
        @PathVariable Long id,
        @Valid @RequestBody UpdateFacilityRequest request
    ) {
        return ApiResponse.success(facilityService.updateFacility(id, request), "Facility updated successfully");
    }

    /**
     * Delete facility
     * DELETE /api/v1/facilities/{id}
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFacility(@PathVariable Long id) {
        facilityService.deleteFacility(id);
    }
}

