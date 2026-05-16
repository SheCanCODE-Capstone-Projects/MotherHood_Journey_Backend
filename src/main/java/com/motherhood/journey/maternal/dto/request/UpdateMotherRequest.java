package com.motherhood.journey.maternal.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateMotherRequest(
    @Size(max = 32)
    String educationLevel,

    String nidaVerifiedStatus
) {}
