package com.motherhood.journey.government.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.government.dto.response.GovernmentResponse;
import com.motherhood.journey.government.service.GovernmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/government")
public class GovernmentController {

    private final GovernmentService governmentService;

    public GovernmentController(GovernmentService governmentService) {
        this.governmentService = governmentService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<GovernmentResponse>> getGovernmentUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(
            ApiResponse.success(governmentService.getGovernmentUserById(id), "Government user retrieved"));
    }

    @GetMapping("/by-user/{userId}")
    @PreAuthorize("hasAnyRole('MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<GovernmentResponse>> getGovernmentUserByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(
            ApiResponse.success(governmentService.getGovernmentUserByUserId(userId), "Government user retrieved"));
    }
}
