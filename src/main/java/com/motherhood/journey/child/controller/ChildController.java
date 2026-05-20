package com.motherhood.journey.child.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.child.dto.request.CreateChildRequest;
import com.motherhood.journey.child.dto.request.UpdateChildRequest;
import com.motherhood.journey.child.dto.response.ChildResponse;
import com.motherhood.journey.child.service.ChildService;
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
@RequestMapping("/api/v1/children")
@Validated
public class ChildController {

    private final ChildService childService;

    public ChildController(ChildService childService) {
        this.childService = childService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<ApiResponse<ChildResponse>> registerChild(
        @Valid @RequestBody CreateChildRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(childService.registerChild(request), "Child registered successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<ChildResponse>> getChildById(
        @PathVariable UUID id,
        @RequestParam UUID facilityId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(childService.getChildById(id, facilityId), "Child retrieved"));
    }

    @GetMapping("/by-mother/{motherId}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<Page<ChildResponse>>> getChildrenByMother(
        @PathVariable UUID motherId,
        @RequestParam UUID facilityId,
        @PageableDefault(size = 20, sort = "registeredAt") Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(childService.getChildrenByMother(motherId, facilityId, pageable), "Children retrieved"));
    }

    @GetMapping("/by-facility/{facilityId}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<Page<ChildResponse>>> getChildrenByFacility(
        @PathVariable UUID facilityId,
        @PageableDefault(size = 20, sort = "registeredAt") Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(childService.getChildrenByFacility(facilityId, pageable), "Children retrieved"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('HEALTH_WORKER', 'FACILITY_ADMIN', 'MOH_ADMIN')")
    public ResponseEntity<ApiResponse<ChildResponse>> updateChild(
        @PathVariable UUID id,
        @RequestParam UUID facilityId,
        @Valid @RequestBody UpdateChildRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(childService.updateChild(id, facilityId, request), "Child updated"));
    }
}
