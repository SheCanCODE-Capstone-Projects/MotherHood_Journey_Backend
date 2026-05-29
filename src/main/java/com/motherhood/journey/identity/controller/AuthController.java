package com.motherhood.journey.identity.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.identity.dto.request.LoginRequest;
import com.motherhood.journey.identity.dto.request.RefreshRequest;
import com.motherhood.journey.identity.dto.request.RegisterRequest;
import com.motherhood.journey.identity.dto.response.TokenResponse;
import com.motherhood.journey.identity.dto.response.UserResponse;
import com.motherhood.journey.identity.service.AuthService;
import com.motherhood.journey.identity.service.RefreshTokenService;
import com.motherhood.journey.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, token refresh, logout, and user registration")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService,
                          UserService userService,
                          RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with phone + password",
        description = "Returns an access token (24h) and refresh token (7d). "
            + "Locks the account for 15 minutes after 5 consecutive failures.")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request), "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access + refresh pair",
        description = "Rotates the refresh token. Reusing a previously rotated token revokes the entire token family.")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(
            ApiResponse.success(authService.refresh(request.refreshToken()), "Token refreshed"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token",
        description = "Idempotent. Subsequent refresh calls with the same token will fail.")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out"));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(userService.register(request), "User registered successfully"));
    }
}
