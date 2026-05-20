package com.motherhood.journey.consent.dto.request;

import java.time.LocalDateTime;

public record UpdateConsentRequest(
    Boolean granted,
    LocalDateTime expiresAt,
    String legalBasis
) {}
