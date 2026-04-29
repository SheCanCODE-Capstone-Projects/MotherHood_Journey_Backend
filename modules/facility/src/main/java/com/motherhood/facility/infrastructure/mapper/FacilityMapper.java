package com.motherhood.facility.infrastructure.mapper;

import com.motherhood.facility.application.dto.FacilityRequest;
import com.motherhood.facility.application.dto.FacilityResponse;
import com.motherhood.facility.domain.model.Facility;
import org.springframework.stereotype.Component;

@Component
public class FacilityMapper {

    public Facility toDomain(FacilityRequest request) {
        return new Facility(
            request.name(),
            request.district(),
            request.province(),
            request.type(),
            request.phoneNumber(),
            request.latitude(),
            request.longitude()
        );
    }

    public FacilityResponse toResponse(Facility facility) {
        return new FacilityResponse(
            facility.getId(),
            facility.getName(),
            facility.getDistrict(),
            facility.getProvince(),
            facility.getType(),
            facility.getPhoneNumber(),
            facility.getLatitude(),
            facility.getLongitude()
        );
    }
}
