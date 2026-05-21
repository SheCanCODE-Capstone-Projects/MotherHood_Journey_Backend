package com.motherhood.journey.child.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record ChildSummaryDTO(
        UUID id,
        String firstName,
        String gender,
        LocalDate dateOfBirth,
        String healthStatus,
        UUID motherId
) {}
