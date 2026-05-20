package com.motherhood.journey.child.service;

import com.motherhood.journey.child.dto.request.CreateChildRequest;
import com.motherhood.journey.child.dto.request.UpdateChildRequest;
import com.motherhood.journey.child.dto.response.ChildResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChildService {
    ChildResponse registerChild(CreateChildRequest request);
    ChildResponse getChildById(UUID id, UUID facilityId);
    Page<ChildResponse> getChildrenByMother(UUID motherId, UUID facilityId, Pageable pageable);
    Page<ChildResponse> getChildrenByFacility(UUID facilityId, Pageable pageable);
    ChildResponse updateChild(UUID id, UUID facilityId, UpdateChildRequest request);
}
