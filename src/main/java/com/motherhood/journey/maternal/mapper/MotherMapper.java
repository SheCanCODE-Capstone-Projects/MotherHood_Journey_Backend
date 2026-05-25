package com.motherhood.journey.maternal.mapper;

import com.motherhood.journey.maternal.dto.response.MotherDTO;
import com.motherhood.journey.maternal.dto.response.MotherSummaryDTO;
import com.motherhood.journey.maternal.entity.Mother;
import org.springframework.stereotype.Component;

@Component
public class MotherMapper {

    public MotherDTO toDTO(Mother mother) {
        if (mother == null) return null;
        return new MotherDTO(
            mother.getId(),
            mother.getUser() != null ? mother.getUser().getId() : null,
            mother.getFacility() != null ? mother.getFacility().getId() : null,
            mother.getGeoLocation() != null ? mother.getGeoLocation().getId() : null,
            mother.getHealthId(),
            mother.getNidaVerifiedStatus() != null ? mother.getNidaVerifiedStatus().name() : null,
            mother.getDateOfBirth(),
            mother.getEducationLevel(),
            mother.getRegisteredAt()
        );
    }

    public MotherSummaryDTO toSummary(Mother mother) {
        if (mother == null) return null;
        return new MotherSummaryDTO(
            mother.getId(),
            mother.getHealthId(),
            mother.getNidaVerifiedStatus() != null ? mother.getNidaVerifiedStatus().name() : null,
            mother.getFacility() != null ? mother.getFacility().getName() : null,
            mother.getFacility() != null ? mother.getFacility().getDistrict() : null
        );
    }
}
