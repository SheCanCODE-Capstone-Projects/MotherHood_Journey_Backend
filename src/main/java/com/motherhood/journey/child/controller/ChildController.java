package com.motherhood.journey.child.controller;

import com.motherhood.journey.child.dto.request.CreateChildRequest;
import com.motherhood.journey.child.dto.response.ChildResponse;
import com.motherhood.journey.child.dto.response.ChildSummaryDTO;
import com.motherhood.journey.child.service.ChildService;
import com.motherhood.journey.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/children")
@RequiredArgsConstructor
public class ChildController {

    private final ChildService childService;

    /**
     * POST /api/v1/children
     * Registers a child and auto-creates their full vaccination schedule.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChildResponse>> register(
            @Valid @RequestBody CreateChildRequest request) {
        ChildResponse response = childService.registerChild(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response,
                        "Child registered with " + response.vaccinationRecordsCreated() + " vaccination records created"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChildResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(childService.getChildById(id), "Child retrieved"));
    }

    @GetMapping("/mother/{motherId}")
    public ResponseEntity<ApiResponse<List<ChildSummaryDTO>>> getByMother(@PathVariable UUID motherId) {
        return ResponseEntity.ok(ApiResponse.success(childService.getChildrenByMother(motherId), "Children retrieved"));
    }
}
