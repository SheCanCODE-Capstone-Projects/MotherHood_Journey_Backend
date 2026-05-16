package com.motherhood.journey.maternal.service;

import com.motherhood.journey.maternal.dto.request.CreatePregnancyRequest;
import com.motherhood.journey.maternal.dto.request.UpdatePregnancyRequest;
import com.motherhood.journey.maternal.dto.response.PregnancyResponse;

import java.util.List;
import java.util.UUID;

public interface PregnancyService {
    PregnancyResponse createPregnancy(CreatePregnancyRequest request);
    PregnancyResponse getPregnancyById(UUID id, UUID facilityId);
    List<PregnancyResponse> getPregnanciesByMother(UUID motherId, UUID facilityId);
    PregnancyResponse updatePregnancy(UUID id, UUID facilityId, UpdatePregnancyRequest request);
}
