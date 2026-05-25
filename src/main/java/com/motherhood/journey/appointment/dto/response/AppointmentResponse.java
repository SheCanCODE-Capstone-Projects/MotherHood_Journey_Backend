package com.motherhood.journey.appointment.dto.response;

import com.motherhood.journey.appointment.entity.Appointment;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID patientRefId,
        String patientType,
        UUID facilityId,
        String facilityName,
        UUID healthWorkerId,
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
                a.getFacility().getName(),
                a.getHealthWorker() != null ? a.getHealthWorker().getId() : null,
                a.getScheduledAt(),
                a.getAppointmentType() != null ? a.getAppointmentType().name() : null,
                a.getStatus() != null ? a.getStatus().name() : null,
                a.getReminderSent(),
                a.getNotes(),
                a.getCreatedAt()
        );
    }
}
