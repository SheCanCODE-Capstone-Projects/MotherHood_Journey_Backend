package com.motherhood.journey.me.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.me.dto.response.MeProfileResponse;
import com.motherhood.journey.me.service.MeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MeProfileResponse>> getMyProfile(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(meService.getMyProfile(userDetails.getUsername()), "Profile retrieved"));
    }
}
