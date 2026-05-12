package com.motherhood.journey.maternal.service;

import com.motherhood.journey.maternal.dto.request.CreateMotherRequest;
import com.motherhood.journey.maternal.dto.response.MotherResponse;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class MotherServiceImpl implements MotherService {

    @Override
    public MotherResponse createMother(CreateMotherRequest request) {
        return null;
    }

    @Override
    public MotherResponse getMotherById(UUID id) {
        return null;
    }

    @Override
    public void deactivateMother(UUID id) {
    }
}