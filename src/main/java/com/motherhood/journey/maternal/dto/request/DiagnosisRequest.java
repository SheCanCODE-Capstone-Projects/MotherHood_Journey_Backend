package com.motherhood.journey.maternal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DiagnosisRequest(
    @NotBlank
    @Pattern(regexp = "^[A-Z][0-9]{2}(\\.[0-9]{1,4})?$", message = "Must be a valid ICD-10 code")
    String icd10Code,

    @NotBlank @Size(max = 255)
    String description,

    @Size(max = 16)
    String severity,

    boolean isPrimary
) {}
