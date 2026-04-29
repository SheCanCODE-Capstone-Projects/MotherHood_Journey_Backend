package com.motherhood.maternal.application.dto;

import com.motherhood.maternal.domain.enums.NidaVerifiedStatus;

import java.util.UUID;

public record MotherSummaryResponse(
        UUID id,
        String healthId,
        NidaVerifiedStatus nidaVerifiedStatus,
        UUID facilityId
) {}
