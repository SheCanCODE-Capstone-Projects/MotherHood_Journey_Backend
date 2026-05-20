package com.motherhood.journey.maternal.service;

import com.motherhood.journey.maternal.dto.request.CreateMotherRequest;
import com.motherhood.journey.maternal.dto.request.UpdateMotherRequest;
import com.motherhood.journey.maternal.dto.response.MotherResponse;

import java.util.List;
import java.util.UUID;

public interface MotherService {
    MotherResponse createMother(CreateMotherRequest request);
    MotherResponse getMotherById(UUID id, UUID facilityId);
    List<MotherResponse> getMothersByFacility(UUID facilityId);
    MotherResponse updateMother(UUID id, UUID facilityId, UpdateMotherRequest request);
    void deactivateMother(UUID id, UUID facilityId);
}
