package com.motherhood.journey.facility.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.facility.dto.request.CreateFacilityRequest;
import com.motherhood.journey.facility.dto.request.UpdateFacilityRequest;
import com.motherhood.journey.facility.dto.response.FacilityResponse;
import com.motherhood.journey.facility.entity.FacilityType;
import com.motherhood.journey.facility.service.FacilityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/facilities")
@Validated
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MOH_ADMIN')")
    public ResponseEntity<ApiResponse<FacilityResponse>> createFacility(
        @Valid @RequestBody CreateFacilityRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(facilityService.createFacility(request), "Facility created successfully"));
    }

    /**
     * Public facility listing — used by frontend facility picker.
     * Returns only active facilities, paginated. No PHI exposed.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<FacilityResponse>>> getFacilities(
        @RequestParam(required = false) @Size(max = 64) String district,
        @RequestParam(required = false) FacilityType facilityType,
        @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(facilityService.getFacilities(district, facilityType, pageable),
                "Facilities retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FacilityResponse>> getFacilityById(@PathVariable UUID id) {
        return ResponseEntity.ok(
            ApiResponse.success(facilityService.getFacilityById(id), "Facility retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<ApiResponse<FacilityResponse>> updateFacility(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateFacilityRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(facilityService.updateFacility(id, request), "Facility updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MOH_ADMIN')")
    public ResponseEntity<Void> deleteFacility(@PathVariable UUID id) {
        facilityService.deleteFacility(id);
        return ResponseEntity.noContent().build();
    }
}
