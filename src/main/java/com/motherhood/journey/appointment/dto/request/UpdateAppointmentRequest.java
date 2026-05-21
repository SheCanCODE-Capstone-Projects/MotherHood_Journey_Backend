package com.motherhood.journey.appointment.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateAppointmentRequest(
    LocalDateTime scheduledAt,
    String appointmentType,
    String status,
    UUID healthWorkerId,
    String notes
) {}
