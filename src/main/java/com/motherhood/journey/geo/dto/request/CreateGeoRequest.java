package com.motherhood.journey.geo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGeoRequest(

    @NotBlank(message = "Province is required")
    @Size(max = 64)
    String province,

    @NotBlank(message = "District is required")
    @Size(max = 64)
    String district,

    @NotBlank(message = "Sector is required")
    @Size(max = 64)
    String sector,

    @NotBlank(message = "Cell is required")
    @Size(max = 64)
    String cell,

    @NotBlank(message = "Village is required")
    @Size(max = 64)
    String village,

    @Size(max = 16)
    String postalCode,

    Double latitude,
    Double longitude
) {}
