package com.motherhood.journey.consent.service;

import com.motherhood.journey.consent.dto.request.CreateConsentRequest;
import com.motherhood.journey.consent.dto.response.ConsentResponse;

import java.util.List;
import java.util.UUID;

public interface ConsentService {
    ConsentResponse createConsent(CreateConsentRequest request);
    ConsentResponse getConsentById(UUID consentId, UUID facilityId);
    List<ConsentResponse> getConsentsByMother(UUID motherId, UUID facilityId);
    void revokeConsent(UUID consentId, UUID facilityId);
}
