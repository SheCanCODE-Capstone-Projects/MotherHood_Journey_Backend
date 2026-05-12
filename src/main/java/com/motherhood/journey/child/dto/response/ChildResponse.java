package com.motherhood.journey.child.dto.response;

import com.motherhood.journey.child.entity.Child;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChildResponse(
    UUID id,
    String healthId,
    UUID motherId,
    Long facilityId,
    UUID geoLocationId,
    String birthCertificateNo,
    String firstName,
    String gender,
    LocalDate dateOfBirth,
    Double birthWeightKg,
    String deliveryType,
    String healthStatus,
    LocalDateTime registeredAt
) {
    public static ChildResponse fromEntity(Child child) {
        return new ChildResponse(
            child.getId(),
            child.getHealthId(),
            child.getMother().getId(),
            child.getFacility().getId(),
            child.getGeoLocation().getId(),
            child.getBirthCertificateNo(),
            child.getFirstName(),
            child.getGender(),
            child.getDateOfBirth(),
            child.getBirthWeightKg(),
            child.getDeliveryType(),
            child.getHealthStatus(),
            child.getRegisteredAt()
        );
    }
}