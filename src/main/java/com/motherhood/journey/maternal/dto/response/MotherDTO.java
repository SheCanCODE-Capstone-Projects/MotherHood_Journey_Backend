package com.motherhood.journey.maternal.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MotherDTO(
        UUID id,
        UUID userId,
        UUID facilityId,
        UUID geoLocationId,
        String healthId,
        String nidaVerifiedStatus,
        LocalDate dateOfBirth,
        String educationLevel,
        LocalDateTime registeredAt
) {}
