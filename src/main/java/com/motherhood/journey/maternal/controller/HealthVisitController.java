package com.motherhood.journey.maternal.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.maternal.dto.request.CreateHealthVisitRequest;
import com.motherhood.journey.maternal.dto.request.UpdateHealthVisitRequest;
import com.motherhood.journey.maternal.dto.response.HealthVisitResponse;
import com.motherhood.journey.maternal.enums.PatientType;
import com.motherhood.journey.maternal.service.HealthVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health-visits")
@Validated
@io.swagger.v3.oas.annotations.tags.Tag(name = "Health Visits",
    description = "ANC, PNC, IMMUNIZATION, SICK_CHILD, GROWTH_MONITORING visits with vitals, diagnoses, prescriptions")
public class HealthVisitController {

    private final HealthVisitService healthVisitService;

    public HealthVisitController(HealthVisitService healthVisitService) {
        this.healthVisitService = healthVisitService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    @Operation(summary = "Record a new health visit",
        description = "Restricted to HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN.")
    public ResponseEntity<ApiResponse<HealthVisitResponse>> createVisit(
        @Valid @RequestBody CreateHealthVisitRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(healthVisitService.createVisit(request), "Visit recorded successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    @Operation(summary = "Get a health visit by ID",
        description = "Restricted to HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER.")
    public ResponseEntity<ApiResponse<HealthVisitResponse>> getVisitById(
        @PathVariable UUID id,
        @RequestParam UUID facilityId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(healthVisitService.getVisitById(id, facilityId), "Visit retrieved"));
    }

    @GetMapping("/by-facility/{facilityId}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    @Operation(summary = "List health visits by facility",
        description = "Restricted to HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER.")
    public ResponseEntity<ApiResponse<Page<HealthVisitResponse>>> getVisitsByFacility(
        @PathVariable UUID facilityId,
        @PageableDefault(size = 20, sort = "visitDatetime") Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(healthVisitService.getVisitsByFacility(facilityId, pageable), "Visits retrieved"));
    }

    @GetMapping("/by-patient")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    @Operation(summary = "List health visits by patient",
        description = "Restricted to HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER.")
    public ResponseEntity<ApiResponse<Page<HealthVisitResponse>>> getVisitsByPatient(
        @RequestParam UUID patientRefId,
        @RequestParam PatientType patientType,
        @RequestParam UUID facilityId,
        @PageableDefault(size = 20, sort = "visitDatetime") Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            healthVisitService.getVisitsByPatient(patientRefId, patientType, facilityId, pageable),
            "Visits retrieved"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    @Operation(summary = "Update a health visit",
        description = "Restricted to HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN.")
    public ResponseEntity<ApiResponse<HealthVisitResponse>> updateVisit(
        @PathVariable UUID id,
        @RequestParam UUID facilityId,
        @Valid @RequestBody UpdateHealthVisitRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(healthVisitService.updateVisit(id, facilityId, request), "Visit updated"));
    }
}
