package com.motherhood.journey.government.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.government.dto.response.GovernmentResponse;
import com.motherhood.journey.government.repository.GovernmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GovernmentServiceImpl implements GovernmentService {

    private final GovernmentRepository governmentRepository;

    public GovernmentServiceImpl(GovernmentRepository governmentRepository) {
        this.governmentRepository = governmentRepository;
    }

    @Override
    public GovernmentResponse getGovernmentUserById(UUID id) {
        return GovernmentResponse.from(
            governmentRepository.findById(id)
                .orElseThrow(() -> new CustomException("Government user not found", HttpStatus.NOT_FOUND))
        );
    }

    @Override
    public GovernmentResponse getGovernmentUserByUserId(UUID userId) {
        return GovernmentResponse.from(
            governmentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new CustomException("Government user not found", HttpStatus.NOT_FOUND))
        );
    }
}
