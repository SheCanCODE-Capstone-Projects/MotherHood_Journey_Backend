package com.motherhood.maternal.application.dto;

import com.motherhood.maternal.domain.enums.EducationLevel;
import com.motherhood.maternal.domain.enums.NidaVerifiedStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MotherResponse(
        UUID id,
        UUID userId,
        UUID facilityId,
        UUID geoLocationId,
        String healthId,
        NidaVerifiedStatus nidaVerifiedStatus,
        LocalDate dateOfBirth,
        EducationLevel educationLevel,
        LocalDateTime registeredAt
) {}
