package com.motherhood.maternal.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PregnancySummary(
        UUID id,
        LocalDate estimatedDueDate,
        int gestationalWeeks,
        String status
) {}
