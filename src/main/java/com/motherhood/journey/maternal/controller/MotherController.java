package com.motherhood.journey.maternal.controller;

import com.motherhood.journey.maternal.dto.request.CreatedMotherRequest;
import com.motherhood.journey.maternal.dto.response.MotherResponse;
import com.motherhood.journey.maternal.dto.response.MotherSummaryResponse;
import com.motherhood.journey.maternal.service.MotherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.motherhood.journey.identity.entity.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mothers")
@RequiredArgsConstructor
public class MotherController {

    private final MotherService motherService;

    @PostMapping
    public ResponseEntity<MotherResponse> register(
            @Valid @RequestBody CreatedMotherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(motherService.registerMother(request));
    }

    @GetMapping("/health/{healthId}")
    public ResponseEntity<MotherResponse> getByHealthId(
            @PathVariable String healthId) {
        return ResponseEntity.ok(motherService.getByHealthId(healthId));
    }

    @PreAuthorize("hasAnyRole('HEALTH_WORKER','FACILITY_ADMIN','DISTRICT_OFFICER','MOH_ADMIN','GOVERNMENT_ANALYST')")
    @GetMapping("/{id}")
    public ResponseEntity<MotherResponse> getMotherById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(motherService.getMotherById(id, caller));
    }

    @PreAuthorize("hasAnyRole('MOH_ADMIN','FACILITY_ADMIN')")
    @GetMapping("/pending-nida")
    public ResponseEntity<List<MotherSummaryResponse>> getPendingNidaVerification() {
        return ResponseEntity.ok(motherService.getPendingNidaVerification());
    }
}
