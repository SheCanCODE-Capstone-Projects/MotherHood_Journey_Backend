package com.motherhood.journey.maternal.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DiagnosisResponse(
    UUID id,
    String icd10Code,
    String description,
    String severity,
    Boolean isPrimary,
    LocalDateTime createdAt
) {}
