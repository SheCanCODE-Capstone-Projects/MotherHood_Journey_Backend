package com.motherhood.journey.appointment.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAppointmentRequest(

    @NotNull(message = "Patient reference ID is required")
    UUID patientRefId,

    @NotNull(message = "Patient type is required")
    com.motherhood.journey.maternal.enums.PatientType patientType,

    @NotNull(message = "Facility ID is required")
    UUID facilityId,

    UUID healthWorkerId,

    UUID geoLocationId,

    @NotNull(message = "Scheduled date/time is required")
    @Future(message = "Appointment must be scheduled in the future")
    LocalDateTime scheduledAt,

    @NotBlank(message = "Appointment type is required")
    String appointmentType,

    String notes
) {}
