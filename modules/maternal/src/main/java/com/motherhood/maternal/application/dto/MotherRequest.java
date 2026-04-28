package com.motherhood.maternal.application.dto;

import com.motherhood.maternal.domain.enums.EducationLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.UUID;

public record MotherRequest(

        @NotNull UUID userId,
        @NotNull UUID facilityId,
        @NotNull UUID geoLocationId,
        @NotNull @Past LocalDate dateOfBirth,
        EducationLevel educationLevel
) {}
