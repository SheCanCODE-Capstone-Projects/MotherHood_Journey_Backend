package com.motherhood.journey.maternal.dto.request;

import com.motherhood.journey.maternal.validation.Icd10Code;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddDiagnosisRequest(

        @NotNull(message = "Visit ID is required")
        UUID visitId,

        @NotBlank(message = "ICD-10 code is required")
        @Icd10Code
        String icd10Code,

        @NotBlank(message = "Description is required")
        String description,

        // MILD | MODERATE | SEVERE
        String severity,

        @NotNull(message = "isPrimary flag is required")
        Boolean isPrimary
) {}
