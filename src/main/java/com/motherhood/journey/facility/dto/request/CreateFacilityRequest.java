package com.motherhood.journey.facility.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFacilityRequest(
    @NotBlank(message = "Facility name is required")
    @Size(max = 128)
    String name,

    @NotBlank(message = "Facility code is required")
    @Size(max = 32)
    String facilityCode,

    @NotBlank(message = "Facility type is required")
    String facilityType,

    @NotBlank(message = "District is required")
    @Size(max = 64)
    String district,

    @Size(max = 20)
    String phone,

    @NotNull(message = "Geo location is required")
    UUID geoLocationId
) {}
