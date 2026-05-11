package com.motherhood.journey.maternal.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.UUID;

public record CreateMotherRequest(

        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Facility ID is required")
        UUID facilityId,

        @NotNull(message = "Geo-location ID is required")
        UUID geoLocationId,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        String educationLevel
) {}
