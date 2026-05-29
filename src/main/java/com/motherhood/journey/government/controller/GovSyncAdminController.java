package com.motherhood.journey.government.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.government.entity.GovSyncLog;
import com.motherhood.journey.government.enums.SyncStatus;
import com.motherhood.journey.government.enums.TargetSystem;
import com.motherhood.journey.government.repository.GovSyncLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/gov-sync")
@PreAuthorize("hasAnyRole('MOH_ADMIN', 'FACILITY_ADMIN')")
@Tag(name = "Gov Sync Admin",
    description = "Monitor outbox status, inspect DEAD_LETTER queue, manually retry failed sync entries")
public class GovSyncAdminController {

    private final GovSyncLogRepository repository;

    public GovSyncAdminController(GovSyncLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/status")
    @Transactional(readOnly = true)
    @Operation(summary = "Get outbox sync counters by status",
        description = "Restricted to MOH_ADMIN, FACILITY_ADMIN.")
    public ResponseEntity<ApiResponse<Map<String, Long>>> status() {
        Map<String, Long> counts = Map.of(
            "pending",     repository.countByStatus(SyncStatus.PENDING),
            "in_flight",   repository.countByStatus(SyncStatus.IN_FLIGHT),
            "succeeded",   repository.countByStatus(SyncStatus.SUCCEEDED),
            "dead_letter", repository.countByDeadLetterTrue()
        );
        return ResponseEntity.ok(ApiResponse.success(counts, "GovSync status"));
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List sync log entries (optionally filtered)",
        description = "Filters: ?status=PENDING|IN_FLIGHT|SUCCEEDED|DEAD_LETTER, "
                    + "?targetSystem=IREMBO|NIDA|HMIS. Both filters are optional. "
                    + "Restricted to MOH_ADMIN, FACILITY_ADMIN.")
    public ResponseEntity<ApiResponse<Page<GovSyncLog>>> list(
            @RequestParam(required = false) SyncStatus status,
            @RequestParam(required = false) TargetSystem targetSystem,
            @PageableDefault(size = 25) Pageable pageable) {
        Page<GovSyncLog> page = repository.search(status, targetSystem, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "GovSync entries"));
    }

    @GetMapping("/dead-letter")
    @Transactional(readOnly = true)
    @Operation(summary = "List dead-letter sync entries",
        description = "Restricted to MOH_ADMIN, FACILITY_ADMIN.")
    public ResponseEntity<ApiResponse<List<GovSyncLog>>> deadLetterQueue() {
        return ResponseEntity.ok(ApiResponse.success(repository.findByDeadLetterTrue(), "Dead-letter entries"));
    }

    @PostMapping("/{id}/retry")
    @Transactional
    @PreAuthorize("hasRole('MOH_ADMIN')")
    @Operation(summary = "Retry a failed sync entry",
        description = "Re-queues a DEAD_LETTER entry by clearing retryCount and status. "
                    + "Restricted to MOH_ADMIN only — per sprint plan.")
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
