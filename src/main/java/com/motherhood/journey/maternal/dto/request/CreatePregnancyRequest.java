package com.motherhood.journey.maternal.dto.request;

import jakarta.validation.constraints.NotNull;


import java.time.LocalDate;

public record CreatePregnancyRequest(
        @NotNull  LocalDate lmpDate,
        @NotNull  Integer gravida,
        @NotNull  Integer para
) {}