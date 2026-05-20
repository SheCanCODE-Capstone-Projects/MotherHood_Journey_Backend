package com.motherhood.journey.government.dto.request;

import com.motherhood.journey.government.enums.ScopeLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GovernmentRequest(
    @NotNull UUID userId,
    @NotBlank @Size(max = 32) String govRole,
    @NotBlank @Size(max = 64) String ministry,
    @NotBlank @Size(max = 64) String employeeId,
    @NotNull ScopeLevel scopeLevel,
    boolean canExport,
    boolean canPushHmis
) {}
