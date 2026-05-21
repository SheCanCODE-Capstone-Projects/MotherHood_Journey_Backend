package com.motherhood.journey.identity.dto.response;

import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String tokenType,
    String role,
    UUID facilityId
) {
    public static AuthResponse of(String token, String role, UUID facilityId) {
        return new AuthResponse(token, "Bearer", role, facilityId);
    }
}
