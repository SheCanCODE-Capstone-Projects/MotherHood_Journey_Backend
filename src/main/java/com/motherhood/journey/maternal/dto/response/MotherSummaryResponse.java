package com.motherhood.journey.maternal.dto.response;

import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.maternal.enums.NidaVerifiedStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MotherSummaryResponse(
        UUID id,
        String healthId,
        NidaVerifiedStatus nidaVerifiedStatus,
        LocalDate dateOfBirth,
        LocalDateTime registeredAt
) {
    public static MotherSummaryResponse from(Mother mother) {
        return new MotherSummaryResponse(
                mother.getId(),
                mother.getHealthId(),
                mother.getNidaVerifiedStatus(),
                mother.getDateOfBirth(),
                mother.getRegisteredAt()
        );
    }
}