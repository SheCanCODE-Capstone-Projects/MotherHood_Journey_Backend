package com.motherhood.journey.child.dto.request;

import com.motherhood.journey.child.enums.DeliveryType;
import com.motherhood.journey.child.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateChildRequest(
    @NotNull(message = "Mother ID is required")
    UUID motherId,

    @NotNull(message = "Facility ID is required")
    Long facilityId,

    @NotNull(message = "Geo location ID is required")
    UUID geoLocationId,

    String birthCertificateNo,

    @NotBlank(message = "First name is required")
    String firstName,

    @NotNull(message = "Gender is required")
    Gender gender,

    @NotNull(message = "Date of birth is required")
    LocalDate dateOfBirth,

    Double birthWeightKg,

    @NotNull(message = "Delivery type is required")
    DeliveryType deliveryType
) {}