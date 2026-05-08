package com.motherhood.journey.maternal.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignChwRequest(
        @NotNull UUID assignedChwId
) {}
