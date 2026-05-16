package com.motherhood.journey.identity.dto.response;

public record AuthResponse(
    String accessToken,
    String tokenType,
    String role,
    Long facilityId
) {
    public static AuthResponse of(String token, String role, Long facilityId) {
        return new AuthResponse(token, "Bearer", role, facilityId);
    }
}
