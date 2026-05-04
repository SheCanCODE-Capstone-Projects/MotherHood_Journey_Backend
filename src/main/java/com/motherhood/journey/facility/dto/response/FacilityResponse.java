package com.motherhood.journey.facility.dto.response;

import com.motherhood.journey.facility.entity.FacilityType;
import java.time.LocalDateTime;

public record FacilityResponse(
    Long id,
    String name,
    String district,
    String province,
    FacilityType type,
    String phoneNumber,
    Double latitude,
    Double longitude,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

