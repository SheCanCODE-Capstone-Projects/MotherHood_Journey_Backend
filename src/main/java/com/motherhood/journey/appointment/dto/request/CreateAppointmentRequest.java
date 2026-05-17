package com.motherhood.journey.appointment.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAppointmentRequest(
        UUID patientRefId,
        String patientType,
        UUID facilityId,
        UUID healthWorkerId,
        UUID geoLocationId,
        LocalDateTime scheduledAt,
        String appointmentType,
        String notes
) {}