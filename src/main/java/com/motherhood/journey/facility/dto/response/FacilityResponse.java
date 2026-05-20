package com.motherhood.journey.facility.dto.response;

import com.motherhood.journey.facility.entity.FacilityType;
import com.motherhood.journey.geo.entity.Facility;

import java.time.LocalDateTime;
import java.util.UUID;

public record FacilityResponse(
    UUID id,
    String name,
    String facilityCode,
    FacilityType facilityType,
    String district,
    String phone,
    Boolean active,
    UUID geoLocationId,
    LocalDateTime createdAt
) {
    public static FacilityResponse from(Facility facility) {
        return new FacilityResponse(
            facility.getId(),
            facility.getName(),
            facility.getFacilityCode(),
            facility.getFacilityType(),
            facility.getDistrict(),
            facility.getPhone(),
            facility.getActive(),
            facility.getGeoLocation() != null ? facility.getGeoLocation().getId() : null,
            facility.getCreatedAt()
        );
    }
}
