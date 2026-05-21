package com.motherhood.journey.child.dto.request;

import com.motherhood.journey.child.enums.DeliveryType;
import com.motherhood.journey.child.enums.Gender;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.UUID;

public record CreateChildRequest(
    @NotNull UUID motherId,
    @NotNull UUID facilityId,
    @NotNull UUID geoLocationId,
    String birthCertificateNo,
    String firstName,
    @NotNull Gender gender,
    @NotNull @PastOrPresent LocalDate dateOfBirth,
    double birthWeightKg,
    @NotNull DeliveryType deliveryType
) {}
