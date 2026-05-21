package com.motherhood.journey.me.dto.response;

import com.motherhood.journey.identity.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record MeProfileResponse(
    UUID id,
    String phoneNumber,
    String firstName,
    String lastName,
    String role,
    String preferredLanguage,
    UUID facilityId,
    UUID geoLocationId,
    LocalDateTime lastLogin
) {
    public static MeProfileResponse from(User user) {
        return new MeProfileResponse(
            user.getId(),
            user.getPhoneNumber(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole(),
            user.getPreferredLanguage(),
            user.getFacility() != null ? user.getFacility().getId() : null,
            user.getGeoLocation() != null ? user.getGeoLocation().getId() : null,
            user.getLastLogin()
        );
    }
}
