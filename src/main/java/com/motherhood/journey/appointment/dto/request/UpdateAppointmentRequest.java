package com.motherhood.journey.appointment.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateAppointmentRequest(
        UUID facilityId,
        UUID healthWorkerId,
        LocalDateTime scheduledAt,
        String appointmentType,
        String status,
        String notes
) {}