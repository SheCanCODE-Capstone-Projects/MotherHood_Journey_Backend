package com.motherhood.journey.government.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.government.dto.response.ServiceRequestDocResponse;
import com.motherhood.journey.government.enums.DocumentType;
import com.motherhood.journey.government.service.ServiceRequestDocService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-requests/{requestId}/documents")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Service Request Documents",
    description = "Multipart upload with magic-byte MIME detection, SHA-256 hashing, 10MB cap")
public class ServiceRequestDocController {

    private final ServiceRequestDocService docService;

    public ServiceRequestDocController(ServiceRequestDocService docService) {
        this.docService = docService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ServiceRequestDocResponse>> attach(
        @PathVariable UUID requestId,
        @RequestParam("documentType") DocumentType documentType,
        @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(docService.attach(requestId, documentType, file),
                "Document attached"));
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
