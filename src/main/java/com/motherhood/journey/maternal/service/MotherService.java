package com.motherhood.journey.maternal.service;

import com.motherhood.journey.maternal.dto.request.CreateMotherRequest;
import com.motherhood.journey.maternal.dto.response.MotherResponse;
import java.util.UUID;

public interface MotherService {
    MotherResponse createMother(CreateMotherRequest request);
    MotherResponse getMotherById(UUID id);
    void deactivateMother(UUID id);
}
