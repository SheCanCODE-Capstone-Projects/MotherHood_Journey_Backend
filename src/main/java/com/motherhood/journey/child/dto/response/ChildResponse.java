package com.motherhood.journey.child.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record ChildResponse(
    UUID id,
    String healthId,
    UUID motherId,
    UUID facilityId,
    UUID geoLocationId,
    String birthCertificateNo,
    String firstName,
    String gender,
    LocalDate dateOfBirth,
    double birthWeightKg,
    String deliveryType,
    String healthStatus
) {}
