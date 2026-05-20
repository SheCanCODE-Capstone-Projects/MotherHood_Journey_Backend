package com.motherhood.journey.maternal.mapper;

import com.motherhood.journey.maternal.dto.response.MotherDTO;
import com.motherhood.journey.maternal.dto.response.MotherResponse;
import com.motherhood.journey.maternal.dto.response.MotherSummaryDTO;
import com.motherhood.journey.maternal.entity.Mother;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MotherMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "facility.id", target = "facilityId")
    @Mapping(source = "geoLocation.id", target = "geoLocationId")
    MotherDTO toDTO(Mother mother);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "facility.id", target = "facilityId")
    @Mapping(source = "geoLocation.id", target = "geoLocationId")
    MotherResponse toResponse(Mother mother);

    @Mapping(source = "facility.name", target = "facilityName")
    @Mapping(source = "facility.district", target = "district")
    MotherSummaryDTO toSummary(Mother mother);
}
