package com.motherhood.journey.me.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.me.dto.response.MeProfileResponse;
import com.motherhood.journey.me.service.MeService;
import io.swagger.v3.oas.annotations.Operation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Me",
    description = "Self-service profile, settings, and consent for the authenticated user")
public class MeController {

    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the authenticated user's profile",
        description = "Requires an authenticated user.")
    public ResponseEntity<ApiResponse<MeProfileResponse>> getMyProfile(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(meService.getMyProfile(userDetails.getUsername()), "Profile retrieved"));
    }
}
