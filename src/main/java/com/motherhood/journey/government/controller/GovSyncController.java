package com.motherhood.journey.government.controller;

import com.motherhood.journey.government.dto.Response.GovSyncLogResponse;
import com.motherhood.journey.government.service.GovSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gov-sync-log")
@RequiredArgsConstructor
public class GovSyncController {

    private final GovSyncService govSyncService;

    // GET /api/v1/gov-sync-log?status=DEAD_LETTER
    @GetMapping
    @PreAuthorize("hasAnyRole('MOH_ADMIN','GOVERNMENT_ANALYST')")
    public ResponseEntity<List<GovSyncLogResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetSystem) {
        return ResponseEntity.ok(govSyncService.getAll(status, targetSystem));
    }

    // POST /api/v1/gov-sync-log/{id}/retry
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('MOH_ADMIN')")
    public ResponseEntity<GovSyncLogResponse> retry(@PathVariable UUID id) {
        return ResponseEntity.ok(govSyncService.retry(id));
    }
}