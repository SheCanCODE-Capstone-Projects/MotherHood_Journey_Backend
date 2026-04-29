package com.motherhood.facility.presentation;

import com.motherhood.facility.application.dto.FacilityRequest;
import com.motherhood.facility.application.dto.FacilityResponse;
import com.motherhood.facility.application.service.FacilityService;
import com.motherhood.facility.domain.model.Facility;
import com.motherhood.shared.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FacilityResponse> create(@RequestBody FacilityRequest request) {
        return ApiResponse.ok(facilityService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<FacilityResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(facilityService.findById(id));
    }

    @GetMapping
    public ApiResponse<List<FacilityResponse>> findAll(
        @RequestParam(required = false) String district,
        @RequestParam(required = false) Facility.Type type
    ) {
        return ApiResponse.ok(facilityService.findByFilters(null, district, type));
    }
}
