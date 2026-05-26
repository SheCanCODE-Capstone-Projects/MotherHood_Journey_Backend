package com.motherhood.journey.government.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.government.entity.GovSyncLog;
import com.motherhood.journey.government.enums.SyncStatus;
import com.motherhood.journey.government.repository.GovSyncLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/gov-sync")
@PreAuthorize("hasAnyRole('MOH_ADMIN', 'FACILITY_ADMIN')")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Gov Sync Admin",
    description = "Monitor outbox status, inspect DEAD_LETTER queue, manually retry failed sync entries")
public class GovSyncAdminController {

    private final GovSyncLogRepository repository;

    public GovSyncAdminController(GovSyncLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/status")
    @Operation(summary = "Get outbox sync counters by status",
        description = "Restricted to MOH_ADMIN, FACILITY_ADMIN.")
    public ResponseEntity<ApiResponse<Map<String, Long>>> status() {
        Map<String, Long> counts = Map.of(
            "pending",     (long) repository.findByStatusOrderByCreatedAtDesc(SyncStatus.PENDING).size(),
            "in_flight",   (long) repository.findByStatusOrderByCreatedAtDesc(SyncStatus.IN_FLIGHT).size(),
            "succeeded",   (long) repository.findByStatusOrderByCreatedAtDesc(SyncStatus.SUCCEEDED).size(),
            "dead_letter", (long) repository.findByDeadLetterTrue().size()
        );
        return ResponseEntity.ok(ApiResponse.success(counts, "GovSync status"));
    }

    @GetMapping("/dead-letter")
    @Operation(summary = "List dead-letter sync entries",
        description = "Restricted to MOH_ADMIN, FACILITY_ADMIN.")
    public ResponseEntity<ApiResponse<List<GovSyncLog>>> deadLetterQueue() {
        return ResponseEntity.ok(ApiResponse.success(repository.findByDeadLetterTrue(), "Dead-letter entries"));
    }

    @PostMapping("/{id}/retry")
    @Transactional
    @Operation(summary = "Retry a failed sync entry",
        description = "Restricted to MOH_ADMIN, FACILITY_ADMIN.")
    public ResponseEntity<ApiResponse<Void>> retry(@PathVariable UUID id) {
        GovSyncLog entry = repository.findById(id)
            .orElseThrow(() -> new CustomException("GovSyncLog entry not found", HttpStatus.NOT_FOUND));

        entry.setStatus(SyncStatus.PENDING);
        entry.setDeadLetter(false);
        entry.setRetryCount(0);
        entry.setNextRetryAt(OffsetDateTime.now());
        entry.setErrorMessage(null);
        return ResponseEntity.ok(ApiResponse.success(null, "Entry re-queued"));
    }
}
