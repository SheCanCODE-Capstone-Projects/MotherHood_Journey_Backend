package com.motherhood.journey.maternal.dto.request;

import com.motherhood.journey.maternal.validation.Icd10Code;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosisRequest(
    @NotBlank
    @Icd10Code
    String icd10Code,

    @NotBlank @Size(max = 255)
    String description,

    @Size(max = 16)
    String severity,

    boolean isPrimary
) {}
