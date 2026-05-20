package com.motherhood.journey.maternal.infrastructure.nida;

import com.motherhood.journey.maternal.enums.NidaVerifiedStatus;

/**
 * Result returned by NidaApiClient after an identity verification attempt.
 */
public record NidaVerificationResult(
        NidaVerifiedStatus status,
        String message
) {}
