package com.motherhood.journey.maternal.dto.response;

import com.motherhood.journey.maternal.entity.Mother;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
public record MotherResponse(
        UUID id,
        UUID userId,
        String healthId,
        String nidaVerifiedStatus,
        LocalDate dateOfBirth,
        String educationLevel,
        UUID facilityId,
        String facilityName,
        UUID geoLocationId,
        String sector,
        String cell,
        String village,
        LocalDateTime registeredAt
) {
    public static MotherResponse from(Mother mother) {
        return new MotherResponse(
                mother.getId(),
                mother.getUser().getId(),
                mother.getHealthId(),
                mother.getNidaVerifiedStatus().name(),
                mother.getDateOfBirth(),
                mother.getEducationLevel(),
                mother.getFacility().getId(),
                mother.getFacility().getName(),
                mother.getGeoLocation().getId(),
                mother.getGeoLocation().getSector(),
                mother.getGeoLocation().getCell(),
                mother.getGeoLocation().getVillage(),
                mother.getRegisteredAt()
        );
    }
}