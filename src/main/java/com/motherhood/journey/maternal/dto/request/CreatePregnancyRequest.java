package com.motherhood.journey.maternal.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreatePregnancyRequest(
    @NotNull UUID motherId,
    LocalDate lmpDate,
    LocalDate edd,
    Integer gravida,
    Integer para,
    UUID assignedChwId
) {}
