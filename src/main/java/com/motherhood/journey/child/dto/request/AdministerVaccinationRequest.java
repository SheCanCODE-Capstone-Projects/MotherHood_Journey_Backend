package com.motherhood.journey.child.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record AdministerVaccinationRequest(
    @NotNull UUID administeredById,
    @NotNull @PastOrPresent LocalDate administeredDate,
    @Size(max = 32) String lotNumber,
    String notes
) {}
