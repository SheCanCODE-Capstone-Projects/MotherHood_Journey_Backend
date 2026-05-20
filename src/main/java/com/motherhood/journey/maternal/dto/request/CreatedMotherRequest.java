package com.motherhood.journey.maternal.dto.request;



import com.motherhood.journey.maternal.enums.EducationLevel;
import jakarta.validation.constraints.NotNull;


import java.time.LocalDate;
import java.util.UUID;

public record CreatedMotherRequest(
        UUID userId,
        UUID facilityId,
        String nationalId,
        UUID geoLocationId,
        LocalDate dateOfBirth,
        EducationLevel educationLevel
) {}