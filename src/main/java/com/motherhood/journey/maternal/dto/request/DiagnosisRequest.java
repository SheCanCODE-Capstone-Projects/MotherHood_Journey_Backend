package com.motherhood.journey.maternal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiagnosisRequest(
    @NotBlank
    @Size(max = 8)
    String icd10Code,

    @NotBlank
    @Size(max = 32)
    String description,

    @Size(max = 16)
    String severity,

    @NotNull
    Boolean isPrimary
) {}
