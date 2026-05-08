package com.motherhood.journey.maternal.dto.response;

import com.motherhood.journey.maternal.entity.Pregnancy;
import com.motherhood.journey.maternal.enums.PregnancyStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PregnancyResponse(
        UUID id,
        UUID motherId,
        LocalDate lmpDate,
        LocalDate edd,
        PregnancyStatus status,
        Integer gravida,
        Integer para,
        UUID assignedChwId,
        String outcomeNotes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PregnancyResponse from(Pregnancy p) {
        return new PregnancyResponse(
                p.getId(),
                p.getMotherId(),
                p.getLmpDate(),
                p.getEdd(),
                PregnancyStatus.valueOf(p.getStatus()),
                p.getGravida(),
                p.getPara(),
                p.getAssignedChwId(),
                p.getOutcomeNotes(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}