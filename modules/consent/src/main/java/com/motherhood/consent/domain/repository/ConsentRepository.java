package com.motherhood.consent.domain.repository;

import com.motherhood.consent.domain.model.Consent;
import com.motherhood.consent.domain.model.ConsentType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository {

    // Save a new consent record to the database
    Consent save(Consent consentRecord);

    // Find a consent record by its ID
    // Used for revocation
    Optional<Consent> findById(UUID id);

    // Find all consent records for a specific mother
    // Used to show a mother's full consent history
    List<Consent> findAllByMotherId(UUID motherId);

    // Find all consent records for a mother of a specific type
    // Used by hasActiveConsent to check if valid consent exists
    List<Consent> findAllByMotherIdAndConsentType(
            UUID motherId, ConsentType consentType);
}