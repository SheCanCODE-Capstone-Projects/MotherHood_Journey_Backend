package com.motherhood.journey.identity.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.identity.dto.request.LoginRequest;
import com.motherhood.journey.identity.dto.response.TokenResponse;
import com.motherhood.journey.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokens = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(tokens, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        TokenResponse tokens = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(tokens, "Token refreshed"));
    }
}
