package com.motherhood.journey.child.dto.response;

import com.motherhood.journey.child.entity.VaccinationRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record VaccinationRecordResponse(
    UUID id,
    UUID childId,
    UUID scheduleId,
    String vaccineName,
    String antigenCode,
    UUID facilityId,
    UUID administeredById,
    LocalDate dueDate,
    LocalDate administeredDate,
    String lotNumber,
    String status,
    String notes,
    LocalDateTime createdAt
) {
    public static VaccinationRecordResponse from(VaccinationRecord r) {
        return new VaccinationRecordResponse(
            r.getId(),
            r.getChild().getId(),
            r.getSchedule().getId(),
            r.getSchedule().getVaccineName(),
            r.getSchedule().getAntigenCode(),
            r.getFacility().getId(),
            r.getAdministeredBy() != null ? r.getAdministeredBy().getId() : null,
            r.getDueDate(),
            r.getAdministeredDate(),
            r.getLotNumber(),
            r.getStatus(),
            r.getNotes(),
            r.getCreatedAt()
        );
    }
}
