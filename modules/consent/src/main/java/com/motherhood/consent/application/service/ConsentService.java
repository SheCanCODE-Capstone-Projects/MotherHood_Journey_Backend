package com.motherhood.consent.application.service;

import com.motherhood.consent.application.dto.ConsentRequest;
import com.motherhood.consent.application.dto.ConsentResponse;
import com.motherhood.consent.domain.model.Consent;
import com.motherhood.consent.domain.model.ConsentType;
import com.motherhood.consent.domain.repository.ConsentRepository;
import com.motherhood.consent.domain.service.ConsentDomainService;
import com.motherhood.consent.infrastructure.mapper.ConsentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final ConsentRepository repository;
    private final ConsentDomainService domainService;
    private final ConsentMapper mapper;

    @Transactional(readOnly = true)
    public boolean hasActiveConsent(UUID motherId, ConsentType type) {

        // Get all consent records for this mother and type
        List<Consent> records = repository
                .findAllByMotherIdAndConsentType(motherId, type);

        // Check if ANY of them is currently active and valid
        return records.stream()
                .anyMatch(domainService::isActiveConsent);
    }

    @Transactional
    public ConsentResponse recordConsent(ConsentRequest request) {

        // Build the consent record from the request
        Consent record = new Consent();
        record.setMotherId(request.getMotherId());
        record.setConsentType(request.getConsentType());
        record.setGranted(request.getGranted());
        record.setGrantedByRole(request.getGrantedByRole());
        record.setLegalBasis(request.getLegalBasis());
        record.setExpiresAt(request.getExpiresAt());
        // consentedAt is set automatically by @PrePersist
        // revokedAt starts as null — not revoked

        // Save to database
        Consent saved = repository.save(record);

        // Convert to response and set the computed active field
        ConsentResponse response = mapper.toResponse(saved);
        response.setActive(domainService.isActiveConsent(saved));

        return response;
    }

    @Transactional
    public ConsentResponse revokeConsent(UUID id) {

        // Find the consent record
        Consent record = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Consent record not found with id: " + id));

        // Check if it is already revoked
        if (domainService.isAlreadyRevoked(record)) {
            throw new RuntimeException(
                    "Consent record is already revoked");
        }

        // Mark as revoked — record stays in database
        record.setRevokedAt(LocalDateTime.now());

        // Save the updated record
        Consent saved = repository.save(record);

        // Return updated response
        ConsentResponse response = mapper.toResponse(saved);
        response.setActive(false); // revoked no longer active
        return response;
    }

    @Transactional(readOnly = true)
    public List<ConsentResponse> getConsentsByMother(UUID motherId) {
        return repository.findAllByMotherId(motherId)
                .stream()
                .map(record -> {
                    ConsentResponse response = mapper.toResponse(record);
                    response.setActive(
                            domainService.isActiveConsent(record));
                    return response;
                })
                .toList();
    }
}