package com.motherhood.journey.maternal.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DiagnosisDTO(
        UUID id,
        UUID visitId,
        String icd10Code,
        String description,
        String severity,       // MILD | MODERATE | SEVERE
        Boolean isPrimary,
        LocalDateTime createdAt
) {}
