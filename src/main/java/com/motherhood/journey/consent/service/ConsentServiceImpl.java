package com.motherhood.journey.consent.service;

import com.motherhood.journey.consent.repository.ConsentRepository;
import org.springframework.stereotype.Service;

@Service
public class ConsentServiceImpl implements ConsentService {
    private final ConsentRepository consentRepository;

    public ConsentServiceImpl(ConsentRepository consentRepository) {
        this.consentRepository = consentRepository;
    }
}
