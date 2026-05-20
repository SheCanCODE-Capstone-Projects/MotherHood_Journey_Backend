package com.motherhood.journey.consent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateConsentRequest(

    @NotNull(message = "Mother ID is required")
    UUID motherId,

    @NotBlank(message = "Consent type is required")
    String consentType,

    @NotNull(message = "Granted flag is required")
    Boolean granted,

    String grantedByRole,

    LocalDateTime expiresAt,

    String legalBasis
) {}
