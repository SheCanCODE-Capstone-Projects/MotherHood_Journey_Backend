package com.motherhood.consent.infrastructure.persistence;

import com.motherhood.consent.domain.model.Consent;
import com.motherhood.consent.domain.model.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataConsentRepository
        extends JpaRepository<Consent, UUID> {

    // Find all records for a mother
    List<Consent> findAllByMotherId(UUID motherId);

    // Find all records for a mother of a specific consent type
    List<Consent> findAllByMotherIdAndConsentType(
            UUID motherId, ConsentType consentType);
}