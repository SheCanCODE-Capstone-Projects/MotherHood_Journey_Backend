package com.motherhood.journey.child.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChildResponse(
        UUID id,
        UUID motherId,
        UUID facilityId,
        UUID geoLocationId,
        String birthCertificateNo,
        String firstName,
        String gender,
        LocalDate dateOfBirth,
        Double birthWeightKg,
        String deliveryType,
        String healthStatus,
        int vaccinationRecordsCreated,
        LocalDateTime registeredAt
) {}
