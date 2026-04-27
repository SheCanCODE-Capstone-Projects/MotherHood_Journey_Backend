package com.motherhood.consent.domain.service;

import com.motherhood.consent.domain.model.Consent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ConsentDomainService {

    // Checks all four conditions that make a consent valid
    // Called by ConsentService.hasActiveConsent()
    public boolean isActiveConsent(Consent record) {

        // consent must have been granted
        if (record.getGranted() == null || !record.getGranted()) {
            return false;
        }

        // consent must not have been revoked
        // revoked_at is null means it was never revoked
        if (record.getRevokedAt() != null) {
            return false;
        }

        // consent must not have expired
        // expires_at is null means it never expires
        if (record.getExpiresAt() != null
                && record.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        // All conditions passed — this consent is active and valid
        return true;
    }

    // Checks if a consent record has already been revoked
    // Used before revoking to prevent double revocation
    public boolean isAlreadyRevoked(Consent record) {
        return record.getRevokedAt() != null;
    }
}