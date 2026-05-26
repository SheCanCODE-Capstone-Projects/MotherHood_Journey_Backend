package com.motherhood.journey.child.mapper;

import com.motherhood.journey.child.dto.response.ChildSummaryDTO;
import com.motherhood.journey.child.entity.Child;
import org.springframework.stereotype.Component;

@Component
public class ChildMapper {

    public ChildSummaryDTO toSummary(Child child) {
        if (child == null) {
            return null;
        }
        return new ChildSummaryDTO(
            child.getId(),
            child.getFirstName(),
            child.getGender(),
            child.getDateOfBirth(),
            child.getHealthStatus(),
            child.getMother() != null ? child.getMother().getId() : null
        );
    }
}
