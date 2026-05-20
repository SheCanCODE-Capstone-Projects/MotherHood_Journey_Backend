package com.motherhood.journey.government.service;

import com.motherhood.journey.government.dto.response.GovernmentResponse;

import java.util.UUID;

public interface GovernmentService {
    GovernmentResponse getGovernmentUserById(UUID id);
    GovernmentResponse getGovernmentUserByUserId(UUID userId);
}
