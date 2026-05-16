package com.motherhood.journey.maternal.dto.response;

import com.motherhood.journey.maternal.entity.Pregnancy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PregnancyResponse(
    UUID id,
    UUID motherId,
    LocalDate lmpDate,
    LocalDate edd,
    String status,
    Integer gravida,
    Integer para,
    UUID assignedChwId,
    String outcomeNotes,
    LocalDateTime createdAt
) {
    public static PregnancyResponse from(Pregnancy p) {
        return new PregnancyResponse(
            p.getId(),
            p.getMother() != null ? p.getMother().getId() : null,
            p.getLmpDate(),
            p.getEdd(),
            p.getStatus(),
            p.getGravida(),
            p.getPara(),
            p.getAssignedChw() != null ? p.getAssignedChw().getId() : null,
            p.getOutcomeNotes(),
            p.getCreatedAt()
        );
    }
}
