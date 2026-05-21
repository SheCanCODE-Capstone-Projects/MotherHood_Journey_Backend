package com.motherhood.journey.appointment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancelAppointmentRequest(

        @NotBlank(message = "Cancellation reason is required")
        String reason
) {}
