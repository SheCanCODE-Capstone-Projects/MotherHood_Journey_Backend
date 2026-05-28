package com.motherhood.journey.maternal.infrastructure.nida;

import com.motherhood.journey.maternal.enums.NidaVerifiedStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Calls Rwanda's NIDA identity-verification API to confirm a mother's national ID.
 *
 * When nida.base-url is not set (production credentials not yet issued) the
 * client falls back to VERIFIED so registration is never blocked — this matches
 * the Sprint-1 stub behaviour and keeps the async flow non-breaking.
 *
 * For local development, point nida.base-url at the embedded mock server:
 *   nida.base-url=http://localhost:8080/mock/nida  (set in application-mock.yml)
 */
@Slf4j
@Component
public class NidaApiClient {

    private final RestTemplate restTemplate;

    @Value("${nida.base-url:}")
    private String nidaBaseUrl;

    @Value("${nida.api-key:}")
    private String nidaApiKey;

    public NidaApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public NidaVerificationResult verifyIdentity(String nationalId) {
        if (nidaBaseUrl == null || nidaBaseUrl.isBlank()) {
            log.warn("NidaApiClient: nida.base-url not configured — returning VERIFIED (fallback)");
            return new NidaVerificationResult(NidaVerifiedStatus.VERIFIED, "Fallback: NIDA URL not configured");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (nidaApiKey != null && !nidaApiKey.isBlank()) {
                headers.set("X-API-Key", nidaApiKey);
            }

            HttpEntity<Map<String, String>> request = new HttpEntity<>(
                    Map.of("nationalId", nationalId), headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    nidaBaseUrl + "/verify", request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String status = (String) response.getBody().get("status");
                NidaVerifiedStatus verifiedStatus = switch (status != null ? status : "") {
                    case "MATCH"         -> NidaVerifiedStatus.VERIFIED;
                    case "PARTIAL_MATCH" -> NidaVerifiedStatus.MANUAL;
                    default              -> NidaVerifiedStatus.FAILED;
                };
                log.info("NidaApiClient: nid={} → {}", nationalId, verifiedStatus);
                return new NidaVerificationResult(verifiedStatus, "NIDA response: " + status);
            }

            log.warn("NidaApiClient: unexpected HTTP {} for nid={}", response.getStatusCode(), nationalId);
            return new NidaVerificationResult(NidaVerifiedStatus.FAILED, "NIDA returned no body");

        } catch (Exception e) {
            log.error("NidaApiClient: HTTP error for nid={}: {}", nationalId, e.getMessage());
            return new NidaVerificationResult(NidaVerifiedStatus.FAILED, "Error: " + e.getMessage());
        }
    }
}
