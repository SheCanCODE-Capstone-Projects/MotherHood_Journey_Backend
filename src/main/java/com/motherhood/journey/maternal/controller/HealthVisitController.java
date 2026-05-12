package com.motherhood.journey.maternal.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.maternal.dto.request.CreateHealthVisitRequest;
import com.motherhood.journey.maternal.dto.response.HealthVisitResponse;
import com.motherhood.journey.maternal.service.HealthVisitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visits")
public class HealthVisitController {

    private final HealthVisitService healthVisitService;

    public HealthVisitController(HealthVisitService healthVisitService) {
        this.healthVisitService = healthVisitService;
    }

    /**
     * Record a new health visit
     * POST /api/v1/visits
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HealthVisitResponse> recordVisit(@Valid @RequestBody CreateHealthVisitRequest request) {
        HealthVisitResponse response = healthVisitService.recordVisit(request);
        return ApiResponse.success(response, "Health visit recorded successfully");
    }

    /**
     * Get a health visit by ID
     * GET /api/v1/visits/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<HealthVisitResponse> getVisitById(@PathVariable UUID id) {
        HealthVisitResponse response = healthVisitService.getVisitById(id);
        return ApiResponse.success(response, "Health visit retrieved successfully");
    }
}