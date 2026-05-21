package com.motherhood.journey.facility.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateFacilityRequest(
    @Size(max = 128)
    String name,

    @Size(max = 32)
    String facilityType,

    @Size(max = 64)
    String district,

    @Size(max = 20)
    String phone,

    Boolean active
) {}
