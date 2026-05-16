package com.motherhood.journey.maternal.dto.response;

import com.motherhood.journey.maternal.entity.Mother;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MotherResponse(
    UUID id,
    UUID userId,
    UUID facilityId,
    UUID geoLocationId,
    String healthId,
    String nidaVerifiedStatus,
    LocalDate dateOfBirth,
    String educationLevel,
    LocalDateTime registeredAt
) {
    public static MotherResponse from(Mother mother) {
        return new MotherResponse(
            mother.getId(),
            mother.getUser() != null ? mother.getUser().getId() : null,
            mother.getFacility() != null ? mother.getFacility().getId() : null,
            mother.getGeoLocation() != null ? mother.getGeoLocation().getId() : null,
            mother.getHealthId(),
            mother.getNidaVerifiedStatus(),
            mother.getDateOfBirth(),
            mother.getEducationLevel(),
            mother.getRegisteredAt()
        );
    }
}
