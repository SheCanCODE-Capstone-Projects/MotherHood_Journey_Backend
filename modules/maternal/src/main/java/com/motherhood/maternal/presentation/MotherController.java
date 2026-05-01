package com.motherhood.maternal.presentation;

import com.motherhood.identity.domain.entity.User;
import com.motherhood.maternal.application.dto.MotherDTO;
import com.motherhood.maternal.application.dto.MotherRequest;
import com.motherhood.maternal.application.dto.MotherResponse;
import com.motherhood.maternal.application.service.MotherService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mothers")
@RequiredArgsConstructor
public class MotherController {

    private final MotherService motherService;

    @PostMapping
    public ResponseEntity<MotherResponse> register(@Valid @RequestBody MotherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(motherService.registerMother(request));
    }

    @GetMapping("/health/{healthId}")
    public ResponseEntity<MotherResponse> getByHealthId(@PathVariable String healthId) {
        return ResponseEntity.ok(motherService.getByHealthId(healthId));
    }

    @PreAuthorize("hasAnyRole('HEALTH_WORKER','FACILITY_ADMIN','DISTRICT_OFFICER')")
    @GetMapping("/{id}")
    public ResponseEntity<MotherDTO> getMotherById(
            @PathVariable UUID id,
            HttpServletRequest request,
            @AuthenticationPrincipal User caller) {

        UUID facilityId = (UUID) request.getAttribute("facilityId");
        List<UUID> geoScopeIds = (List<UUID>) request.getAttribute("geoScopeIds");

        return ResponseEntity.ok(
                motherService.getMotherById(id, caller)
        );
    }
}