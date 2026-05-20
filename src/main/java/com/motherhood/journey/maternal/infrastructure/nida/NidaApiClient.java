package com.motherhood.journey.maternal.infrastructure.nida;

import com.motherhood.journey.maternal.enums.NidaVerifiedStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub NIDA API client for Sprint 1.
 * Always returns VERIFIED — replace with real HTTP call in Sprint 2.
 */
@Slf4j
@Component
public class NidaApiClient {

    /**
     * Verifies a national ID against the NIDA registry.
     *
     * @param nationalId the NID to verify
     * @return verification result with status VERIFIED | FAILED | MANUAL
     */
    public NidaVerificationResult verifyIdentity(String nationalId) {
        log.info("NidaApiClient.verifyIdentity() called for nid={} [STUB — returning VERIFIED]", nationalId);
        // TODO Sprint 2: replace with real NIDA REST/SOAP call
        return new NidaVerificationResult(NidaVerifiedStatus.VERIFIED, "Stub verification successful");
    }
}
