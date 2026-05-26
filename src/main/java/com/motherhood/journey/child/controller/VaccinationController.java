package com.motherhood.journey.child.controller;

import com.motherhood.journey.child.dto.request.AdministerVaccinationRequest;
import com.motherhood.journey.child.dto.response.VaccinationRecordResponse;
import com.motherhood.journey.child.service.VaccinationService;
import com.motherhood.journey.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vaccinations")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Vaccinations",
    description = "Mark vaccines administered, age-validated, audit-trailed")
public class VaccinationController {

    private final VaccinationService vaccinationService;

    public VaccinationController(VaccinationService vaccinationService) {
        this.vaccinationService = vaccinationService;
    }

    @GetMapping("/by-child/{childId}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<List<VaccinationRecordResponse>>> getByChild(
        @PathVariable UUID childId,
        @RequestParam UUID facilityId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(vaccinationService.getByChild(childId, facilityId), "Vaccination records retrieved"));
    }

    @PatchMapping("/{id}/administer")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<ApiResponse<VaccinationRecordResponse>> administer(
        @PathVariable UUID id,
        @RequestParam UUID facilityId,
        @Valid @RequestBody AdministerVaccinationRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(vaccinationService.administer(id, facilityId, request), "Vaccination administered"));
    }
}
