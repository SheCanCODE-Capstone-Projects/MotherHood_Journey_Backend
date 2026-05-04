package com.motherhood.journey.facility.dto.request;

import com.motherhood.journey.facility.entity.FacilityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFacilityRequest(
    @NotBlank(message = "Facility name is required")
    String name,

    @NotBlank(message = "District is required")
    String district,

    @NotBlank(message = "Province is required")
    String province,

    @NotNull(message = "Facility type is required")
    FacilityType type,

    String phoneNumber,
    Double latitude,
    Double longitude
) {}

