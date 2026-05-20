package com.motherhood.journey.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AfricasTalkingClient {

    private static final String MESSAGING_URL = "https://api.africastalking.com/version1/messaging";

    private final RestTemplate restTemplate;

    @Value("${africas-talking.api-key}")
    private String apiKey;

    @Value("${africas-talking.username}")
    private String username;

    /**
     * Sends a single SMS message and returns the provider message id when available.
     */
    public String sendSms(String to, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("apiKey", apiKey);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("username", username);
            body.add("to", to);
            body.add("message", message);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(MESSAGING_URL, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Africa's Talking returned non-success status: " + response.getStatusCode());
            }

            Object smsMessageData = response.getBody().get("SMSMessageData");
            if (smsMessageData instanceof Map<?, ?> data) {
                Object recipients = data.get("Recipients");
                if (recipients instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> rec) {
                    Object messageId = rec.get("messageId");
                    if (messageId != null) {
                        return String.valueOf(messageId);
                    }
                }
            }

            log.warn("Africa's Talking response did not include messageId for recipient {}", to);
            return null;
        } catch (Exception e) {
            throw new RuntimeException("AFRICAS_TALKING_ERROR: " + e.getMessage(), e);
        }
    }
}
