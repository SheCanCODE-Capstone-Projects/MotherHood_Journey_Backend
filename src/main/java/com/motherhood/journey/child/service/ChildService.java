package com.motherhood.journey.child.service;

import com.motherhood.journey.child.dto.request.CreateChildRequest;
import com.motherhood.journey.child.dto.request.UpdateChildRequest;
import com.motherhood.journey.child.dto.response.ChildResponse;

import java.util.List;
import java.util.UUID;

public interface ChildService {
    ChildResponse registerChild(CreateChildRequest request);
    ChildResponse getChildById(UUID id, UUID facilityId);
    List<ChildResponse> getChildrenByMother(UUID motherId, UUID facilityId);
    List<ChildResponse> getChildrenByFacility(UUID facilityId);
    ChildResponse updateChild(UUID id, UUID facilityId, UpdateChildRequest request);
}
