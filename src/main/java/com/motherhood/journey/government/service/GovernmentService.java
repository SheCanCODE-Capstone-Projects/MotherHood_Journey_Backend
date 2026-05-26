package com.motherhood.journey.government.service;

import com.motherhood.journey.government.dto.request.CreateGovernmentUserRequest;
import com.motherhood.journey.government.dto.request.UpdateGovernmentScopeRequest;
import com.motherhood.journey.government.dto.response.GovernmentResponse;

import java.util.List;
import java.util.UUID;

public interface GovernmentService {
    GovernmentResponse getGovernmentUserById(UUID id);
    GovernmentResponse getGovernmentUserByUserId(UUID userId);
    GovernmentResponse create(CreateGovernmentUserRequest request);
    GovernmentResponse updateScope(UUID id, UpdateGovernmentScopeRequest request);
    List<GovernmentResponse> list();
    void deactivate(UUID id);
}
