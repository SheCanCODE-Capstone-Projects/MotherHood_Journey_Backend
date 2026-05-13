package com.motherhood.journey.maternal.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.maternal.dto.request.CreateMotherRequest;
import com.motherhood.journey.maternal.dto.response.MotherDTO;
import com.motherhood.journey.maternal.dto.response.MotherResponse;
import com.motherhood.journey.maternal.service.MotherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mothers")
@RequiredArgsConstructor
public class MotherController {

    private final MotherService motherService;

    /**
     * POST /api/v1/mothers
     * Registers a new mother, returns immediately with generated health_id.
     * NIDA verification runs asynchronously in the background.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MotherResponse>> register(
            @Valid @RequestBody CreateMotherRequest request) {
        MotherResponse response = motherService.registerMother(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Mother registered successfully. NIDA verification is in progress."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MotherDTO>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(motherService.getMotherById(id), "Mother retrieved"));
    }

    @GetMapping("/health-id/{healthId}")
    public ResponseEntity<ApiResponse<MotherDTO>> getByHealthId(@PathVariable String healthId) {
        return ResponseEntity.ok(ApiResponse.success(motherService.getMotherByHealthId(healthId), "Mother retrieved"));
    }
}
