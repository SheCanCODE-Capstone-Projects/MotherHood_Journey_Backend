package com.motherhood.journey.government.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.government.dto.request.GovReportRequest;
import com.motherhood.journey.government.dto.response.GovReportResponse;
import com.motherhood.journey.government.service.GovReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gov-reports")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Government Reports",
    description = "Aggregate analytics for MoH: vaccination coverage, ANC, birth registration, maternal health")
public class GovReportController {

    private final GovReportService govReportService;

    public GovReportController(GovReportService govReportService) {
        this.govReportService = govReportService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<GovReportResponse>> generate(
        @Valid @RequestBody GovReportRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(govReportService.generate(request), "Report generated"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<GovReportResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(
            ApiResponse.success(govReportService.getById(id), "Report retrieved"));
    }

    @GetMapping("/by-user/{userId}")
    @PreAuthorize("hasAnyRole('MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<Page<GovReportResponse>>> getByUser(
        @PathVariable UUID userId,
        @PageableDefault(size = 20, sort = "generatedAt") Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(govReportService.getByUser(userId, pageable), "Reports retrieved"));
    }
}
