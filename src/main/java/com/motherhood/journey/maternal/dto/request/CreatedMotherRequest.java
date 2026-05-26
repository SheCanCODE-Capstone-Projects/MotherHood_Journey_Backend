package com.motherhood.journey.maternal.dto.request;



import com.motherhood.journey.maternal.enums.EducationLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

public record CreatedMotherRequest(
        @NotNull UUID userId,
        @NotNull UUID facilityId,
        @Pattern(regexp = "^[12]\\d{15}$",
                 message = "National ID must be 16 digits starting with 1 or 2")
        String nationalId,
        @NotNull UUID geoLocationId,
        @NotNull @Past LocalDate dateOfBirth,
        EducationLevel educationLevel
) {}