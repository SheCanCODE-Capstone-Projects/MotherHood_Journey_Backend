package com.motherhood.journey.consent.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.consent.dto.request.CreateConsentRequest;
import com.motherhood.journey.consent.dto.response.ConsentResponse;
import com.motherhood.journey.consent.service.ConsentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/v1/consents")
@Validated
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<ApiResponse<ConsentResponse>> createConsent(
        @Valid @RequestBody CreateConsentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(consentService.createConsent(request), "Consent recorded"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<ApiResponse<ConsentResponse>> getConsentById(
        @PathVariable UUID id,
        @RequestParam UUID facilityId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(consentService.getConsentById(id, facilityId), "Consent retrieved"));
    }

    @GetMapping("/by-mother/{motherId}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<ApiResponse<List<ConsentResponse>>> getConsentsByMother(
        @PathVariable UUID motherId,
        @RequestParam UUID facilityId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(consentService.getConsentsByMother(motherId, facilityId), "Consents retrieved"));
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<Void> revokeConsent(
        @PathVariable UUID id,
        @RequestParam UUID facilityId
    ) {
        consentService.revokeConsent(id, facilityId);
        return ResponseEntity.noContent().build();
    }
}
