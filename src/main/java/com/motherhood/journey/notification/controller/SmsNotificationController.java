package com.motherhood.journey.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motherhood.journey.notification.dto.request.DeliveryWebhookRequest;
import com.motherhood.journey.notification.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/at")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Webhooks (Africa's Talking)",
    description = "HMAC-validated delivery callbacks from Africa's Talking. Public, signature-checked.")
@Slf4j
public class SmsNotificationController {

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    // POST /webhooks/at/delivery
    // Receives delivery status callbacks from Africa's Talking
    // This endpoint is PUBLIC
    @PostMapping("/delivery")
    @Operation(summary = "Handle Africa's Talking SMS delivery callback")
    public ResponseEntity<Void> handleDeliveryStatus(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Africastalking-Signature", required = false) String primarySignature,
            @RequestHeader(value = "X-AT-Signature",            required = false) String legacySignature) {

        // Accept the conventional header name first; fall back to the legacy X-AT-Signature
        // so existing webhook configurations on AT's side continue to work.
        String signature = primarySignature != null && !primarySignature.isBlank()
            ? primarySignature : legacySignature;

        if (signature == null || signature.isBlank()) {
            log.warn("Webhook received without signature — rejected");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!webhookService.isValidSignature(rawBody, signature)) {
            log.warn("Webhook signature validation failed — rejected");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        //  Parse the request body into our DTO
        try {
            DeliveryWebhookRequest request = objectMapper
                    .readValue(rawBody, DeliveryWebhookRequest.class);

            // Update the SMS notification status
            webhookService.handleDeliveryUpdate(request);

            // Return 200 OK to tell Africa's Talking we received it
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to process delivery webhook: {}",
                    e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}