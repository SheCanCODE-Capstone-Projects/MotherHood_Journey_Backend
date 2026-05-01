package com.motherhood.maternal.application.dto;

import com.motherhood.maternal.domain.enums.NidaVerifiedStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MotherDTO(
        UUID id,
        UUID userId,
        String healthId,
        LocalDate dateOfBirth,
        NidaVerifiedStatus nidaStatus,
        LocalDateTime registeredAt,

        // facility
        UUID facilityId,
        String facilityName,

        // geo summary
        GeoSummary geoLocation,

        // current pregnancy (null if none active)
        PregnancySummary currentPregnancy
) {
    public record GeoSummary(UUID id, String sector, String cell, String village) {}
}
