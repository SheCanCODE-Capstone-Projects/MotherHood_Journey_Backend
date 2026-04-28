package com.motherhood.maternal.application.mapper;

import com.motherhood.maternal.application.dto.MotherResponse;
import com.motherhood.maternal.application.dto.MotherSummaryResponse;
import com.motherhood.maternal.domain.entity.Mother;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MotherMapper {

    @Mapping(target = "facilityId",    source = "facility.id")
    @Mapping(target = "geoLocationId", source = "geoLocation.id")
    MotherResponse toResponse(Mother mother);

    @Mapping(target = "facilityId", source = "facility.id")
    MotherSummaryResponse toSummary(Mother mother);
}
