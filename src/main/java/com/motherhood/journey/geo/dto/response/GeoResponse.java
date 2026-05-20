package com.motherhood.journey.geo.dto.response;

import com.motherhood.journey.geo.entity.GeoLocation;

import java.time.LocalDateTime;
import java.util.UUID;

public record GeoResponse(
    UUID id,
    String province,
    String district,
    String sector,
    String cell,
    String village,
    String postalCode,
    Double latitude,
    Double longitude,
    Boolean active,
    LocalDateTime createdAt
) {
    public static GeoResponse from(GeoLocation g) {
        return new GeoResponse(
            g.getId(),
            g.getProvince(),
            g.getDistrict(),
            g.getSector(),
            g.getCell(),
            g.getVillage(),
            g.getPostalCode(),
            g.getLatitude(),
            g.getLongitude(),
            g.getActive(),
            g.getCreatedAt()
        );
    }
}
