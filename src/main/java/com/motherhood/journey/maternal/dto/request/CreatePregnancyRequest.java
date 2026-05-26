package com.motherhood.journey.maternal.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.UUID;

public record CreatePregnancyRequest(
    @NotNull UUID motherId,
    @PastOrPresent LocalDate lmpDate,
    LocalDate edd,
    @PositiveOrZero Integer gravida,
    @PositiveOrZero Integer para,
    UUID assignedChwId
) {}
