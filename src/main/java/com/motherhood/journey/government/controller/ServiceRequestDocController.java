package com.motherhood.journey.government.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.government.dto.request.AttachDocumentRequest;
import com.motherhood.journey.government.dto.response.ServiceRequestDocResponse;
import com.motherhood.journey.government.service.ServiceRequestDocService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-requests/{requestId}/documents")
public class ServiceRequestDocController {

    private final ServiceRequestDocService docService;

    public ServiceRequestDocController(ServiceRequestDocService docService) {
        this.docService = docService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ServiceRequestDocResponse>> attach(
        @PathVariable UUID requestId,
        @Valid @RequestBody AttachDocumentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(docService.attach(requestId, request), "Document attached"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    public ResponseEntity<ApiResponse<List<ServiceRequestDocResponse>>> getByRequest(
        @PathVariable UUID requestId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(docService.getByRequest(requestId), "Documents retrieved"));
    }
}
