package com.motherhood.journey.consent.repository;

import com.motherhood.journey.consent.entity.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsentRepository extends JpaRepository<ConsentRecord, UUID> {

    List<ConsentRecord> findByMother_Id(UUID motherId);

    List<ConsentRecord> findByMother_IdAndConsentType(UUID motherId, String consentType);

    boolean existsByMother_IdAndConsentTypeAndGrantedTrue(UUID motherId, String consentType);

    /**
     * Returns true only when consent is granted, not revoked, and not expired.
     * Use this for all security-sensitive consent checks (e.g. GOV_DATA_SHARE).
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(c) > 0 FROM ConsentRecord c
        WHERE c.mother.id = :motherId
          AND c.consentType = :consentType
          AND c.granted = true
          AND c.revokedAt IS NULL
          AND (c.expiresAt IS NULL OR c.expiresAt > CURRENT_TIMESTAMP)
        """)
    boolean existsActiveConsent(
        @org.springframework.data.repository.query.Param("motherId") UUID motherId,
        @org.springframework.data.repository.query.Param("consentType") String consentType
    );
}
