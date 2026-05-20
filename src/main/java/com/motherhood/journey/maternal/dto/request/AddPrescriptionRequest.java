package com.motherhood.journey.maternal.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddPrescriptionRequest(

        @NotNull(message = "Visit ID is required")
        UUID visitId,

        @NotBlank(message = "Medication name is required")
        String medicationName,

        @NotBlank(message = "Dosage is required")
        String dosage,

        @NotBlank(message = "Frequency is required")
        String frequency,

        @NotNull(message = "Duration in days is required")
        @Min(value = 1, message = "Duration must be at least 1 day")
        Integer durationDays,

        String instructions
) {}
