package com.motherhood.journey.government.dto.request;

import com.motherhood.journey.government.enums.ServiceType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record SubmitServiceRequestRequest(
    @NotNull(message = "Service type is required")
    ServiceType serviceType,

    @NotNull(message = "Facility ID is required")
    UUID facilityId,

    @NotNull(message = "Geo location ID is required")
    UUID geoLocationId,

    Map<String, Object> payload
) {}
