package com.motherhood.journey.appointment.dto.response;

import com.motherhood.journey.appointment.entity.Appointment;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
    UUID id,
    UUID patientRefId,
    String patientType,
    UUID facilityId,
    UUID healthWorkerId,
    UUID geoLocationId,
    LocalDateTime scheduledAt,
    String appointmentType,
    String status,
    Boolean reminderSent,
    String notes,
    LocalDateTime createdAt
) {
    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
            a.getId(),
            a.getPatientRefId(),
            a.getPatientType(),
            a.getFacility().getId(),
            a.getHealthWorker() != null ? a.getHealthWorker().getId() : null,
            a.getGeoLocation() != null ? a.getGeoLocation().getId() : null,
            a.getScheduledAt(),
            a.getAppointmentType(),
            a.getStatus(),
            a.getReminderSent(),
            a.getNotes(),
            a.getCreatedAt()
        );
    }
}
