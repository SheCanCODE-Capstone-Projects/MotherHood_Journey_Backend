package com.motherhood.journey.government.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class HmisApiClient {

    private final RestTemplate restTemplate;

    @Value("${hmis.api.base-url}")
    private String hmisBaseUrl;

    @Value("${hmis.api.key}")
    private String hmisApiKey;

    /**
     * Pushes an aggregated government report to HMIS.
     * The API contract can evolve, but the outbox semantics remain stable.
     */
    public String pushReport(String referenceNo, Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + hmisApiKey);

            payload.put("externalReferenceNo", referenceNo);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    hmisBaseUrl + "/reports",
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {
                    });

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                if (body == null) {
                    return "OK";
                }
                Object reportId = body.getOrDefault("reportId", body.getOrDefault("id", "OK"));
                return String.valueOf(reportId);
            }

            throw new RuntimeException("HMIS returned unexpected status: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("HMIS push failed for referenceNo={}: {}", referenceNo, e.getMessage());
            throw new RuntimeException("HMIS_API_ERROR: " + e.getMessage(), e);
        }
    }
}
