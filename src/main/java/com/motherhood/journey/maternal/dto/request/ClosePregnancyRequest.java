package com.motherhood.journey.maternal.dto.request;

import com.motherhood.journey.maternal.enums.PregnancyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClosePregnancyRequest(
        @NotNull PregnancyStatus status,
        @NotBlank String outcomeNotes
) {}
