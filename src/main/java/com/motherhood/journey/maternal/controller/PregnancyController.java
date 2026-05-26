package com.motherhood.journey.maternal.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.maternal.dto.request.CreatePregnancyRequest;
import com.motherhood.journey.maternal.dto.request.UpdatePregnancyRequest;
import com.motherhood.journey.maternal.dto.response.PregnancyResponse;
import com.motherhood.journey.maternal.service.PregnancyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pregnancies")
@Tag(name = "Pregnancies", description = "Pregnancy lifecycle (ACTIVE / DELIVERED / LOST / TRANSFERRED), EDD, CHW assignment")
public class PregnancyController {

    private final PregnancyService pregnancyService;

    public PregnancyController(PregnancyService pregnancyService) {
        this.pregnancyService = pregnancyService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    @Operation(summary = "Open a new pregnancy",
        description = "Rejects if the mother already has an ACTIVE pregnancy. Computes EDD from LMP if not supplied.")
    public ResponseEntity<ApiResponse<PregnancyResponse>> createPregnancy(
        @Valid @RequestBody CreatePregnancyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(pregnancyService.createPregnancy(request), "Pregnancy recorded successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    @Operation(summary = "Get a pregnancy by UUID (facility-scoped)")
    public ResponseEntity<ApiResponse<PregnancyResponse>> getPregnancyById(
        @PathVariable UUID id,
        @RequestParam UUID facilityId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(pregnancyService.getPregnancyById(id, facilityId), "Pregnancy retrieved"));
    }

    @GetMapping("/by-mother/{motherId}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    @Operation(summary = "List all pregnancies for a mother")
    public ResponseEntity<ApiResponse<List<PregnancyResponse>>> getPregnanciesByMother(
        @PathVariable UUID motherId,
        @RequestParam UUID facilityId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            pregnancyService.getPregnanciesByMother(motherId, facilityId), "Pregnancies retrieved"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    @Operation(summary = "Update a pregnancy (status transitions enforced)",
        description = "Allowed transitions from ACTIVE: DELIVERED, LOST, TRANSFERRED. Terminal states cannot be reopened.")
    public ResponseEntity<ApiResponse<PregnancyResponse>> updatePregnancy(
        @PathVariable UUID id,
        @RequestParam UUID facilityId,
        @Valid @RequestBody UpdatePregnancyRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(pregnancyService.updatePregnancy(id, facilityId, request), "Pregnancy updated"));
    }
}
