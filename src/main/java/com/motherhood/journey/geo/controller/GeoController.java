package com.motherhood.journey.geo.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.geo.dto.request.CreateGeoRequest;
import com.motherhood.journey.geo.dto.response.GeoResponse;
import com.motherhood.journey.geo.service.GeoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/geo-locations")
public class GeoController {

    private final GeoService geoService;

    public GeoController(GeoService geoService) {
        this.geoService = geoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<ApiResponse<GeoResponse>> createGeoLocation(
        @Valid @RequestBody CreateGeoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(geoService.createGeoLocation(request), "GeoLocation created"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GeoResponse>> getGeoLocationById(@PathVariable UUID id) {
        return ResponseEntity.ok(
            ApiResponse.success(geoService.getGeoLocationById(id), "GeoLocation retrieved"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<GeoResponse>>> getAllGeoLocations(
        @PageableDefault(size = 50, sort = "district") Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(geoService.getAllGeoLocations(pageable), "GeoLocations retrieved"));
    }
}
