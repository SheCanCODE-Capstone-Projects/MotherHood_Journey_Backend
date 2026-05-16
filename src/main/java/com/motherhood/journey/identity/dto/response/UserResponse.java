package com.motherhood.journey.identity.dto.response;

import com.motherhood.journey.identity.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String phoneNumber,
    String nationalId,
    String firstName,
    String lastName,
    String role,
    String preferredLanguage,
    Boolean active,
    UUID facilityId,
    UUID geoLocationId,
    LocalDateTime createdAt,
    LocalDateTime lastLogin
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getPhoneNumber(),
            user.getNationalId(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole(),
            user.getPreferredLanguage(),
            user.getActive(),
            user.getFacility() != null ? user.getFacility().getId() : null,
            user.getGeoLocation() != null ? user.getGeoLocation().getId() : null,
            user.getCreatedAt(),
            user.getLastLogin()
        );
    }
}
