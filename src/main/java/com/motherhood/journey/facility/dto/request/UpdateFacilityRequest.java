package com.motherhood.journey.facility.dto.request;

import com.motherhood.journey.facility.entity.FacilityType;

public record UpdateFacilityRequest(
    String name,
    String district,
    String province,
    FacilityType type,
    String phoneNumber,
    Double latitude,
    Double longitude
) {}

