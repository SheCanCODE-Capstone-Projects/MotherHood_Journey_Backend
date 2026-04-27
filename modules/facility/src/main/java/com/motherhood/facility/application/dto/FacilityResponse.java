package com.motherhood.facility.application.dto;

import com.motherhood.facility.domain.model.Facility;

public record FacilityResponse(
    Long id,
    String name,
    String district,
    String province,
    Facility.Type type,
    String phoneNumber,
    Double latitude,
    Double longitude
) {}
