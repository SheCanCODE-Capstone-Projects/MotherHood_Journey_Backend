package com.motherhood.journey.maternal.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.maternal.dto.request.CreateMotherRequest;
import com.motherhood.journey.maternal.dto.request.UpdateMotherRequest;
import com.motherhood.journey.maternal.dto.response.MotherResponse;
import com.motherhood.journey.maternal.service.MotherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mothers")
public class MotherController {

    private final MotherService motherService;

    public MotherController(MotherService motherService) {
        this.motherService = motherService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<ApiResponse<MotherResponse>> createMother(
        @Valid @RequestBody CreateMotherRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(motherService.createMother(request), "Mother registered successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<MotherResponse>> getMotherById(
        @PathVariable UUID id,
        @RequestParam UUID facilityId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(motherService.getMotherById(id, facilityId), "Mother retrieved"));
    }

    @GetMapping("/by-facility/{facilityId}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<List<MotherResponse>>> getMothersByFacility(
        @PathVariable UUID facilityId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(motherService.getMothersByFacility(facilityId), "Mothers retrieved"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<ApiResponse<MotherResponse>> updateMother(
        @PathVariable UUID id,
        @RequestParam UUID facilityId,
        @Valid @RequestBody UpdateMotherRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(motherService.updateMother(id, facilityId, request), "Mother updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<Void> deactivateMother(
        @PathVariable UUID id,
        @RequestParam UUID facilityId
    ) {
        motherService.deactivateMother(id, facilityId);
        return ResponseEntity.noContent().build();
    }
}
