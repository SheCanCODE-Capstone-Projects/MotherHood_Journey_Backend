package com.motherhood.journey.identity.controller;

import com.motherhood.journey.common.dto.ApiResponse;
import com.motherhood.journey.identity.dto.request.LoginRequest;
import com.motherhood.journey.identity.dto.request.RegisterRequest;
import com.motherhood.journey.identity.dto.response.AuthResponse;
import com.motherhood.journey.identity.dto.response.UserResponse;
import com.motherhood.journey.identity.service.AuthService;
import com.motherhood.journey.identity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request), "Login successful"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(userService.register(request), "User registered successfully"));
    }
}
