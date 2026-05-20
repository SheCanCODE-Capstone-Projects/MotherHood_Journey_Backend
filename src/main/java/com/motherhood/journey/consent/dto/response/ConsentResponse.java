package com.motherhood.journey.consent.dto.response;

import com.motherhood.journey.consent.entity.ConsentRecord;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsentResponse(
    UUID id,
    UUID motherId,
    String consentType,
    Boolean granted,
    String grantedByRole,
    LocalDateTime consentedAt,
    LocalDateTime expiresAt,
    String legalBasis,
    LocalDateTime revokedAt
) {
    public static ConsentResponse from(ConsentRecord c) {
        return new ConsentResponse(
            c.getId(),
            c.getMother().getId(),
            c.getConsentType(),
            c.getGranted(),
            c.getGrantedByRole(),
            c.getConsentedAt(),
            c.getExpiresAt(),
            c.getLegalBasis(),
            c.getRevokedAt()
        );
    }
}
