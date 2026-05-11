package com.motherhood.journey.identity.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String role
) {}
