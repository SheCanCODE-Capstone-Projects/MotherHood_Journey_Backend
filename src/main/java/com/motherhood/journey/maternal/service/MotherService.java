package com.motherhood.journey.maternal.service;

import com.motherhood.journey.maternal.dto.request.CreateMotherRequest;
import com.motherhood.journey.maternal.dto.response.MotherDTO;
import com.motherhood.journey.maternal.dto.response.MotherResponse;
import com.motherhood.journey.maternal.dto.response.MotherSummaryDTO;

import java.util.List;
import java.util.UUID;

public interface MotherService {

    /** Registers a new mother, generates health_id, triggers async NIDA verification. */
    MotherResponse registerMother(CreateMotherRequest request);

    MotherDTO getMotherById(UUID id);

    MotherDTO getMotherByHealthId(String healthId);

    List<MotherSummaryDTO> getMothersByNidaStatus(String status);
}
