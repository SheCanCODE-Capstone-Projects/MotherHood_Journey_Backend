package com.motherhood.journey.maternal.dto.response;

import java.util.UUID;

/** Lightweight projection used in lists and search results. */
public record MotherSummaryDTO(
        UUID id,
        String healthId,
        String nidaVerifiedStatus,
        String facilityName,
        String district
) {}
