package com.motherhood.journey.government.service;

import com.motherhood.journey.government.repository.GovernmentRepository;
import org.springframework.stereotype.Service;

@Service
public class GovernmentServiceImpl implements GovernmentService {
    private final GovernmentRepository governmentRepository;

    public GovernmentServiceImpl(GovernmentRepository governmentRepository) {
        this.governmentRepository = governmentRepository;
    }
}
