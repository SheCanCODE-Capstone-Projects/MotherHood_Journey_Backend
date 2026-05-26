package com.motherhood.journey.government.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.government.dto.request.CreateGovernmentUserRequest;
import com.motherhood.journey.government.dto.request.UpdateGovernmentScopeRequest;
import com.motherhood.journey.government.dto.response.GovernmentResponse;
import com.motherhood.journey.government.service.GovernmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/government")
@Tag(name = "Government Users",
    description = "Onboard MoH staff, assign geo scope, toggle export / HMIS push permissions")
public class GovernmentController {

    private final GovernmentService governmentService;

    public GovernmentController(GovernmentService governmentService) {
        this.governmentService = governmentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MOH_ADMIN')")
    @Operation(summary = "Create a government user")
    public ResponseEntity<ApiResponse<GovernmentResponse>> create(
        @Valid @RequestBody CreateGovernmentUserRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(governmentService.create(request), "Government user created"));
    }

    @GetMapping
    @PreAuthorize("hasRole('MOH_ADMIN')")
    @Operation(summary = "List all government users")
    public ResponseEntity<ApiResponse<List<GovernmentResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(governmentService.list(), "Government users retrieved"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MOH_ADMIN', 'DISTRICT_OFFICER')")
    @Operation(summary = "Get a government user by ID")
    public ResponseEntity<ApiResponse<GovernmentResponse>> getGovernmentUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(
            ApiResponse.success(governmentService.getGovernmentUserById(id), "Government user retrieved"));
    }

    @GetMapping("/by-user/{userId}")
    @PreAuthorize("hasAnyRole('MOH_ADMIN', 'DISTRICT_OFFICER')")
    @Operation(summary = "Get a government user by underlying user ID")
    public ResponseEntity<ApiResponse<GovernmentResponse>> getGovernmentUserByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(
            ApiResponse.success(governmentService.getGovernmentUserByUserId(userId), "Government user retrieved"));
    }

    @PatchMapping("/{id}/scope")
    @PreAuthorize("hasRole('MOH_ADMIN')")
    @Operation(summary = "Update geo scope + permission toggles")
    public ResponseEntity<ApiResponse<GovernmentResponse>> updateScope(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateGovernmentScopeRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(governmentService.updateScope(id, request), "Scope updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MOH_ADMIN')")
    @Operation(summary = "Deactivate a government user")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        governmentService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
