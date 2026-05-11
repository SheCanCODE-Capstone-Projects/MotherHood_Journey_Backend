package com.motherhood.journey.government.service;

import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class NidaApiClient {

    private final RestTemplate restTemplate;

    @Value("${nida.api.base-url}")
    private String nidaBaseUrl;

    @Value("${nida.api.key}")
    private String nidaApiKey;

    /**
     * Verifies a national ID against Rwanda's NIDA registry.
     * Returns "VERIFIED", "FAILED", or "MANUAL" based on the response.
     */
    public String verifyIdentity(String nationalId, String firstName, String lastName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", nidaApiKey);

            Map<String, String> body = Map.of(
                    "nationalId", nationalId,
                    "firstName", firstName,
                    "lastName", lastName
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    nidaBaseUrl + "/verify", request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String status = (String) response.getBody().get("status");
                return switch (status) {
                    case "MATCH" -> "VERIFIED";
                    case "PARTIAL_MATCH" -> "MANUAL";
                    default -> "FAILED";
                };
            }
            return "FAILED";

        } catch (Exception e) {
            log.error("NIDA verification failed for nationalId={}: {}", nationalId, e.getMessage());
            throw new RuntimeException("NIDA_API_ERROR: " + e.getMessage(), e);
        }
    }
}
