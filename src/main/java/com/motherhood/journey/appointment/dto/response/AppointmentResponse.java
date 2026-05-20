package com.motherhood.journey.appointment.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID patientRefId,
        String patientType,
        UUID facilityId,
        UUID healthWorkerId,
        LocalDateTime scheduledAt,
        String appointmentType,
        String status,
        Boolean reminderSent,
        String notes,
        String cancellationReason,
        LocalDateTime createdAt
) {}
