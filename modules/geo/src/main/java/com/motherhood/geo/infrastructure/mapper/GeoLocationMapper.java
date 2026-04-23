package com.motherhood.geo.infrastructure.mapper;

import com.motherhood.geo.application.dto.GeoLocationResponse;
import com.motherhood.geo.application.dto.GeoLocationSummary;
import com.motherhood.geo.domain.model.GeoLocation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GeoLocationMapper {

    // Converts full GeoLocation entity to GeoLocationResponse DTO
    GeoLocationResponse toResponse(GeoLocation geoLocation);

    // Converts GeoLocation entity to lightweight GeoLocationSummary DTO
    @Mapping(target = "id", source = "id")
    @Mapping(target = "sector", source = "sector")
    @Mapping(target = "cell", source = "cell")
    @Mapping(target = "village", source = "village")
    GeoLocationSummary toSummary(GeoLocation geoLocation);
}