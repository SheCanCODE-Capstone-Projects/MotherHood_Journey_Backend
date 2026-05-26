package com.motherhood.journey.government.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateGovernmentUserRequest(
    @NotNull UUID userId,
    @NotBlank @Size(max = 32) String govRole,
    @NotBlank @Size(max = 32) String ministry,
    @NotBlank @Size(max = 64) String employeeId
) {}
