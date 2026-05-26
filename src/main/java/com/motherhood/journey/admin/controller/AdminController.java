package com.motherhood.journey.admin.controller;

import com.motherhood.journey.admin.dto.response.AdminDashboardResponse;
import com.motherhood.journey.admin.service.AdminService;
import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.identity.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('MOH_ADMIN')")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin",
    description = "Platform-wide dashboard counters and user activation/deactivation")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Retrieve platform-wide dashboard counters",
        description = "Restricted to MOH_ADMIN.")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(
            ApiResponse.success(adminService.getDashboard(), "Dashboard retrieved"));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users (paginated)",
        description = "Restricted to MOH_ADMIN.")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(adminService.getAllUsers(pageable), "Users retrieved"));
    }

    @PatchMapping("/users/{id}/deactivate")
    @Operation(summary = "Deactivate a user account",
        description = "Restricted to MOH_ADMIN.")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(
            ApiResponse.success(adminService.deactivateUser(id), "User deactivated"));
    }

    @PatchMapping("/users/{id}/activate")
    @Operation(summary = "Activate a user account",
        description = "Restricted to MOH_ADMIN.")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(
            ApiResponse.success(adminService.activateUser(id), "User activated"));
    }
}
