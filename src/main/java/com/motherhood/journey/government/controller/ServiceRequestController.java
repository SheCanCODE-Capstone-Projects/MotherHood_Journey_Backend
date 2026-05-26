package com.motherhood.journey.government.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.government.dto.request.SubmitServiceRequestRequest;
import com.motherhood.journey.government.dto.response.ServiceRequestResponse;
import com.motherhood.journey.government.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/v1/service-requests")
@Validated
@io.swagger.v3.oas.annotations.tags.Tag(name = "Service Requests",
    description = "Government service request workflow: submit, review, approve, reject. Consent-gated and audited.")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    public ServiceRequestController(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit a new government service request",
        description = "Requires an authenticated user.")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> submit(
        @Valid @RequestBody SubmitServiceRequestRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(serviceRequestService.submit(request), "Service request submitted"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    @Operation(summary = "Get a service request by ID",
        description = "Restricted to FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER.")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(
            ApiResponse.success(serviceRequestService.getById(id), "Service request retrieved"));
    }

    @GetMapping("/by-facility/{facilityId}")
    @PreAuthorize("hasAnyRole('FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    @Operation(summary = "List service requests by facility",
        description = "Restricted to FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER.")
    public ResponseEntity<ApiResponse<Page<ServiceRequestResponse>>> getByFacility(
        @PathVariable UUID facilityId,
        @PageableDefault(size = 20, sort = "submittedAt") Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(serviceRequestService.getByFacility(facilityId, pageable), "Service requests retrieved"));
    }

    @GetMapping("/by-status")
    @PreAuthorize("hasAnyRole('MOH_ADMIN', 'DISTRICT_OFFICER')")
    @Operation(summary = "List service requests filtered by status",
        description = "Restricted to MOH_ADMIN, DISTRICT_OFFICER.")
    public ResponseEntity<ApiResponse<Page<ServiceRequestResponse>>> getByStatus(
        @RequestParam String status,
        @PageableDefault(size = 20, sort = "submittedAt") Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(serviceRequestService.getByStatus(status, pageable), "Service requests retrieved"));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('FACILITY_ADMIN', 'MOH_ADMIN')")
    @Operation(summary = "Approve a service request",
        description = "Restricted to FACILITY_ADMIN, MOH_ADMIN.")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(
            ApiResponse.success(serviceRequestService.approve(id), "Service request approved"));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('FACILITY_ADMIN', 'MOH_ADMIN')")
    @Operation(summary = "Reject a service request",
        description = "Restricted to FACILITY_ADMIN, MOH_ADMIN.")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> reject(
        @PathVariable UUID id,
        @RequestParam @NotBlank String reason
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(serviceRequestService.reject(id, reason), "Service request rejected"));
    }
}
