package com.motherhood.journey.child.dto.request;

import java.time.LocalDate;

public record MarkAdministeredRequest(
        LocalDate administeredDate,
        String lotNumber,
        String notes
) {}