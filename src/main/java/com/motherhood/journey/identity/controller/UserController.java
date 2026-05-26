package com.motherhood.journey.identity.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.identity.dto.request.UpdateUserRequest;
import com.motherhood.journey.identity.dto.response.UserResponse;
import com.motherhood.journey.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Users",
    description = "User profile lookup and update (admin-managed)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACILITY_ADMIN', 'MOH_ADMIN', 'DISTRICT_OFFICER')")
    @Operation(summary = "Get a user by ID",
        description = "Restricted to FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER.")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUser(id), "User retrieved"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACILITY_ADMIN', 'MOH_ADMIN')")
    @Operation(summary = "Update a user profile",
        description = "Restricted to FACILITY_ADMIN, MOH_ADMIN.")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable UUID id,
                                                                @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(id, request), "User updated"));
    }
}
