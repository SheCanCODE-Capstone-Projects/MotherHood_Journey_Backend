package com.motherhood.journey.maternal.dto.request;

import com.motherhood.journey.maternal.enums.EducationLevel;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateMotherRequest(
        UUID facilityId,
        UUID geoLocationId,
        LocalDate dateOfBirth,
        EducationLevel educationLevel
) {}