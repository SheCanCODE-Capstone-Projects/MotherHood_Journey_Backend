package com.motherhood.journey.child.dto.response;
import com.motherhood.journey.child.entity.VaccinationRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record VaccinationResponse(
        UUID id,
        UUID childId,
        String vaccineName,
        String antigenCode,
        Integer doseNumber,
        LocalDate dueDate,
        LocalDate administeredDate,
        String lotNumber,
        String status,
        String notes,
        UUID administeredBy,
        LocalDateTime createdAt
) {
    public static VaccinationResponse from(VaccinationRecord r) {
        return new VaccinationResponse(
                r.getId(),
                r.getChild().getId(),
                r.getSchedule().getVaccineName(),
                r.getSchedule().getAntigenCode(),
                r.getSchedule().getDoseNumber(),
                r.getDueDate(),
                r.getAdministeredDate(),
                r.getLotNumber(),
                r.getStatus(),
                r.getNotes(),
                r.getAdministeredBy() != null ? r.getAdministeredBy().getId() : null,
                r.getCreatedAt()
        );
    }
}