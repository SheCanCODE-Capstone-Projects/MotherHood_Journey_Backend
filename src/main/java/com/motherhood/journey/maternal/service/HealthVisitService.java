package com.motherhood.journey.maternal.service;

import com.motherhood.journey.maternal.dto.request.CreateHealthVisitRequest;
import com.motherhood.journey.maternal.dto.response.HealthVisitResponse;

import java.util.UUID;

public interface HealthVisitService {
    HealthVisitResponse recordVisit(CreateHealthVisitRequest request);
    HealthVisitResponse getVisitById(UUID id);
}
