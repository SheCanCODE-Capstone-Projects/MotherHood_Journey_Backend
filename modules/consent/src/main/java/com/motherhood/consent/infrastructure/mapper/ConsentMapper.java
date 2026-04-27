package com.motherhood.consent.infrastructure.mapper;

import com.motherhood.consent.application.dto.ConsentResponse;
import com.motherhood.consent.domain.model.Consent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConsentMapper {

    // Converts entity to response DTO
    // active field is set manually in the service
    // so we ignore it here
    @Mapping(target = "active", ignore = true)
    ConsentResponse toResponse(Consent consentRecord);
}