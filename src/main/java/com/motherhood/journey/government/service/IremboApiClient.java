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
public class IremboApiClient {

    private final RestTemplate restTemplate;

    @Value("${irembo.api.base-url}")
    private String iremboBaseUrl;

    @Value("${irembo.api.key}")
    private String iremboApiKey;

    /**
     * Submits a citizen service request to Irembo.
     * Returns the Irembo ticket ID on success.
     */
    public String submitServiceRequest(String referenceNo, Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + iremboApiKey);

            payload.put("externalReferenceNo", referenceNo);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    iremboBaseUrl + "/service-requests", request, Map.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                return (String) response.getBody().get("ticketId");
            }

            throw new RuntimeException("Irembo returned unexpected status: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("Irembo submission failed for referenceNo={}: {}", referenceNo, e.getMessage());
            throw new RuntimeException("IREMBO_API_ERROR: " + e.getMessage(), e);
        }
    }
}
