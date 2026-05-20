package com.motherhood.journey.maternal.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrescriptionRequest(
    @NotBlank @Size(max = 64) String medicationName,
    @NotBlank @Size(max = 64) String dosage,
    @NotBlank @Size(max = 64) String frequency,
    @Min(1) int durationDays,
    String instructions
) {}
