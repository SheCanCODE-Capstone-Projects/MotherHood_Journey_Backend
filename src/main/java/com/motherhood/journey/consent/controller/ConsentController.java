package com.motherhood.journey.consent.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.consent.dto.request.CreateConsentRequest;
import com.motherhood.journey.consent.dto.response.ConsentResponse;
import com.motherhood.journey.consent.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consents")
@Validated
@io.swagger.v3.oas.annotations.tags.Tag(name = "Consent",
    description = "Data-sharing consent under Rwanda Law No. 058/2021 (grant + revoke)")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    @Operation(summary = "Record a new data-sharing consent",
        description = "Restricted to HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN.")
    public ResponseEntity<ApiResponse<ConsentResponse>> createConsent(
        @Valid @RequestBody CreateConsentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(consentService.createConsent(request), "Consent recorded"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    @Operation(summary = "Get a consent by ID",
        description = "Restricted to HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN.")
    public ResponseEntity<ApiResponse<ConsentResponse>> getConsentById(
        @PathVariable UUID id,
        @RequestParam UUID facilityId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(consentService.getConsentById(id, facilityId), "Consent retrieved"));
    }

    @GetMapping("/by-mother/{motherId}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    @Operation(summary = "List consents by mother",
        description = "Restricted to HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN.")
    public ResponseEntity<ApiResponse<List<ConsentResponse>>> getConsentsByMother(
        @PathVariable UUID motherId,
        @RequestParam UUID facilityId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(consentService.getConsentsByMother(motherId, facilityId), "Consents retrieved"));
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    @Operation(summary = "Revoke a consent",
        description = "Restricted to HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN.")
    public ResponseEntity<Void> revokeConsent(
        @PathVariable UUID id,
        @RequestParam UUID facilityId
    ) {
        consentService.revokeConsent(id, facilityId);
        return ResponseEntity.noContent().build();
    }
}
