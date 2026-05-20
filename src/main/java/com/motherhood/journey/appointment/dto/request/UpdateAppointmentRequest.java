package com.motherhood.journey.appointment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateAppointmentRequest(

        /** COMPLETED or NO_SHOW — used by health workers after appointment time. */
        @NotBlank(message = "Status is required")
        String status,

        String notes
) {}
