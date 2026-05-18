package com.motherhood.journey.child.service;

import com.motherhood.journey.child.dto.request.CreateChildRequest;
import com.motherhood.journey.child.dto.response.ChildResponse;
import com.motherhood.journey.child.dto.response.ChildSummaryDTO;

import java.util.List;
import java.util.UUID;

public interface ChildService {

    /**
     * Registers a child linked to a mother, records birth details,
     * and auto-creates one VaccinationRecord per vaccination_schedules entry.
     */
    ChildResponse registerChild(CreateChildRequest request);

    ChildResponse getChildById(UUID id);

    List<ChildSummaryDTO> getChildrenByMother(UUID motherId);
}
